package com.app.anilover.ui

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.anilover.R
import com.app.anilover.data.AuthRepository
import kotlinx.coroutines.launch

private val AuthError = Color(0xFFFF6B6B)

/**
 * Pintu masuk aplikasi: bila Firebase belum dikonfigurasi, langsung tampilkan aplikasi
 * (perilaku lama). Bila sudah, tampilkan layar Masuk/Daftar sampai pengguna login
 * atau memilih "Lanjut tanpa akun".
 */
@Composable
fun AuthGate(content: @Composable () -> Unit) {
    if (!AuthRepository.isConfigured) {
        content()
        return
    }
    val ready by AuthRepository.ready.collectAsState()
    val user by AuthRepository.user.collectAsState()
    val guest by AuthRepository.guest.collectAsState()
    when {
        !ready -> Box(Modifier.fillMaxSize().background(AniloverBackground))
        user != null || guest -> content()
        else -> AuthScreen()
    }
}

@Composable
fun AuthScreen() {
    var showForm by rememberSaveable { mutableStateOf(false) }
    var register by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    fun validate(needPassword: Boolean): String? = when {
        register && needPassword && name.trim().length < 2 -> "Nama minimal 2 karakter."
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Format email tidak valid."
        needPassword && password.length < 6 -> "Kata sandi minimal 6 karakter."
        register && needPassword && password != confirm -> "Konfirmasi kata sandi tidak sama."
        else -> null
    }

    fun submit() {
        if (loading) return
        focus.clearFocus()
        error = null
        info = null
        val problem = validate(needPassword = true)
        if (problem != null) {
            error = problem
            return
        }
        loading = true
        scope.launch {
            val result = if (register) {
                AuthRepository.signUp(name.trim(), email.trim(), password)
            } else {
                AuthRepository.signIn(email.trim(), password)
            }
            loading = false
            result.onFailure { error = AuthRepository.friendlyError(it) }
        }
    }

    fun forgotPassword() {
        if (loading) return
        focus.clearFocus()
        error = null
        info = null
        val problem = validate(needPassword = false)
        if (problem != null) {
            error = "Isi email kamu dulu, lalu ketuk \"Lupa kata sandi?\"."
            return
        }
        loading = true
        scope.launch {
            val result = AuthRepository.sendPasswordReset(email.trim())
            loading = false
            result.onSuccess { info = "Tautan reset kata sandi dikirim ke ${email.trim()}. Cek kotak masuk atau folder spam." }
            result.onFailure { error = AuthRepository.friendlyError(it) }
        }
    }

    androidx.activity.compose.BackHandler(enabled = showForm) {
        showForm = false; error = null; info = null
    }
    if (!showForm) {
        WelcomeScreen(
            onLogin = { register = false; showForm = true },
            onRegister = { register = true; showForm = true },
            onGuest = { AuthRepository.continueAsGuest() },
        )
        return
    }

    Box(Modifier.fillMaxSize().background(AniloverBackground)) {
        // Semburat aksen di bagian atas.
        Box(
            Modifier.fillMaxWidth().height(340.dp).background(
                Brush.verticalGradient(
                    listOf(AniloverAccent.copy(alpha = 0.16f), Color.Transparent),
                ),
            ),
        )
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { showForm = false; error = null; info = null },
                    modifier = Modifier.size(40.dp).background(AniloverSurfaceHigh, androidx.compose.foundation.shape.CircleShape),
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
            }
            Spacer(Modifier.height(8.dp))
            Image(
                painter = painterResource(R.drawable.anilover_logo),
                contentDescription = null,
                modifier = Modifier.size(width = 52.dp, height = 63.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Anilover", color = AniloverTextPrimary,
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (register) "Buat akun untuk mulai menonton" else "Masuk untuk lanjut menonton",
                color = AniloverTextDim, style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            // Pengalih Masuk / Daftar
            Row(
                Modifier.fillMaxWidth().clip(AniloverShapes.Button)
                    .background(AniloverSurface)
                    .border(1.dp, AniloverOutline, AniloverShapes.Button)
                    .padding(4.dp),
            ) {
                AuthTab("Masuk", !register, Modifier.weight(1f)) {
                    register = false; error = null; info = null
                }
                AuthTab("Daftar", register, Modifier.weight(1f)) {
                    register = true; error = null; info = null
                }
            }
            Spacer(Modifier.height(20.dp))

            if (register) {
                AuthField(
                    value = name, onValueChange = { name = it }, label = "Nama",
                    icon = Icons.Filled.Person, enabled = !loading,
                    keyboardType = KeyboardType.Text, imeAction = ImeAction.Next,
                    onAction = { focus.moveFocus(FocusDirection.Down) },
                )
                Spacer(Modifier.height(12.dp))
            }
            AuthField(
                value = email, onValueChange = { email = it }, label = "Email",
                icon = Icons.Filled.Email, enabled = !loading,
                keyboardType = KeyboardType.Email, imeAction = ImeAction.Next,
                onAction = { focus.moveFocus(FocusDirection.Down) },
            )
            Spacer(Modifier.height(12.dp))
            AuthField(
                value = password, onValueChange = { password = it }, label = "Kata sandi",
                icon = Icons.Filled.Lock, enabled = !loading,
                keyboardType = KeyboardType.Password,
                imeAction = if (register) ImeAction.Next else ImeAction.Done,
                isPassword = true, showPassword = showPass,
                onTogglePassword = { showPass = !showPass },
                onAction = { if (register) focus.moveFocus(FocusDirection.Down) else submit() },
            )
            if (register) {
                Spacer(Modifier.height(12.dp))
                AuthField(
                    value = confirm, onValueChange = { confirm = it }, label = "Ulangi kata sandi",
                    icon = Icons.Filled.Lock, enabled = !loading,
                    keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
                    isPassword = true, showPassword = showPass,
                    onTogglePassword = { showPass = !showPass },
                    onAction = { submit() },
                )
            }

            if (!register) {
                Text(
                    "Lupa kata sandi?", color = AniloverAccent,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.End)
                        .clip(AniloverShapes.Chip)
                        .clickable(enabled = !loading) { forgotPassword() }
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }

            error?.let {
                Text(
                    it, color = AuthError, style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                )
            }
            info?.let {
                Text(
                    it, color = AniloverGreen, style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                )
            }

            Button(
                onClick = { submit() },
                enabled = !loading,
                shape = AniloverShapes.Button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AniloverAccent, contentColor = AniloverOnAccent,
                    disabledContainerColor = AniloverAccent.copy(alpha = 0.5f),
                    disabledContentColor = AniloverOnAccent,
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp,
                        color = AniloverOnAccent,
                    )
                } else {
                    Text(
                        if (register) "Buat Akun" else "Masuk",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(1.dp).background(AniloverOutline))
                Text(
                    "atau", color = AniloverTextDim,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Box(Modifier.weight(1f).height(1.dp).background(AniloverOutline))
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { AuthRepository.continueAsGuest() },
                enabled = !loading,
                shape = AniloverShapes.Button,
                border = androidx.compose.foundation.BorderStroke(1.dp, AniloverOutline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AniloverTextPrimary),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text("Lanjut tanpa akun", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Riwayat tonton & Daftarku tersimpan di perangkat ini.",
                color = AniloverTextDim, style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Layar selamat datang: kolase pita miring, logo, judul besar, tombol Masuk / Daftar. */
@Composable
private fun WelcomeScreen(onLogin: () -> Unit, onRegister: () -> Unit, onGuest: () -> Unit) {
    val stripes = listOf(
        Triple("ANIME", listOf(Color(0xFF123A0C), Color(0xFF2E7D1C)), Color.White),
        Triple("SUB INDO", listOf(Color(0xFF1E1F23), Color(0xFF3A3C44)), Color.White),
        Triple("STREAMING", listOf(AniloverAccent, Color(0xFF2BAE2B)), AniloverOnAccent),
    )
    Box(Modifier.fillMaxSize().background(AniloverBackground)) {
        // Kolase pita miring di separuh atas.
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.5f).clipToBounds()) {
            Column(
                Modifier.align(Alignment.Center).fillMaxWidth()
                    .graphicsLayer { rotationZ = -9f; scaleX = 1.4f; scaleY = 1.4f },
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for ((label, colors, textColor) in stripes) {
                    Box(
                        Modifier.fillMaxWidth().height(92.dp)
                            .background(Brush.horizontalGradient(colors)),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            label, color = textColor.copy(alpha = 0.9f),
                            fontSize = 40.sp, fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(start = 40.dp),
                        )
                    }
                }
            }
        }
        // Pudar ke hitam supaya teks di bawah terbaca.
        Box(
            Modifier.fillMaxWidth().fillMaxHeight(0.62f).align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.62f to Color.Transparent,
                        1.0f to AniloverBackground,
                    )
                ),
        )
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.anilover_logo),
                contentDescription = null,
                modifier = Modifier.size(width = 66.dp, height = 80.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Selamat datang di\nAnilover", color = AniloverTextPrimary,
                fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Nonton anime subtitle Indonesia, unduh, dan lanjutkan di mana saja.",
                color = AniloverTextDim, style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onLogin,
                    shape = AniloverShapes.Button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AniloverAccent, contentColor = AniloverOnAccent,
                    ),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("Masuk", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                OutlinedButton(
                    onClick = onRegister,
                    shape = AniloverShapes.Button,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, AniloverAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AniloverAccent),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("Daftar", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onGuest) {
                Text("Lanjut tanpa akun", color = AniloverTextDim, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AuthTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(AniloverShapes.Chip)
            .background(if (selected) AniloverAccent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) AniloverOnAccent else AniloverTextDim,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onAction: () -> Unit,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showPassword) "Sembunyikan kata sandi" else "Tampilkan kata sandi",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onAction() }, onDone = { onAction() }),
        shape = AniloverShapes.Button,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AniloverTextPrimary,
            unfocusedTextColor = AniloverTextPrimary,
            disabledTextColor = AniloverTextDim,
            focusedContainerColor = AniloverSurface,
            unfocusedContainerColor = AniloverSurface,
            disabledContainerColor = AniloverSurface,
            focusedBorderColor = AniloverAccent,
            unfocusedBorderColor = AniloverOutline,
            disabledBorderColor = AniloverOutline,
            focusedLabelColor = AniloverAccent,
            unfocusedLabelColor = AniloverTextDim,
            cursorColor = AniloverAccent,
            focusedLeadingIconColor = AniloverAccent,
            unfocusedLeadingIconColor = AniloverTextDim,
            focusedTrailingIconColor = AniloverTextDim,
            unfocusedTrailingIconColor = AniloverTextDim,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
