package com.example.insightku.feature.auth.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.InputShape
import com.example.insightku.core.ui.theme.adaptiveDp

// ─── Auth Header ──────────────────────────────────────────────────────────────

@Composable
fun AuthHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    logoSize: Dp = adaptiveDp(64.dp, 80.dp, 96.dp)
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Branded logo
        Box(
            modifier = Modifier
                .size(logoSize)
                .background(
                    color = AppPalette.accent.copy(alpha = 0.08f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Outer glow ring
            Box(
                modifier = Modifier
                    .size(logoSize + 16.dp)
                    .background(
                        color = AppPalette.accent.copy(alpha = 0.04f),
                        shape = CircleShape
                    )
            )
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(logoSize * 0.5f),
                tint = AppPalette.accent
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            color = AppPalette.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

// ─── Auth Text Field ──────────────────────────────────────────────────────────

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector = Icons.Default.Email,
    isError: Boolean = false,
    errorMessage: String? = null,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Email,
    imeAction: ImeAction = ImeAction.Next,
    onNext: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
    singleLine: Boolean = true,
    enabled: Boolean = true
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, color = AppPalette.textMuted.copy(alpha = 0.5f)) }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .semantics {
                    contentDescription = if (isError && errorMessage != null) {
                        "$label field, error: $errorMessage"
                    } else {
                        "$label input field"
                    }
                },
            shape = InputShape,
            leadingIcon = {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = when {
                        isError -> AppPalette.error
                        value.isNotEmpty() && !isError -> AppPalette.success
                        else -> AppPalette.textMuted
                    },
                    modifier = Modifier.size(Dimens.IconSizeMedium)
                )
            },
            trailingIcon = {
                if (isPassword && onTogglePasswordVisibility != null) {
                    IconButton(
                        onClick = onTogglePasswordVisibility,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = AppPalette.textMuted,
                            modifier = Modifier.size(Dimens.IconSizeMedium)
                        )
                    }
                } else if (value.isNotEmpty() && !isPassword) {
                    Icon(
                        if (isError) Icons.Default.Error else Icons.Default.Check,
                        contentDescription = if (isError) "Invalid" else "Valid",
                        tint = if (isError) AppPalette.error else AppPalette.success,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            visualTransformation = if (isPassword && !showPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppPalette.cardElevated,
                unfocusedContainerColor = AppPalette.cardElevated,
                errorContainerColor = AppPalette.error.copy(alpha = 0.05f),
                disabledContainerColor = AppPalette.cardElevated.copy(alpha = 0.5f),
                focusedBorderColor = when {
                    isError -> AppPalette.error
                    else -> AppPalette.accent
                },
                unfocusedBorderColor = when {
                    isError -> AppPalette.error.copy(alpha = 0.5f)
                    value.isNotEmpty() -> AppPalette.success.copy(alpha = 0.3f)
                    else -> AppPalette.cardBorder
                },
                errorBorderColor = AppPalette.error.copy(alpha = 0.5f),
                focusedLabelColor = AppPalette.accent,
                unfocusedLabelColor = AppPalette.textMuted,
                errorLabelColor = AppPalette.error,
                cursorColor = AppPalette.accent,
                disabledTextColor = AppPalette.textMuted.copy(alpha = 0.5f)
            ),
            isError = isError,
            singleLine = singleLine,
            enabled = enabled,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = imeAction,
                keyboardType = keyboardType
            ),
            keyboardActions = KeyboardActions(
                onNext = { onNext?.invoke() },
                onDone = {
                    onDone?.invoke()
                    focusManager.clearFocus()
                }
            )
        )

        // Error message with animated visibility
        AnimatedVisibility(
            visible = isError && errorMessage != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut()
        ) {
            if (errorMessage != null) {
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 4.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = AppPalette.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = errorMessage,
                        color = AppPalette.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// ─── Auth Button ──────────────────────────────────────────────────────────────

@Composable
fun AuthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String = "",
    buttonColor: Color = AppPalette.accent,
    contentColor: Color = Color.White,
    height: Dp = adaptiveDp(Dimens.ButtonHeightCompact, Dimens.ButtonHeightMedium, Dimens.ButtonHeightExpanded)
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                contentDescription = when {
                    isLoading -> "$text, loading"
                    enabled -> "$text, enabled"
                    else -> "$text, disabled"
                }
            },
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            disabledContainerColor = buttonColor.copy(alpha = 0.4f),
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(Dimens.ButtonRadius),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        )
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = contentColor.copy(alpha = 0.8f),
                    strokeWidth = 2.5.dp
                )
                if (loadingText.isNotEmpty()) {
                    Text(
                        text = loadingText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─── Secondary / Tonal Button ─────────────────────────────────────────────────

@Composable
fun AuthSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val height = adaptiveDp(Dimens.ButtonHeightCompact, Dimens.ButtonHeightMedium, Dimens.ButtonHeightExpanded)

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(Dimens.ButtonRadius),
        border = BorderStroke(1.dp, AppPalette.cardBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppPalette.card,
            contentColor = AppPalette.textPrimary,
            disabledContainerColor = AppPalette.card.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = AppPalette.accent
            )
        } else {
            Text(
                text = text,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
        }
    }
}

// ─── Auth Footer ──────────────────────────────────────────────────────────────

@Composable
fun AuthFooter(
    promptText: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = promptText,
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.textMuted
        )
        Spacer(modifier = Modifier.width(4.dp))
        TextButton(
            onClick = onActionClick,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Text(
                text = actionText,
                color = AppPalette.accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

// ─── Auth Error Card ──────────────────────────────────────────────────────────

@Composable
fun AuthErrorCard(
    errorMessage: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppPalette.error.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, AppPalette.error.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = AppPalette.error,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.error,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_dismiss_error),
                    modifier = Modifier.size(14.dp),
                    tint = AppPalette.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ─── Auth Divider ─────────────────────────────────────────────────────────────

@Composable
fun AuthDivider(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.or_continue_with)
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = AppPalette.cardBorder,
            thickness = 1.dp
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = AppPalette.textMuted
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = AppPalette.cardBorder,
            thickness = 1.dp
        )
    }
}

// ─── Google Auth Button ───────────────────────────────────────────────────────

@Composable
fun GoogleAuthButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    text: String = stringResource(R.string.login_with_google)
) {
    val height = adaptiveDp(Dimens.ButtonHeightCompact, Dimens.ButtonHeightMedium, Dimens.ButtonHeightExpanded)

    OutlinedButton(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(Dimens.ButtonRadius),
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
                color = AppPalette.accent
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = AppPalette.textPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
        }
    }
}

// ─── Form Card Container ──────────────────────────────────────────────────────

@Composable
fun AuthFormCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadiusLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationSmall),
        colors = CardDefaults.cardColors(containerColor = AppPalette.card),
        border = BorderStroke(1.dp, AppPalette.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(
                adaptiveDp(
                    Dimens.CardInnerPaddingCompact,
                    Dimens.CardInnerPaddingMedium,
                    Dimens.CardInnerPaddingExpanded
                )
            ),
            content = content
        )
    }
}
