package com.example.insightku.ui.components.auth.login

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.example.insightku.data.model.UserData
import com.example.insightku.viewmodel.LoginViewModel



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (UserData) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val viewModel: LoginViewModel = hiltViewModel()
    val uiState = viewModel.uiState
    var showPassword by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val passwordFocusRequester = remember { FocusRequester() }

    // Theme colors
    val primaryPurple = MaterialTheme.colorScheme.primary
    val lightGrayBg = MaterialTheme.colorScheme.surfaceVariant
    val errorColor = MaterialTheme.colorScheme.error
    val successColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Handle login success
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onLoginSuccess(UserData(name = "User", email = uiState.email))
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
            .semantics { contentDescription = "Login screen" },
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
            text = "Welcome to InsightKu",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Unlock powerful insights for better decisions",
            style = MaterialTheme.typography.bodyMedium,
            color = onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Login Form within a Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Email Field with validation
                EmailInputField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEvent(LoginEvent.EmailChanged(it)) },
                    isError = uiState.emailError != null,
                    errorMessage = uiState.emailError,
                    onNext = { passwordFocusRequester.requestFocus() },
                    lightGrayBg = lightGrayBg,
                    errorColor = errorColor,
                    successColor = successColor,
                    onSurfaceVariant = onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field with validation
                PasswordInputField(
                    value = uiState.password,
                    onValueChange = { viewModel.onEvent(LoginEvent.PasswordChanged(it)) },
                    isError = uiState.passwordError != null,
                    errorMessage = uiState.passwordError,
                    showPassword = showPassword,
                    onTogglePasswordVisibility = { showPassword = !showPassword },
                    onDone = {
                        if (uiState.isLoginEnabled) {
                            viewModel.onEvent(LoginEvent.Submit)
                        }
                    },
                    focusRequester = passwordFocusRequester,
                    lightGrayBg = lightGrayBg,
                    errorColor = errorColor,
                    successColor = successColor,
                    onSurfaceVariant = onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // General Error Display
                if (uiState.error != null) {
                    ErrorCard(
                        errorMessage = uiState.error,
                        onDismiss = { viewModel.clearError() },
                        errorColor = errorColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Forgot Password
                TextButton(
                    onClick = onNavigateToForgotPassword,
                    modifier = Modifier
                        .align(Alignment.End)
                        .semantics { contentDescription = "Forgot password link" }
                ) {
                    Text(text = "Forgot password?", color = primaryPurple)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Login Button
                Button(
                    onClick = { viewModel.onEvent(LoginEvent.Submit) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .semantics {
                            contentDescription = if (uiState.isLoading) {
                                "Logging in, please wait"
                            } else if (uiState.isLoginEnabled) {
                                "Login button, enabled"
                            } else {
                                "Login button, disabled. Please fill in valid email and password"
                            }
                        },
                    enabled = uiState.isLoginEnabled && !uiState.isLoading,
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
                        Text("Login", fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimary)
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

        // Login with Google Button
        OutlinedButton(
            onClick = { /* TODO: Implement Google Login */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics { contentDescription = "Login with Google" },
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
                    tint = Color.Unspecified // Penting: hindari tinting agar warna asli Google tetap terlihat
                )

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Login with Google",
                    color = Color.Black,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign Up Text
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics { contentDescription = "Sign up section" }
        ) {
            Text(
                text = "Don't have an account?",
                color = onSurfaceVariant,
                fontSize = 13.sp,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(
                onClick = onNavigateToSignUp,
                modifier = Modifier.semantics { contentDescription = "Sign up for InsightKu link" },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Sign up for InsightKu",
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
            label = { Text("Email") },
            placeholder = { Text("Enter your email") },
            modifier = Modifier
                .fillMaxWidth()
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
            label = { Text("Password") },
            placeholder = { Text("Enter your password") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .semantics {
                    contentDescription = if (isError) {
                        "Password input field, error: ${errorMessage ?: "Invalid password"}"
                    } else {
                        "Password input field"
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
                            contentDescription = if (isError) "Invalid password" else "Valid password",
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

        // Error message
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = errorColor,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(start = 16.dp, top = 4.dp)
                    .semantics { contentDescription = "Password error: $errorMessage" }
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

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun LoginScreenElegantPreview() {
    LoginScreen(
        onLoginSuccess = {},
        onNavigateToSignUp = {},
        onNavigateToForgotPassword = {},
        onBack = {}
    )
}
