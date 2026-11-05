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
data class AuthUser(val uid: String, val name: String, val email: String?)

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
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
                setGuest(false)
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
                // Nama gagal disimpan tidak boleh membatalkan pendaftaran.
                try {
                    user.updateProfile(
                        UserProfileChangeRequest.Builder().setDisplayName(name).build(),
                    ).await()
                } catch (e: Exception) { /* nama bisa diubah nanti */ }
                setGuest(false)
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

    /** Keluar dari akun, atau meninggalkan mode tamu agar layar masuk muncul lagi. */
    fun signOut() {
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
                _user.value = AuthUser(uid, o.optString("name"), o.optString("email"))
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
            _user.value = AuthUser(uid, o.optString("name"), key)
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
        localPrefs?.edit()?.putString("accounts", arr.toString())?.putString("session", uid)?.apply()
        _user.value = AuthUser(uid, cleanName, key)
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
    )

    /** Menunggu Task Google selesai tanpa dependensi coroutines-play-services. */
    private suspend fun <T> Task<T>.await(): T? = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }
}
