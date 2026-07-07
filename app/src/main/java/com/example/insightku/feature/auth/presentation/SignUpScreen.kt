package com.example.insightku.feature.auth.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.insightku.R
import com.example.insightku.core.data.model.UserData
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.adaptiveDp
import com.example.insightku.feature.auth.presentation.components.*

@Composable
fun SignUpScreen(
    onSignUpSuccess: (UserData) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val viewModel: SignUpViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onSignUpSuccess(UserData(name = uiState.name, email = uiState.email))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    SignUpScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToLogin = onNavigateToLogin
    )
}

@Composable
private fun SignUpScreenContent(
    uiState: SignUpState,
    onEvent: (SignUpEvent) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    val screenPadding = adaptiveDp(
        Dimens.AuthScreenPaddingCompact,
        Dimens.AuthScreenPaddingMedium,
        Dimens.AuthScreenPaddingExpanded
    )
    val formSpacing = adaptiveDp(
        Dimens.FormSpacingCompact,
        Dimens.FormSpacingMedium,
        Dimens.FormSpacingExpanded
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPalette.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = screenPadding)
                .imePadding()
                .verticalScroll(scrollState)
                .semantics { contentDescription = "Sign up screen" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Dimens.PaddingExtraLarge))

            // ─── Header ──────────────────────────────────────────────────
            AuthHeader(
                title = stringResource(R.string.signup_title),
                subtitle = stringResource(R.string.signup_subtitle)
            )

            Spacer(modifier = Modifier.height(formSpacing * 2))

            // ─── Form Card ───────────────────────────────────────────────
            AuthFormCard {
                // Name
                AuthTextField(
                    value = uiState.name,
                    onValueChange = { onEvent(SignUpEvent.NameChanged(it)) },
                    label = stringResource(R.string.name_label),
                    placeholder = stringResource(R.string.name_placeholder),
                    leadingIcon = Icons.Default.Person,
                    isError = uiState.nameError != null,
                    errorMessage = uiState.nameError,
                    keyboardType = KeyboardType.Text,
                    onNext = { emailFocusRequester.requestFocus() }
                )

                Spacer(modifier = Modifier.height(formSpacing))

                // Email
                AuthTextField(
                    value = uiState.email,
                    onValueChange = { onEvent(SignUpEvent.EmailChanged(it)) },
                    label = stringResource(R.string.email_label),
                    placeholder = stringResource(R.string.email_placeholder),
                    leadingIcon = Icons.Default.Email,
                    isError = uiState.emailError != null,
                    errorMessage = uiState.emailError,
                    keyboardType = KeyboardType.Email,
                    onNext = { passwordFocusRequester.requestFocus() },
                    focusRequester = emailFocusRequester
                )

                Spacer(modifier = Modifier.height(formSpacing))

                // Password
                AuthTextField(
                    value = uiState.password,
                    onValueChange = { onEvent(SignUpEvent.PasswordChanged(it)) },
                    label = stringResource(R.string.password_label),
                    placeholder = "Create a password",
                    leadingIcon = Icons.Default.Lock,
                    isError = uiState.passwordError != null,
                    errorMessage = uiState.passwordError,
                    isPassword = true,
                    showPassword = showPassword,
                    onTogglePasswordVisibility = { showPassword = !showPassword },
                    keyboardType = KeyboardType.Password,
                    onNext = { confirmPasswordFocusRequester.requestFocus() },
                    focusRequester = passwordFocusRequester
                )

                // Password Strength
                AnimatedVisibility(visible = uiState.password.isNotEmpty()) {
                    PasswordStrengthSection(password = uiState.password)
                }

                Spacer(modifier = Modifier.height(formSpacing))

                // Confirm Password
                AuthTextField(
                    value = uiState.confirmPassword,
                    onValueChange = { onEvent(SignUpEvent.ConfirmPasswordChanged(it)) },
                    label = stringResource(R.string.confirm_password_label),
                    placeholder = stringResource(R.string.confirm_password_placeholder),
                    leadingIcon = Icons.Default.Lock,
                    isError = uiState.confirmPasswordError != null,
                    errorMessage = uiState.confirmPasswordError,
                    isPassword = true,
                    showPassword = showConfirmPassword,
                    onTogglePasswordVisibility = { showConfirmPassword = !showConfirmPassword },
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    onNext = { if (uiState.isSignUpEnabled) onEvent(SignUpEvent.Submit) },
                    focusRequester = confirmPasswordFocusRequester
                )

                Spacer(modifier = Modifier.height(formSpacing))

                // Error Card
                if (uiState.error != null) {
                    AuthErrorCard(
                        errorMessage = uiState.error!!,
                        onDismiss = { onEvent(SignUpEvent.ClearError) }
                    )
                    Spacer(modifier = Modifier.height(formSpacing))
                }

                // Sign Up Button
                AuthButton(
                    text = stringResource(R.string.signup_button),
                    onClick = { onEvent(SignUpEvent.Submit) },
                    enabled = uiState.isSignUpEnabled,
                    isLoading = uiState.isLoading,
                    loadingText = stringResource(R.string.creating_account)
                )
            }

            Spacer(modifier = Modifier.height(formSpacing * 2))

            // ─── Footer ──────────────────────────────────────────────────
            AuthFooter(
                promptText = stringResource(R.string.has_account_prompt),
                actionText = stringResource(R.string.login_link),
                onActionClick = onNavigateToLogin
            )

            Spacer(modifier = Modifier.height(Dimens.PaddingExtraLarge))
        }
    }
}

