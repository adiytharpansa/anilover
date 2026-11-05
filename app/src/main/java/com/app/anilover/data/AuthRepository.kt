package com.app.anilover.data

import com.app.anilover.Str
import com.app.anilover.R
import android.content.Context
import android.util.Base64
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.json.JSONArray
import org.json.JSONObject

/** Akun yang sedang masuk (diringkas dari FirebaseUser). */
data class AuthUser(
    val uid: String,
    val name: String,
    val email: String?,
    val emailVerified: Boolean = false,
)

/** Galat akun lokal; pesannya sudah siap ditampilkan ke pengguna. */
class LocalAuthException(message: String) : Exception(message)

/**
 * Pembungkus Firebase Authentication (email + kata sandi).
 *
 * Bila berkas app/google-services.json belum dipasang, Firebase tidak terinisialisasi:
 * [isConfigured] = false dan Masuk/Daftar memakai AKUN LOKAL yang tersimpan di perangkat
 * (kata sandi di-hash PBKDF2 + salt). Setelah Firebase dipasang, otomatis memakai Firebase.
 */
object AuthRepository {
    /** Tersedia hanya bila Firebase berhasil diinisialisasi (google-services.json ada). */
    @Volatile
    var isConfigured: Boolean = false
        private set

    private val _user = MutableStateFlow<AuthUser?>(null)
    val user: StateFlow<AuthUser?> = _user

    /** false sampai Firebase selesai memuat sesi tersimpan — mencegah layar masuk berkedip. */
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    private val _guest = MutableStateFlow(false)
    val guest: StateFlow<Boolean> = _guest

    /** true bila pengguna yang masuk belum menyelesaikan personalisasi awal (pilih 3 favorit). */
    private val _needsOnboarding = MutableStateFlow(false)
    val needsOnboarding: StateFlow<Boolean> = _needsOnboarding

    private var prefs: android.content.SharedPreferences? = null
    private var localPrefs: android.content.SharedPreferences? = null

    @Volatile
    private var initialized = false

    @Volatile
    private var registering = false

