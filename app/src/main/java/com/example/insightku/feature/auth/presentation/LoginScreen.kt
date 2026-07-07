package com.example.insightku.feature.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.insightku.R
import com.example.insightku.core.data.model.UserData
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.adaptiveDp
import com.example.insightku.feature.auth.presentation.LoginViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (UserData) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val viewModel: LoginViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            onLoginSuccess(UserData(name = "User", email = uiState.email))
        }
    }

    // Fungsi launcher Google Sign-In menggunakan Credential Manager
    val onGoogleSignInClick: () -> Unit = {
        coroutineScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)  // tampilkan semua akun Google
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                    viewModel.onEvent(LoginEvent.GoogleSignIn(googleIdToken.idToken))
                }
            } catch (e: GetCredentialException) {
                android.util.Log.e("LoginScreen", "Google Sign-In error: ${e.message}")
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

    // Responsive values
    val screenPadding    = adaptiveDp(Dimens.AuthScreenPaddingCompact, Dimens.AuthScreenPaddingMedium, Dimens.AuthScreenPaddingExpanded)
    val cardInnerPadding = adaptiveDp(Dimens.CardInnerPaddingCompact, Dimens.CardInnerPaddingMedium, Dimens.CardInnerPaddingExpanded)
    val formSpacing      = adaptiveDp(Dimens.FormSpacingCompact, Dimens.FormSpacingMedium, Dimens.FormSpacingExpanded)
    val buttonHeight     = adaptiveDp(Dimens.ButtonHeightCompact, Dimens.ButtonHeightMedium, Dimens.ButtonHeightExpanded)

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val successColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val lightGrayBg = MaterialTheme.colorScheme.surfaceVariant

    // statusBarsPadding di Box luar agar logo tidak tertutup status bar
    // padding konten (horizontal + vertical) tetap di Column dalam scroll
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()  // handle status bar inset
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
            AppLogo(adaptiveDp(Dimens.AuthLogoSizeCompact, Dimens.AuthLogoSizeMedium, Dimens.AuthLogoSizeExpanded))
            Spacer(modifier = Modifier.height(Dimens.PaddingExtraLarge))
            WelcomeText()
            Spacer(modifier = Modifier.height(formSpacing))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationMedium),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(cardInnerPadding)) {

                // ─── Email Field ───────────────────────────────────────────────
                EmailInputField(
                    value = uiState.email,
                    onValueChange = { onEvent(LoginEvent.EmailChanged(it)) },
                    isError = uiState.emailError != null,
                    errorMessage = uiState.emailError,
                    onNext = { passwordFocusRequester.requestFocus() },
                    lightGrayBg = lightGrayBg,
                    errorColor = errorColor,
                    successColor = successColor,
                    onSurfaceVariant = onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(formSpacing))

                // ─── Password Field ────────────────────────────────────────────
                PasswordInputField(
                    value = uiState.password,
                    onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
                    isError = uiState.passwordError != null,
                    errorMessage = uiState.passwordError,
                    showPassword = showPassword,
                    onTogglePasswordVisibility = { showPassword = !showPassword },
                    onDone = { if (uiState.isLoginEnabled) onEvent(LoginEvent.Submit) },
                    focusRequester = passwordFocusRequester,
                    lightGrayBg = lightGrayBg,
                    errorColor = errorColor,
                    successColor = successColor,
                    onSurfaceVariant = onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Dimens.PaddingSmall))

                // ─── Error Card ────────────────────────────────────────────────
                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
                    ErrorCard(
                        errorMessage = uiState.error,
                        onDismiss = { onEvent(LoginEvent.ClearError) },
                        errorColor = errorColor
                    )
                }

                // ─── Forgot Password ───────────────────────────────────────────
                TextButton(
                    onClick = onNavigateToForgotPassword,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = stringResource(R.string.forgot_password_link),
                        color = primaryColor,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))

                // ─── Login Button ──────────────────────────────────────────────
                LoginButton(uiState = uiState, onEvent = onEvent, height = buttonHeight)
            }
        }

        Spacer(modifier = Modifier.height(Dimens.PaddingExtraLarge))
        OrDivider()
        Spacer(modifier = Modifier.height(Dimens.PaddingExtraLarge))
        GoogleLoginButton(onClick = onGoogleSignInClick, isLoading = uiState.isLoading)
        Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
        SignUpPrompt(onNavigateToSignUp = onNavigateToSignUp)
        Spacer(modifier = Modifier.height(Dimens.PaddingExtraLarge))
        } // end Column
    } // end Box
}