// ─── Password Strength Section ────────────────────────────────────────────────

@Composable
private fun PasswordStrengthSection(password: String) {
    val strength = calculatePasswordStrength(password)
    val formSpacing = adaptiveDp(
        Dimens.FormSpacingCompact,
        Dimens.FormSpacingMedium,
        Dimens.FormSpacingExpanded
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        // Strength bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LinearProgressIndicator(
                progress = { strength.progress },
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp),
                color = strength.color,
                trackColor = AppPalette.cardBorder,
            )
            Text(
                text = strength.label,
                style = MaterialTheme.typography.labelSmall,
                color = strength.color,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Requirements
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PasswordRequirement(
                text = stringResource(R.string.password_req_length),
                isMet = password.length >= 8
            )
            PasswordRequirement(
                text = stringResource(R.string.password_req_number),
                isMet = password.any { it.isDigit() }
            )
            PasswordRequirement(
                text = stringResource(R.string.password_req_letter),
                isMet = password.any { it.isLetter() }
            )
            PasswordRequirement(
                text = stringResource(R.string.password_req_special),
                isMet = password.any { !it.isLetterOrDigit() }
            )
        }
    }
}

@Composable
private fun PasswordRequirement(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isMet) AppPalette.success else AppPalette.textMuted.copy(alpha = 0.4f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isMet) AppPalette.success else AppPalette.textMuted.copy(alpha = 0.6f)
        )
    }
}

// ─── Password Strength Calculation ────────────────────────────────────────────

private data class PasswordStrength(
    val progress: Float,
    val color: Color,
    val label: String
)

@Composable
private fun calculatePasswordStrength(password: String): PasswordStrength {
    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { it.isLetter() }) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++

    return when (score) {
        0, 1 -> PasswordStrength(
            progress = 0.2f,
            color = AppPalette.error,
            label = stringResource(R.string.password_strength_weak)
        )
        2 -> PasswordStrength(
            progress = 0.4f,
            color = AppPalette.warning,
            label = stringResource(R.string.password_strength_fair)
        )
        3 -> PasswordStrength(
            progress = 0.6f,
            color = AppPalette.accent,
            label = stringResource(R.string.password_strength_good)
        )
        4 -> PasswordStrength(
            progress = 0.8f,
            color = AppPalette.cyan,
            label = stringResource(R.string.password_strength_strong)
        )
        else -> PasswordStrength(
            progress = 1.0f,
            color = AppPalette.success,
            label = stringResource(R.string.password_strength_excellent)
        )
    }
}
