package com.app.anilover.ui

import com.app.anilover.Str
import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.anilover.R
import com.app.anilover.data.AuthRepository
import com.app.anilover.data.Title
import kotlinx.coroutines.launch

private val AuthError = AniloverDanger

/**
 * Pintu masuk aplikasi: tampilkan layar Masuk/Daftar sampai pengguna login atau memilih
 * "Lanjut tanpa akun". Alur ala Netflix: langkah 1 email, langkah 2 kata sandi,
 * langkah 3 personalisasi (lihat [OnboardingScreen]). Tanpa Firebase, akun disimpan
 * lokal di perangkat.
 */
@Composable
fun AuthGate(content: @Composable () -> Unit) {
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
    var register by rememberSaveable { mutableStateOf(true) }
    var step by rememberSaveable { mutableIntStateOf(1) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    BackHandler(enabled = step == 2 && !loading) { step = 1; error = null; info = null }

    fun emailProblem(): String? =
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) Str.get(R.string.val_email) else null

    fun submit() {
        if (loading) return
        focus.clearFocus()
        error = null
        info = null
        if (step == 1) {
            val problem = emailProblem()
            if (problem != null) { error = problem; return }
            step = 2
            return
        }
        val problem = when {
            password.length < 6 -> Str.get(R.string.val_pass)
            register && password != confirm -> Str.get(R.string.val_confirm)
            else -> null
        }
        if (problem != null) { error = problem; return }
        loading = true
        scope.launch {
            val result = if (register) {
                AuthRepository.signUp("", email.trim(), password)
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
        if (emailProblem() != null) {
            error = Str.get(R.string.fill_email_first)
            return
        }
        loading = true
        scope.launch {
            val result = AuthRepository.sendPasswordReset(email.trim())
            loading = false
            result.onSuccess { info = Str.get(R.string.reset_sent, email.trim()) }
            result.onFailure { error = AuthRepository.friendlyError(it) }
        }
    }

    Box(Modifier.fillMaxSize().background(AniloverBackground)) {
        // Semburat aksen di bagian atas.
        Box(
            Modifier.fillMaxWidth().height(340.dp).background(
                Brush.verticalGradient(
                    listOf(AniloverAccent.copy(alpha = 0.14f), Color.Transparent),
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
            Spacer(Modifier.height(16.dp))
            Image(
                painter = painterResource(R.drawable.anilover_logo),
                contentDescription = null,
                modifier = Modifier.size(width = 44.dp, height = 54.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Anilover", color = AniloverTextPrimary,
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (register) Str.get(R.string.auth_sub_register) else Str.get(R.string.auth_sub_login),
                color = AniloverTextDim, style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            if (register) {
                Spacer(Modifier.height(8.dp))
                Text(
                    Str.get(R.string.auth_step_of, step, 3),
                    color = AniloverTextDim, style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(Modifier.height(28.dp))

            if (step == 1) {
                // Pengalih Masuk / Daftar
                Row(
                    Modifier.fillMaxWidth().clip(AniloverShapes.Button)
                        .background(AniloverSurface)
                        .border(1.dp, AniloverOutline, AniloverShapes.Button)
                        .padding(4.dp),
                ) {
                    AuthTab(Str.get(R.string.sign_in), !register, Modifier.weight(1f)) {
                        register = false; error = null; info = null
                    }
                    AuthTab(Str.get(R.string.sign_up), register, Modifier.weight(1f)) {
                        register = true; error = null; info = null
                    }
                }
                Spacer(Modifier.height(20.dp))
                AuthField(
                    value = email, onValueChange = { email = it }, label = "Email",
                    icon = Icons.Filled.Email, enabled = !loading,
                    keyboardType = KeyboardType.Email, imeAction = ImeAction.Done,
                    onAction = { submit() },
                )
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "‹ " + Str.get(R.string.auth_back), color = AniloverAccent,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clip(AniloverShapes.Chip)
                            .clickable(enabled = !loading) { step = 1; error = null; info = null }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        email.trim(), color = AniloverTextDim,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(3f), textAlign = TextAlign.End,
                    )
                    Text(
                        Str.get(R.string.auth_change_email), color = AniloverAccent,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clip(AniloverShapes.Chip)
                            .clickable(enabled = !loading) { step = 1; error = null; info = null }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                AuthField(
                    value = password, onValueChange = { password = it }, label = Str.get(R.string.field_password),
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
                        value = confirm, onValueChange = { confirm = it }, label = Str.get(R.string.field_confirm),
                        icon = Icons.Filled.Lock, enabled = !loading,
                        keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
                        isPassword = true, showPassword = showPass,
                        onTogglePassword = { showPass = !showPass },
                        onAction = { submit() },
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        Str.get(R.string.auth_pass_hint), color = AniloverTextDim,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (!register && AuthRepository.isConfigured) {
                    Text(
                        Str.get(R.string.forgot_password), color = AniloverAccent,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.End)
                            .clip(AniloverShapes.Chip)
                            .clickable(enabled = !loading) { forgotPassword() }
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                }
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
                        when {
                            step == 1 -> Str.get(R.string.auth_continue)
                            register -> Str.get(R.string.create_account)
                            else -> Str.get(R.string.sign_in)
                        },
                        fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    )
                }
            }

            if (step == 1) {
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(1.dp).background(AniloverOutline))
                    Text(
                        Str.get(R.string.or), color = AniloverTextDim,
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
                    Text(Str.get(R.string.continue_guest), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (AuthRepository.isConfigured) Str.get(R.string.guest_note_cloud)
                    else Str.get(R.string.guest_note_local),
                    color = AniloverTextDim, style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Langkah 3 pendaftaran ala Netflix: nama tampilan (opsional) + pilih 3 anime favorit
 * dari poster katalog. Hasilnya disimpan ke profil (Firestore / lokal) untuk
 * personalisasi Beranda. Bisa dilewati.
 */
@Composable
fun OnboardingScreen(
    titles: List<Title>,
    accountName: String,
    onFinish: (name: String, genres: List<String>, titleIds: List<String>) -> Unit,
    onSkip: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(accountName) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var busy by remember { mutableStateOf(false) }
    val candidates = remember(titles) {
        titles.sortedByDescending { it.score ?: 0.0 }.take(18)
    }

    Column(
        Modifier.fillMaxSize().background(AniloverBackground)
            .statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Text(
            Str.get(R.string.auth_step_of, 3, 3),
            color = AniloverTextDim, style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            Str.get(R.string.ob_title), color = AniloverTextPrimary,
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            Str.get(R.string.ob_sub), color = AniloverTextDim,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (AuthRepository.isConfigured) {
            Spacer(Modifier.height(8.dp))
            Text(
                Str.get(R.string.verify_sent_note), color = AniloverGreen,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(16.dp))
        AuthField(
            value = name, onValueChange = { name = it }, label = Str.get(R.string.ob_name_label),
            icon = Icons.Filled.Person, enabled = !busy,
            keyboardType = KeyboardType.Text, imeAction = ImeAction.Done,
            onAction = { },
        )
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(candidates, key = { it.id }) { t ->
                val isSel = selected.contains(t.id)
                Column {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                            .clip(AniloverShapes.Card)
                            .then(
                                if (isSel) Modifier.border(3.dp, AniloverAccent, AniloverShapes.Card)
                                else Modifier,
                            )
                            .clickable(enabled = !busy) {
                                selected = if (isSel) selected - t.id else selected + t.id
                            },
                    ) {
                        PosterImage(
                            model = t.poster, contentDescription = t.title,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (isSel) {
                            Icon(
                                Icons.Filled.CheckCircle, contentDescription = null,
                                tint = AniloverAccent,
                                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        t.title, color = if (isSel) AniloverTextPrimary else AniloverTextDim,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            Str.get(R.string.ob_counter, selected.size.coerceAtMost(3)),
            color = AniloverTextDim, style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                if (busy) return@Button
                busy = true
                val picked = candidates.filter { selected.contains(it.id) }
                val genres = picked.flatMap { it.genres }.distinct().take(12)
                onFinish(name.trim(), genres, picked.map { it.id })
            },
            enabled = !busy && selected.size >= 3,
            shape = AniloverShapes.Button,
            colors = ButtonDefaults.buttonColors(
                containerColor = AniloverAccent, contentColor = AniloverOnAccent,
                disabledContainerColor = AniloverAccent.copy(alpha = 0.5f),
                disabledContentColor = AniloverOnAccent,
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp,
                    color = AniloverOnAccent,
                )
            } else {
                Text(Str.get(R.string.ob_done), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        TextButton(
            onClick = { if (!busy) { busy = true; onSkip() } },
            enabled = !busy,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(Str.get(R.string.ob_skip), color = AniloverTextDim)
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
                        contentDescription = if (showPassword) Str.get(R.string.hide_password) else Str.get(R.string.show_password),
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