// ──────────────────────────────────────────────────────────────────────────────
// Sub-components
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppLogo(size: androidx.compose.ui.unit.Dp = 80.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Lightbulb,
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(size * 0.5f),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun WelcomeText() {
    Text(
        text = stringResource(R.string.welcome_to_app, stringResource(R.string.app_name)),
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
    Text(
        text = stringResource(R.string.app_tagline),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun EmailInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    onNext: () -> Unit,
    lightGrayBg: Color,
    errorColor: Color,
    successColor: Color,
    onSurfaceVariant: Color
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.email_label)) },
            placeholder = { Text(stringResource(R.string.email_placeholder)) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = if (isError)
                        "Email input field, error: ${errorMessage ?: "Invalid email"}"
                    else
                        "Email input field"
                },
            shape = RoundedCornerShape(14.dp),
            leadingIcon = {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    tint = when {
                        isError -> errorColor
                        value.isNotEmpty() && !isError -> successColor
                        else -> onSurfaceVariant
                    }
                )
            },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    Icon(
                        if (isError) Icons.Default.Error else Icons.Default.Check,
                        contentDescription = if (isError) "Invalid email" else "Valid email",
                        tint = if (isError) errorColor else successColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = lightGrayBg,
                unfocusedContainerColor = lightGrayBg,
                errorContainerColor = errorColor.copy(alpha = 0.1f),
                focusedBorderColor = when {
                    isError -> errorColor
                    value.isNotEmpty() && !isError -> successColor
                    else -> MaterialTheme.colorScheme.primary
                },
                unfocusedBorderColor = when {
                    isError -> errorColor
                    value.isNotEmpty() && !isError -> successColor
                    else -> Color.Transparent
                },
                errorBorderColor = errorColor,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                errorLabelColor = errorColor
            ),
            isError = isError,
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Email
            ),
            keyboardActions = KeyboardActions(onNext = { onNext() })
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = errorColor,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun PasswordInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    showPassword: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onDone: () -> Unit,
    focusRequester: FocusRequester,
    lightGrayBg: Color,
    errorColor: Color,
    successColor: Color,
    onSurfaceVariant: Color
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.password_label)) },
            placeholder = { Text(stringResource(R.string.password_placeholder)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .semantics {
                    contentDescription = if (isError)
                        "Password input field, error: ${errorMessage ?: "Invalid password"}"
                    else
                        "Password input field"
                },
            shape = RoundedCornerShape(14.dp),
            leadingIcon = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = when {
                        isError -> errorColor
                        value.isNotEmpty() && !isError -> successColor
                        else -> onSurfaceVariant
                    }
                )
            },
            trailingIcon = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (value.isNotEmpty()) {
                        Icon(
                            if (isError) Icons.Default.Error else Icons.Default.Check,
                            contentDescription = null,
                            tint = if (isError) errorColor else successColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onTogglePasswordVisibility,
                        modifier = Modifier.semantics {
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        }
                    ) {
                        Icon(
                            if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = onSurfaceVariant
                        )
                    }
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = lightGrayBg,
                unfocusedContainerColor = lightGrayBg,
                errorContainerColor = errorColor.copy(alpha = 0.1f),
                focusedBorderColor = when {
                    isError -> errorColor
                    value.isNotEmpty() && !isError -> successColor
                    else -> MaterialTheme.colorScheme.primary
                },
                unfocusedBorderColor = when {
                    isError -> errorColor
                    value.isNotEmpty() && !isError -> successColor
                    else -> Color.Transparent
                },
                errorBorderColor = errorColor,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                errorLabelColor = errorColor
            ),
            isError = isError,
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Password
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() })
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = errorColor,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun ErrorCard(errorMessage: String, onDismiss: () -> Unit, errorColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = errorColor.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, errorColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Error",
                    tint = errorColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = errorColor
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss error",
                    modifier = Modifier.size(16.dp),
                    tint = errorColor
                )
            }
        }
    }
}

@Composable
private fun LoginButton(
    uiState: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    height: androidx.compose.ui.unit.Dp = 50.dp
) {
    Button(
        onClick = { onEvent(LoginEvent.Submit) },
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                contentDescription = when {
                    uiState.isLoading -> "Logging in, please wait"
                    uiState.isLoginEnabled -> "Login button, enabled"
                    else -> "Login button, disabled. Please fill in valid email and password"
                }
            },
        enabled = uiState.isLoginEnabled && !uiState.isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = stringResource(R.string.login_button),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.or_continue_with),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun GoogleLoginButton(onClick: () -> Unit, isLoading: Boolean = false) {
    val buttonHeight = adaptiveDp(Dimens.ButtonHeightCompact, Dimens.ButtonHeightMedium, Dimens.ButtonHeightExpanded)
    OutlinedButton(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(buttonHeight),
        shape = RoundedCornerShape(50.dp),
        border = BorderStroke(1.dp, AppPalette.cardBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppPalette.card,
            contentColor = AppPalette.textPrimary
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(Dimens.PaddingMedium))
            Text(
                text = stringResource(R.string.login_with_google),
                color = Color.Black,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SignUpPrompt(onNavigateToSignUp: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.no_account_prompt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(Dimens.PaddingSmall))
        TextButton(
            onClick = onNavigateToSignUp,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Text(
                text = stringResource(R.string.signup_link, stringResource(R.string.app_name)),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

