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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.insightku.R
import com.example.insightku.feature.auth.presentation.SignUpViewModel
import com.example.insightku.core.data.model.UserData
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.adaptiveDp
import com.example.insightku.core.ui.theme.rememberWindowSize

@Composable
fun SignUpScreen(
    onSignUpSuccess: (UserData) -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val viewModel: SignUpViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    // Responsive values
    val screenPadding      = adaptiveDp(Dimens.AuthScreenPaddingCompact, Dimens.AuthScreenPaddingMedium, Dimens.AuthScreenPaddingExpanded)
    val logoSize           = adaptiveDp(Dimens.AuthLogoSizeCompact, Dimens.AuthLogoSizeMedium, Dimens.AuthLogoSizeExpanded)
    val cardInnerPadding   = adaptiveDp(Dimens.CardInnerPaddingCompact, Dimens.CardInnerPaddingMedium, Dimens.CardInnerPaddingExpanded)
    val formSpacing        = adaptiveDp(Dimens.FormSpacingCompact, Dimens.FormSpacingMedium, Dimens.FormSpacingExpanded)
    val buttonHeight       = adaptiveDp(Dimens.ButtonHeightCompact, Dimens.ButtonHeightMedium, Dimens.ButtonHeightExpanded)
    val iconLogoSize       = adaptiveDp(Dimens.AuthLogoSizeCompact * 0.5f, Dimens.AuthLogoSizeMedium * 0.5f, Dimens.AuthLogoSizeExpanded * 0.5f)

    // Theme colors
    val primaryPurple = MaterialTheme.colorScheme.primary
    val lightGrayBg = MaterialTheme.colorScheme.surfaceVariant
    val errorColor = MaterialTheme.colorScheme.error
    val successColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Handle sign up success
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onSignUpSuccess(UserData(name = uiState.name, email = uiState.email))
        }
    }

    // Reset state when screen is first loaded
    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    // Box terluar handle status bar inset agar logo tidak terpotong
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
        // App Icon
        Box(
            modifier = Modifier
                .size(logoSize)
                .background(
                    color = primaryPurple.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = "InsightKu logo",
                modifier = Modifier.size(iconLogoSize),
                tint = primaryPurple
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Welcome Text
        Text(
            text = "Join InsightKu",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create your account for smarter financial tracking",
            style = MaterialTheme.typography.bodyMedium,
            color = onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(formSpacing))

        // Sign Up Form within a Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(cardInnerPadding)
            ) {
                // Name Field with validation
                NameInputField(
                    value = uiState.name,
                    onValueChange = { viewModel.onEvent(SignUpEvent.NameChanged(it)) },
                    isError = uiState.nameError != null,
                    errorMessage = uiState.nameError,
                    onNext = { emailFocusRequester.requestFocus() },
                    lightGrayBg = lightGrayBg,
                    errorColor = errorColor,
                    successColor = successColor,
                    onSurfaceVariant = onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(formSpacing))

                // Email Field with validation
                EmailInputField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEvent(SignUpEvent.EmailChanged(it)) },
                    isError = uiState.emailError != null,
                    errorMessage = uiState.emailError,
                    onNext = { passwordFocusRequester.requestFocus() },
                    focusRequester = emailFocusRequester,
                    lightGrayBg = lightGrayBg,
                    errorColor = errorColor,
                    successColor = successColor,
                    onSurfaceVariant = onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(formSpacing))

                // Password Field with validation
                PasswordInputField(
                    value = uiState.password,
                    onValueChange = { viewModel.onEvent(SignUpEvent.PasswordChanged(it)) },
                    isError = uiState.passwordError != null,
                    errorMessage = uiState.passwordError,
                    showPassword = showPassword,
                    onTogglePasswordVisibility = { showPassword = !showPassword },
                    onNext = { confirmPasswordFocusRequester.requestFocus() },
                    focusRequester = passwordFocusRequester,
                    lightGrayBg = lightGrayBg,
                    errorColor = errorColor,
                    successColor = successColor,
                    onSurfaceVariant = onSurfaceVariant,
                    label = "Password",
                    placeholder = "Create a password"
                )

                // Password Strength Indicator
                if (uiState.password.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(formSpacing * 0.5f))
                    PasswordStrengthIndicator(password = uiState.password)
                }

                Spacer(modifier = Modifier.height(formSpacing))

                // Confirm Password Field with validation
                PasswordInputField(
                    value = uiState.confirmPassword,
                    onValueChange = { viewModel.onEvent(SignUpEvent.ConfirmPasswordChanged(it)) },
                    isError = uiState.confirmPasswordError != null,
                    errorMessage = uiState.confirmPasswordError,
                    showPassword = showConfirmPassword,
                    onTogglePasswordVisibility = { showConfirmPassword = !showConfirmPassword },
                    onNext = {
                        if (uiState.isSignUpEnabled) {
                            viewModel.onEvent(SignUpEvent.Submit)
                        }
                    },
                    focusRequester = confirmPasswordFocusRequester,
                    lightGrayBg = lightGrayBg,
                    errorColor = errorColor,
                    successColor = successColor,
                    onSurfaceVariant = onSurfaceVariant,
                    label = "Confirm Password",
                    placeholder = "Confirm your password"
                )

                Spacer(modifier = Modifier.height(formSpacing))

                // General Error Display
                if (uiState.error != null) {
                    ErrorCard(
                        errorMessage = uiState.error!!,
                        onDismiss = { viewModel.clearError() },
                        errorColor = errorColor
                    )
                    Spacer(modifier = Modifier.height(formSpacing))
                }

                // Sign Up Button
                Button(
                    onClick = { viewModel.onEvent(SignUpEvent.Submit) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight)
                        .semantics {
                            contentDescription = if (uiState.isLoading) {
                                "Creating account, please wait"
                            } else if (uiState.isSignUpEnabled) {
                                "Create account button, enabled"
                            } else {
                                "Create account button, disabled. Please fill in all valid information"
                            }
                        },
                    enabled = uiState.isSignUpEnabled && !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryPurple,
                        disabledContainerColor = primaryPurple.copy(alpha = 0.5f)
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
                        Text("Create Account", fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(formSpacing))

        // Login Prompt
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics { contentDescription = "Login section" }
        ) {
            Text(
                text = "Already have an account?",
                color = onSurfaceVariant,
                fontSize = 13.sp,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.semantics { contentDescription = "Login to InsightKu link" },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Login to InsightKu",
                    color = primaryPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.PaddingExtraLarge))
    } // end Column
    } // end Box
}

