package com.app.anilover.data

import android.content.Context
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Akun yang sedang masuk (diringkas dari FirebaseUser). */
data class AuthUser(val uid: String, val name: String, val email: String?)

/**
 * Pembungkus Firebase Authentication (email + kata sandi).
 *
 * Bila berkas app/google-services.json belum dipasang, Firebase tidak terinisialisasi:
 * [isConfigured] = false dan aplikasi berjalan seperti sebelumnya (tanpa layar masuk),
 * sehingga build tidak pecah.
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

    @Volatile
    private var registering = false

    /** Panggil sekali di MainActivity.onCreate sebelum setContent. */
    fun init(context: Context) {
        val app = context.applicationContext
        prefs = app.getSharedPreferences("anilover_auth", Context.MODE_PRIVATE)
        _guest.value = prefs?.getBoolean("guest", false) ?: false
        isConfigured = try {
            FirebaseApp.getApps(app).isNotEmpty()
        } catch (e: Exception) {
            false
        }
        if (!isConfigured) {
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
            registering = true
            try {
                val auth = FirebaseAuth.getInstance()
                val user = auth.createUserWithEmailAndPassword(email, password).await()?.user
                    ?: throw IllegalStateException("Akun gagal dibuat.")
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

    suspend fun sendPasswordReset(email: String): Result<Unit> =
        try {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    /** Keluar dari akun, atau meninggalkan mode tamu agar layar masuk muncul lagi. */
    fun signOut() {
        setGuest(false)
        if (isConfigured) {
            try { FirebaseAuth.getInstance().signOut() } catch (e: Exception) { }
            _user.value = null
        }
    }

    fun continueAsGuest() = setGuest(true)

    private fun setGuest(value: Boolean) {
        _guest.value = value
        prefs?.edit()?.putBoolean("guest", value)?.apply()
    }

    /** Pesan galat berbahasa Indonesia yang ramah untuk ditampilkan di layar. */
    fun friendlyError(e: Throwable): String = when (e) {
        is FirebaseNetworkException -> "Tidak ada koneksi internet. Periksa jaringan lalu coba lagi."
        // WeakPassword adalah turunan InvalidCredentials, jadi harus diperiksa lebih dulu.
        is FirebaseAuthWeakPasswordException -> "Kata sandi terlalu lemah. Gunakan minimal 6 karakter."
        is FirebaseAuthUserCollisionException -> "Email ini sudah terdaftar. Silakan masuk."
        is FirebaseAuthInvalidCredentialsException -> "Email atau kata sandi salah."
        is FirebaseAuthInvalidUserException -> "Akun tidak ditemukan atau dinonaktifkan."
        is FirebaseTooManyRequestsException -> "Terlalu banyak percobaan. Coba lagi beberapa saat lagi."
        else -> e.localizedMessage?.takeIf { it.isNotBlank() } ?: "Terjadi kesalahan. Coba lagi."
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        name = displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Pengguna",
        email = email,
    )

    /** Menunggu Task Google selesai tanpa dependensi coroutines-play-services. */
    private suspend fun <T> Task<T>.await(): T? = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }
}
