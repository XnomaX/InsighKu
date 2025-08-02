package com.example.insightku.ui.components.auth.signup

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.R
import com.example.insightku.viewmodel.SignUpViewModel
import com.example.insightku.data.model.UserData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUpSuccess: (UserData) -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val viewModel: SignUpViewModel = hiltViewModel()
    val uiState = viewModel.uiState
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(scrollState)
            .semantics { contentDescription = "Sign up screen" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = primaryPurple.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = "InsightKu logo",
                modifier = Modifier.size(40.dp),
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

        Spacer(modifier = Modifier.height(32.dp))

        // Sign Up Form within a Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

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

                Spacer(modifier = Modifier.height(16.dp))

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
                    Spacer(modifier = Modifier.height(8.dp))
                    PasswordStrengthIndicator(password = uiState.password)
                }

                Spacer(modifier = Modifier.height(16.dp))

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

                Spacer(modifier = Modifier.height(16.dp))

                // General Error Display
                if (uiState.error != null) {
                    ErrorCard(
                        errorMessage = uiState.error!!,
                        onDismiss = { viewModel.clearError() },
                        errorColor = errorColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Sign Up Button
                Button(
                    onClick = { viewModel.onEvent(SignUpEvent.Submit) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
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
                    shape = CircleShape
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

        Spacer(modifier = Modifier.height(24.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = onSurfaceVariant.copy(alpha = 0.3f)
            )
            Text(
                text = "or continue with",
                color = onSurfaceVariant,
                fontSize = 12.sp,
                style = MaterialTheme.typography.bodySmall
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = onSurfaceVariant.copy(alpha = 0.3f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Up with Google Button
        OutlinedButton(
            onClick = { /* TODO: Implement Google Sign Up */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics { contentDescription = "Sign up with Google" },
            shape = RoundedCornerShape(50.dp),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign up with Google",
                    color = Color.Black,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Login Text
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

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun NameInputField(
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
            shape = RoundedCornerShape(12.dp),
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
            shape = RoundedCornerShape(12.dp),
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
            shape = RoundedCornerShape(12.dp),
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
            tint = if (isMet) Color.Green else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isMet) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
fun SignUpScreenPreview() {
    SignUpScreen(
        onSignUpSuccess = {},
        onNavigateToLogin = {}
    )
}

@Preview
@Composable
fun PasswordStrengthIndicatorPreview() {
    PasswordStrengthIndicator(password = "Password123!")
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
            color = Color.Red,
            text = "Very Weak"
        )
        2 -> PasswordStrength(
            progress = 0.4f,
            color = Color(0xFFFF9800),
            text = "Weak"
        )
        3 -> PasswordStrength(
            progress = 0.6f,
            color = Color(0xFFFFEB3B),
            text = "Fair"
        )
        4 -> PasswordStrength(
            progress = 0.8f,
            color = Color(0xFF4CAF50),
            text = "Good"
        )
        else -> PasswordStrength(
            progress = 1.0f,
            color = Color.Green,
            text = "Strong"
        )
    }
}
