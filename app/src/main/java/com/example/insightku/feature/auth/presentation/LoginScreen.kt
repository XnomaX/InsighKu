package com.example.insightku.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.insightku.R
import com.example.insightku.core.data.model.UserData
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.adaptiveDp
import com.example.insightku.feature.auth.presentation.components.AuthButton
import com.example.insightku.feature.auth.presentation.components.AuthDivider
import com.example.insightku.feature.auth.presentation.components.AuthErrorCard
import com.example.insightku.feature.auth.presentation.components.AuthFooter
import com.example.insightku.feature.auth.presentation.components.AuthFormCard
import com.example.insightku.feature.auth.presentation.components.AuthHeader
import com.example.insightku.feature.auth.presentation.components.AuthTextField
import com.example.insightku.feature.auth.presentation.components.GoogleAuthButton
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (UserData) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val viewModel: LoginViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // Activity context required — ApplicationContext makes the picker fail silently.
    val activityContext = context.findActivity() ?: context

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            onLoginSuccess(UserData(name = "User", email = uiState.email))
        }
    }

    val onGoogleSignInClick: () -> Unit = {
        coroutineScope.launch {
            try {
                val credentialManager = CredentialManager.create(activityContext)
                // Explicit button → full account picker, not One Tap bottom sheet.
                val googleIdOption = GetSignInWithGoogleOption.Builder(
                    context.getString(R.string.default_web_client_id)
                ).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(
                    context = activityContext,
                    request = request
                )
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                    viewModel.onEvent(LoginEvent.GoogleSignIn(googleIdToken.idToken))
                } else {
                    viewModel.onEvent(
                        LoginEvent.GoogleSignInFailed(
                            context.getString(R.string.error_google_signin_failed)
                        )
                    )
                }
            } catch (_: GetCredentialCancellationException) {
                // User closed the picker — not an error.
            } catch (e: NoCredentialException) {
                android.util.Log.e("LoginScreen", "No Google credential: ${e.message}", e)
                viewModel.onEvent(
                    LoginEvent.GoogleSignInFailed(
                        context.getString(R.string.error_google_no_account)
                    )
                )
            } catch (e: GetCredentialException) {
                android.util.Log.e("LoginScreen", "Google Sign-In error: ${e.type} ${e.message}", e)
                viewModel.onEvent(
                    LoginEvent.GoogleSignInFailed(
                        e.message ?: context.getString(R.string.error_google_signin_failed)
                    )
                )
            }
        }
    }

    LoginScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onGoogleSignInClick = onGoogleSignInClick,
        onNavigateToSignUp = onNavigateToSignUp,
        onNavigateToForgotPassword = onNavigateToForgotPassword
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun LoginScreenContent(
    uiState: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }
    val passwordFocusRequester = remember { FocusRequester() }
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Dimens.PaddingExtraLarge))

            // ─── Header ──────────────────────────────────────────────────
            AuthHeader(
                title = stringResource(R.string.login_title),
                subtitle = stringResource(R.string.login_subtitle)
            )

            Spacer(modifier = Modifier.height(formSpacing * 2))

            // ─── Form Card ───────────────────────────────────────────────
            AuthFormCard {
                // Email
                AuthTextField(
                    value = uiState.email,
                    onValueChange = { onEvent(LoginEvent.EmailChanged(it)) },
                    label = stringResource(R.string.email_label),
                    placeholder = stringResource(R.string.email_placeholder),
                    leadingIcon = Icons.Default.Email,
                    isError = uiState.emailError != null,
                    errorMessage = uiState.emailError,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                    onNext = { passwordFocusRequester.requestFocus() }
                )

                Spacer(modifier = Modifier.height(formSpacing))

                // Password
                AuthTextField(
                    value = uiState.password,
                    onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
                    label = stringResource(R.string.password_label),
                    placeholder = stringResource(R.string.password_placeholder),
                    leadingIcon = Icons.Default.Lock,
                    isError = uiState.passwordError != null,
                    errorMessage = uiState.passwordError,
                    isPassword = true,
                    showPassword = showPassword,
                    onTogglePasswordVisibility = { showPassword = !showPassword },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    focusRequester = passwordFocusRequester,
                    onDone = { if (uiState.isLoginEnabled) onEvent(LoginEvent.Submit) }
                )

                // Forgot Password
                TextButton(
                    onClick = onNavigateToForgotPassword,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = stringResource(R.string.forgot_password_link),
                        color = AppPalette.accent,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Error Card
                if (uiState.error != null) {
                    AuthErrorCard(
                        errorMessage = uiState.error,
                        onDismiss = { onEvent(LoginEvent.ClearError) }
                    )
                    Spacer(modifier = Modifier.height(formSpacing))
                }

                // Login Button
                AuthButton(
                    text = stringResource(R.string.login_button),
                    onClick = { onEvent(LoginEvent.Submit) },
                    enabled = uiState.isLoginEnabled,
                    isLoading = uiState.isLoading,
                    loadingText = stringResource(R.string.logging_in)
                )
            }

            Spacer(modifier = Modifier.height(formSpacing * 2))

            // ─── Divider & Social ────────────────────────────────────────
            AuthDivider()

            Spacer(modifier = Modifier.height(formSpacing * 2))

            GoogleAuthButton(
                onClick = onGoogleSignInClick,
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(formSpacing * 2))

            // ─── Footer ──────────────────────────────────────────────────
            AuthFooter(
                promptText = stringResource(R.string.no_account_prompt),
                actionText = stringResource(R.string.signup_link),
                onActionClick = onNavigateToSignUp
            )

            Spacer(modifier = Modifier.height(Dimens.PaddingExtraLarge))
        }
    }
}