    /** Panggil sekali di MainActivity.onCreate sebelum setContent. */
    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val app = context.applicationContext
        prefs = app.getSharedPreferences("anilover_auth", Context.MODE_PRIVATE)
        localPrefs = app.getSharedPreferences("anilover_local_accounts", Context.MODE_PRIVATE)
        _guest.value = prefs?.getBoolean("guest", false) ?: false
        isConfigured = try {
            FirebaseApp.getApps(app).isNotEmpty()
        } catch (e: Exception) {
            false
        }
        if (!isConfigured) {
            restoreLocalSession()
            _ready.value = true
            return
        }
        try {
            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                if (!registering) _user.value = auth.currentUser?.toAuthUser()
                _ready.value = true
            }
        } catch (e: Exception) {
            isConfigured = false
            _ready.value = true
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> =
        withContext(NonCancellable) {
            if (!isConfigured) {
                return@withContext withContext(Dispatchers.Default) { localSignIn(email, password) }
            }
            try {
                val cred = FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
                setGuest(false)
                val uid = cred?.user?.uid
                _needsOnboarding.value = uid != null && fetchOnboardingPending(uid)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun signUp(name: String, email: String, password: String): Result<Unit> =
        withContext(NonCancellable) {
            if (!isConfigured) {
                return@withContext withContext(Dispatchers.Default) { localSignUp(name, email, password) }
            }
            registering = true
            try {
                val auth = FirebaseAuth.getInstance()
                val user = auth.createUserWithEmailAndPassword(email, password).await()?.user
                    ?: throw IllegalStateException(Str.get(R.string.auth_err_create))
                val cleanName = name.trim()
                // Nama gagal disimpan tidak boleh membatalkan pendaftaran.
                if (cleanName.isNotBlank()) {
                    try {
                        user.updateProfile(
                            UserProfileChangeRequest.Builder().setDisplayName(cleanName).build(),
                        ).await()
                    } catch (e: Exception) { /* nama bisa diubah nanti */ }
                }
                // Email verifikasi otomatis (tautan, bukan kode) — kegagalannya tidak
                // membatalkan pendaftaran; pengguna tetap bisa masuk & menonton.
                try { user.sendEmailVerification().await() } catch (e: Exception) { }
                saveProfileDoc(
                    user.uid,
                    mapOf(
                        "email" to user.email,
                        "displayName" to cleanName.ifBlank { null },
                        "onboardingDone" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
                setGuest(false)
                _needsOnboarding.value = true
                _user.value = user.toAuthUser()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                registering = false
                _user.value = try {
                    FirebaseAuth.getInstance().currentUser?.toAuthUser()
                } catch (e: Exception) { null }
            }
        }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        if (!isConfigured) {
            return Result.failure(
                LocalAuthException(Str.get(R.string.auth_err_reset_local))
            )
        }
        return try {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Tulis dokumen profil users/{uid} di Firestore (gabung, jangan timpa kolom lain). */
    private suspend fun saveProfileDoc(uid: String, data: Map<String, Any?>) {
        try {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(data.filterValues { it != null }, SetOptions.merge()).await()
        } catch (e: Exception) { /* profil awan tambahan; jangan gagalkan alur akun */ }
    }

    /** true bila dokumen profil belum menandai personalisasi selesai. */
    private suspend fun fetchOnboardingPending(uid: String): Boolean = try {
        val doc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
        doc?.getBoolean("onboardingDone") != true
    } catch (e: Exception) { false }

    /** Kirim ulang email verifikasi ke pengguna Firebase yang sedang masuk. */
    suspend fun resendVerification(): Result<Unit> {
        if (!isConfigured) {
            return Result.failure(LocalAuthException(Str.get(R.string.verify_unavailable)))
        }
        return try {
            val fu = FirebaseAuth.getInstance().currentUser
                ?: return Result.failure(LocalAuthException(Str.get(R.string.auth_err_user)))
            fu.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Segarkan data pengguna dari server (mis. status verifikasi email terbaru). */
    suspend fun refreshUser() {
        if (!isConfigured) return
        try {
            val fu = FirebaseAuth.getInstance().currentUser ?: return
            fu.reload().await()
            _user.value = fu.toAuthUser()
        } catch (e: Exception) { }
    }

    /**
     * Selesaikan personalisasi awal: simpan nama tampilan + genre/judul favorit,
     * lalu tandai selesai agar tidak ditanya lagi.
     */
    suspend fun completeOnboarding(
        name: String,
        genres: List<String>,
        titleIds: List<String>,
    ): Result<Unit> {
        val cleanName = name.trim()
        if (isConfigured) {
            try {
                val fu = FirebaseAuth.getInstance().currentUser
                if (fu != null) {
                    if (cleanName.isNotBlank() && cleanName != fu.displayName) {
                        try {
                            fu.updateProfile(
                                UserProfileChangeRequest.Builder().setDisplayName(cleanName).build(),
                            ).await()
                        } catch (e: Exception) { }
                    }
                    saveProfileDoc(
                        fu.uid,
                        mapOf(
                            "displayName" to cleanName.ifBlank { null },
                            "favoriteGenres" to genres,
                            "favoriteTitleIds" to titleIds,
                            "onboardingDone" to true,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                    _user.value = fu.toAuthUser()
                }
            } catch (e: Exception) { }
        } else {
            val u = _user.value
            if (u != null) {
                val profile = JSONObject()
                    .put("name", cleanName)
                    .put("genres", JSONArray(genres))
                    .put("titles", JSONArray(titleIds))
                localPrefs?.edit()
                    ?.putString("profile_" + u.uid, profile.toString())
                    ?.remove("ob_pending")
                    ?.apply()
                if (cleanName.isNotBlank()) _user.value = u.copy(name = cleanName)
            }
        }
        _needsOnboarding.value = false
        return Result.success(Unit)
    }

    /** Lewati personalisasi: tandai selesai (tanpa favorit) agar tidak ditanya lagi. */
    suspend fun skipOnboarding() {
        if (isConfigured) {
            try {
                FirebaseAuth.getInstance().currentUser?.let {
                    saveProfileDoc(it.uid, mapOf("onboardingDone" to true))
                }
            } catch (e: Exception) { }
        } else {
            localPrefs?.edit()?.remove("ob_pending")?.apply()
        }
        _needsOnboarding.value = false
    }

    /** Keluar dari akun, atau meninggalkan mode tamu agar layar masuk muncul lagi. */
    fun signOut() {
        _needsOnboarding.value = false
        setGuest(false)
        if (isConfigured) {
            try { FirebaseAuth.getInstance().signOut() } catch (e: Exception) { }
            _user.value = null
        } else {
            localPrefs?.edit()?.remove("session")?.apply()
            _user.value = null
        }
    }

    // ---- Akun lokal (dipakai bila Firebase belum dipasang) ----

    private fun localAccounts(): JSONArray = try {
        JSONArray(localPrefs?.getString("accounts", "[]") ?: "[]")
    } catch (e: Exception) { JSONArray() }

    private fun hashPassword(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, 10_000, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun restoreLocalSession() {
        val uid = localPrefs?.getString("session", null) ?: return
        val arr = localAccounts()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optString("uid") == uid) {
                _user.value = AuthUser(uid, o.optString("name"), o.optString("email"), emailVerified = true)
                _needsOnboarding.value = localPrefs?.getString("ob_pending", null) == uid
                return
            }
        }
    }

    @Synchronized
    private fun localSignIn(email: String, password: String): Result<Unit> {
        val key = email.trim().lowercase()
        val arr = localAccounts()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optString("email") != key) continue
            val salt = Base64.decode(o.optString("salt"), Base64.NO_WRAP)
            if (hashPassword(password, salt) != o.optString("hash")) break
            val uid = o.optString("uid")
            localPrefs?.edit()?.putString("session", uid)?.apply()
            _user.value = AuthUser(uid, o.optString("name"), key, emailVerified = true)
            _needsOnboarding.value = localPrefs?.getString("ob_pending", null) == uid
            setGuest(false)
            return Result.success(Unit)
        }
        return Result.failure(LocalAuthException(Str.get(R.string.auth_err_credentials)))
    }

    @Synchronized
    private fun localSignUp(name: String, email: String, password: String): Result<Unit> {
        val key = email.trim().lowercase()
        val arr = localAccounts()
        for (i in 0 until arr.length()) {
            if (arr.optJSONObject(i)?.optString("email") == key) {
                return Result.failure(LocalAuthException(Str.get(R.string.auth_err_exists)))
            }
        }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val uid = "local_" + UUID.randomUUID().toString()
        val cleanName = name.trim().ifBlank { key.substringBefore("@") }
        arr.put(
            JSONObject()
                .put("uid", uid).put("name", cleanName).put("email", key)
                .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .put("hash", hashPassword(password, salt))
        )
        localPrefs?.edit()?.putString("accounts", arr.toString())?.putString("session", uid)
            ?.putString("ob_pending", uid)?.apply()
        _user.value = AuthUser(uid, cleanName, key, emailVerified = true)
        _needsOnboarding.value = true
        setGuest(false)
        return Result.success(Unit)
    }

    fun continueAsGuest() = setGuest(true)

    private fun setGuest(value: Boolean) {
        _guest.value = value
        prefs?.edit()?.putBoolean("guest", value)?.apply()
    }

    /** Pesan galat berbahasa Indonesia yang ramah untuk ditampilkan di layar. */
    fun friendlyError(e: Throwable): String = when (e) {
        is LocalAuthException -> e.message ?: Str.get(R.string.err_generic)
        is FirebaseNetworkException -> Str.get(R.string.auth_err_network)
        // WeakPassword adalah turunan InvalidCredentials, jadi harus diperiksa lebih dulu.
        is FirebaseAuthWeakPasswordException -> Str.get(R.string.auth_err_weak)
        is FirebaseAuthUserCollisionException -> Str.get(R.string.auth_err_exists)
        is FirebaseAuthInvalidCredentialsException -> Str.get(R.string.auth_err_credentials)
        is FirebaseAuthInvalidUserException -> Str.get(R.string.auth_err_user)
        is FirebaseTooManyRequestsException -> Str.get(R.string.auth_err_many)
        else -> e.localizedMessage?.takeIf { it.isNotBlank() } ?: Str.get(R.string.err_generic)
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        name = displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: Str.get(R.string.user_default),
        email = email,
        emailVerified = isEmailVerified,
    )

    /** Menunggu Task Google selesai tanpa dependensi coroutines-play-services. */
    private suspend fun <T> Task<T>.await(): T? = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }
}