@Composable
private fun NameInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    onNext: () -> Unit,
    lightGrayBg:   Color,
    errorColor: Color,
    successColor: Color,
    onSurfaceVariant: Color
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Full Name") },
            placeholder = { Text("Enter your full name") },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = if (isError) {
                        "Name input field, error: ${errorMessage ?: "Invalid name"}"
                    } else {
                        "Name input field"
                    }
                },
            shape = RoundedCornerShape(14.dp),
            leadingIcon = {
                Icon(
                    Icons.Default.Person,
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
                        contentDescription = if (isError) "Invalid name" else "Valid name",
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
                keyboardType = KeyboardType.Text
            ),
            keyboardActions = KeyboardActions(onNext = { onNext() })
        )

        // Error message
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = errorColor,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(start = 16.dp, top = 4.dp)
                    .semantics { contentDescription = "Name error: $errorMessage" }
            )
        }
    }
}

@Composable
private fun EmailInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    onNext: () -> Unit,
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
            label = { Text("Email") },
            placeholder = { Text("Enter your email") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .semantics {
                    contentDescription = if (isError) {
                        "Email input field, error: ${errorMessage ?: "Invalid email"}"
                    } else {
                        "Email input field"
                    }
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

        // Error message
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = errorColor,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(start = 16.dp, top = 4.dp)
                    .semantics { contentDescription = "Email error: $errorMessage" }
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
    onNext: () -> Unit,
    focusRequester: FocusRequester,
    lightGrayBg: Color,
    errorColor: Color,
    successColor: Color,
    onSurfaceVariant: Color,
    label: String,
    placeholder: String
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .semantics {
                    contentDescription = if (isError) {
                        "$label input field, error: ${errorMessage ?: "Invalid $label"}"
                    } else {
                        "$label input field"
                    }
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
                            contentDescription = if (isError) "Invalid $label" else "Valid $label",
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
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Password
            ),
            keyboardActions = KeyboardActions(onNext = { onNext() })
        )

        // Error message
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = errorColor,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(start = 16.dp, top = 4.dp)
                    .semantics { contentDescription = "$label error: $errorMessage" }
            )
        }
    }
}

@Composable
private fun ErrorCard(
    errorMessage: String,
    onDismiss: () -> Unit,
    errorColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = errorColor.copy(alpha = 0.1f)
        ),
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
                    color = errorColor,
                    modifier = Modifier.semantics {
                        contentDescription = "Error message: $errorMessage"
                    }
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(24.dp)
                    .semantics { contentDescription = "Dismiss error message" }
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = errorColor
                )
            }
        }
    }
}

@Composable
fun PasswordStrengthIndicator(password: String) {
    val strength = calculatePasswordStrength(password)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Password Strength",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LinearProgressIndicator(
            progress = { strength.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = strength.color,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )

        Text(
            text = strength.text,
            style = MaterialTheme.typography.bodySmall,
            color = strength.color,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (password.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(top = 8.dp)
            ) {
                PasswordRequirement(
                    text = "At least 6 characters",
                    isMet = password.length >= 6
                )
                PasswordRequirement(
                    text = "Contains a number",
                    isMet = password.any { it.isDigit() }
                )
                PasswordRequirement(
                    text = "Contains a letter",
                    isMet = password.any { it.isLetter() }
                )
            }
        }
    }
}

@Composable
private fun PasswordRequirement(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isMet) Color(0xFF6D28D9) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isMet) Color(0xFF6D28D9) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

private data class PasswordStrength(
    val progress: Float,
    val color: Color,
    val text: String
)

private fun calculatePasswordStrength(password: String): PasswordStrength {
    var score = 0

    if (password.length >= 6) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { it.isLetter() }) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++

    return when (score) {
        0, 1 -> PasswordStrength(
            progress = 0.2f,
            color = Color(0xFFE11D48),   // rose-600 — tidak terlalu menyala
            text = "Terlalu lemah"
        )
        2 -> PasswordStrength(
            progress = 0.4f,
            color = Color(0xFFD97706),   // amber-600 — hangat, tidak norak
            text = "Lemah"
        )
        3 -> PasswordStrength(
            progress = 0.6f,
            color = Color(0xFF7C3AED),   // violet-600 — warna primer app
            text = "Cukup"
        )
        4 -> PasswordStrength(
            progress = 0.8f,
            color = Color(0xFF0D9488),   // teal-600 — calmer green
            text = "Kuat"
        )
        else -> PasswordStrength(
            progress = 1.0f,
            color = Color(0xFF059669),   // emerald-600 — bukan raw Green
            text = "Sangat Kuat"
        )
    }
}


