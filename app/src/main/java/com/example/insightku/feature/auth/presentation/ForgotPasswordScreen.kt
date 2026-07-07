package com.example.insightku.feature.auth.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.adaptiveDp
import com.example.insightku.feature.auth.presentation.components.*

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ForgotPasswordScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToLogin = onNavigateToLogin
    )
}

@Composable
private fun ForgotPasswordScreenContent(
    uiState: ForgotPasswordState,
    onEvent: (ForgotPasswordEvent) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val scrollState = rememberScrollState()
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
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Dimens.PaddingExtraLarge))

            // ─── Back Button ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppPalette.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(formSpacing))

            // ─── Header ──────────────────────────────────────────────────
            AnimatedContent(
                targetState = uiState.isEmailSent,
                transitionSpec = {
                    fadeIn() + slideInVertically(initialOffsetY = { it / 4 }) togetherWith
                        fadeOut() + slideOutVertically(targetOffsetY = { -it / 4 })
                },
                label = "forgot_password_content"
            ) { emailSent ->
                if (!emailSent) {
                    // ─── Request Form ────────────────────────────────────
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Logo
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    color = AppPalette.accent.copy(alpha = 0.08f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = AppPalette.accent
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.forgot_password_title),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            color = AppPalette.textPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.forgot_password_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppPalette.textMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(formSpacing * 2))

                        // Form Card
                        AuthFormCard {
                            AuthTextField(
                                value = uiState.email,
                                onValueChange = { onEvent(ForgotPasswordEvent.EmailChanged(it.trim())) },
                                label = stringResource(R.string.email_label),
                                placeholder = stringResource(R.string.forgot_password_email_placeholder),
                                leadingIcon = Icons.Default.Email,
                                isError = uiState.error != null,
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                                onDone = {
                                    if (uiState.email.isNotBlank() && !uiState.isLoading) {
                                        onEvent(ForgotPasswordEvent.Submit)
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(formSpacing * 2))

                            AuthButton(
                                text = stringResource(R.string.forgot_password_button),
                                onClick = { onEvent(ForgotPasswordEvent.Submit) },
                                enabled = uiState.email.isNotBlank(),
                                isLoading = uiState.isLoading,
                                loadingText = stringResource(R.string.sending_reset_link)
                            )
                        }

                        Spacer(modifier = Modifier.height(formSpacing * 2))

                        // Back to Sign In
                        AuthSecondaryButton(
                            text = stringResource(R.string.back_to_sign_in),
                            onClick = onNavigateToLogin
                        )
                    }
                } else {
                    // ─── Success State ────────────────────────────────────
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(formSpacing * 2))

                        // Success Icon
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(
                                    color = AppPalette.success.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AppPalette.success,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.reset_link_sent_title),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            color = AppPalette.textPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.reset_link_sent_subtitle, uiState.email),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppPalette.textMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(formSpacing * 3))

                        AuthButton(
                            text = stringResource(R.string.back_to_sign_in),
                            onClick = onNavigateToLogin,
                            buttonColor = AppPalette.success
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.PaddingExtraLarge))
        }
    }
}
