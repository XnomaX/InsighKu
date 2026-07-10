package com.example.insightku.core.ui.components.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════════════
// CONTEXT-AWARE DIALOG TYPE SYSTEM
// ══════════════════════════════════════════════════════════════════════════════
//
// Every dialog type has a unique:
//   - Icon (vector drawable)
//   - Accent color (semantic color from AppPalette)
//   - Background tint (subtle alpha for the icon container)
//   - Button style (destructive = filled red, positive = filled accent, neutral = outlined)
//   - Animation behavior (unique spring/delay per type)
//
// The InsightDialog composable automatically selects the right visual identity
// based on the DialogType, so feature code only specifies:
//   DialogType, Title, Description, Actions
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Button style determines how the primary and secondary actions are rendered.
 */
enum class DialogButtonStyle {
    /** Destructive: filled red primary + outlined secondary. Used by Delete. */
    DESTRUCTIVE,
    /** Positive: filled accent primary + outlined secondary. Used by Success, Goal, Allocation, Budget. */
    POSITIVE,
    /** Neutral: filled primary + text-button secondary. Used by Info, Warning. */
    NEUTRAL,
    /** Warning: filled amber primary + outlined secondary. Used by Warning, Archive. */
    WARNING_STYLE
}

/**
 * Visual identity for each context-aware dialog type.
 *
 * @param icon          Unique icon vector for this dialog type
 * @param accentColor   Primary accent color applied to icon tint + button fill
 * @param iconTint      Override tint for the icon (defaults to accentColor)
 * @param buttonStyle   Controls primary/secondary button rendering
 * @param defaultTitle  Fallback title when caller doesn't provide one
 */
enum class DialogType(
    val icon: ImageVector,
    val accentColor: Color,
    val buttonStyle: DialogButtonStyle,
    val defaultTitle: String
) {
    // NOTE: Raw Color values must be used here — AppPalette colors are @Composable
    // and cannot be referenced in enum constructors.
    SUCCESS(
        icon = Icons.Outlined.CheckCircle,
        accentColor = Color(0xFF10B981),   // AppPalette.success
        buttonStyle = DialogButtonStyle.POSITIVE,
        defaultTitle = "Success"
    ),
    WARNING(
        icon = Icons.Outlined.Warning,
        accentColor = Color(0xFFF59E0B),   // AppPalette.warning
        buttonStyle = DialogButtonStyle.WARNING_STYLE,
        defaultTitle = "Warning"
    ),
    ERROR(
        icon = Icons.Outlined.ErrorOutline,
        accentColor = Color(0xFFEF4444),   // AppPalette.error
        buttonStyle = DialogButtonStyle.POSITIVE,
        defaultTitle = "Error"
    ),
    DELETE(
        icon = Icons.Outlined.Delete,
        accentColor = Color(0xFFEF4444),   // AppPalette.error
        buttonStyle = DialogButtonStyle.DESTRUCTIVE,
        defaultTitle = "Delete"
    ),
    ARCHIVE(
        icon = Icons.Outlined.Archive,
        accentColor = Color(0xFFF59E0B),   // AppPalette.warning
        buttonStyle = DialogButtonStyle.WARNING_STYLE,
        defaultTitle = "Archive"
    ),
    ALLOCATION(
        icon = Icons.Outlined.AccountBalance,
        accentColor = Color(0xFF06B6D4),   // AppPalette.cyan
        buttonStyle = DialogButtonStyle.POSITIVE,
        defaultTitle = "Allocation"
    ),
    GOAL(
        icon = Icons.Outlined.Flag,
        accentColor = Color(0xFF7C4DFF),   // AppPalette.accent
        buttonStyle = DialogButtonStyle.POSITIVE,
        defaultTitle = "Goal"
    ),
    BUDGET(
        icon = Icons.Outlined.Savings,
        accentColor = Color(0xFF8B5CF6),   // violet
        buttonStyle = DialogButtonStyle.POSITIVE,
        defaultTitle = "Budget"
    ),
    ACCOUNT(
        icon = Icons.Outlined.CreditCard,
        accentColor = Color(0xFF3B82F6),   // AppPalette.defaultBlue
        buttonStyle = DialogButtonStyle.POSITIVE,
        defaultTitle = "Account"
    ),
    INFORMATION(
        icon = Icons.Outlined.Lightbulb,
        accentColor = Color(0xFF3B82F6),   // AppPalette.defaultBlue
        buttonStyle = DialogButtonStyle.NEUTRAL,
        defaultTitle = "Information"
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// LEGACY TYPE ALIAS (backward compat — keeps existing PremiumDialogType usages)
// ══════════════════════════════════════════════════════════════════════════════

typealias PremiumDialogType = DialogType

// ══════════════════════════════════════════════════════════════════════════════
// InsightDialog — THE REUSABLE CORE
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Premium, context-aware alert dialog that automatically selects:
 *   - Unique icon per [type]
 *   - Unique accent color per [type]
 *   - Unique button styling per [type]
 *   - Smooth entrance animations (scale + fade + slide)
 *
 * Feature code only specifies: type, title, description, actions.
 * Everything else is handled automatically.
 */
@Composable
fun InsightDialog(
    type: DialogType,
    title: String,
    message: String,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(R.string.dialog_confirm),
    onConfirm: () -> Unit,
    dismissText: String = stringResource(R.string.cancel),
    showDismissButton: Boolean = true,
    customIcon: ImageVector? = null,
    customAccentColor: Color? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties()
) {
    val accentColor = customAccentColor ?: type.accentColor
    val icon = customIcon ?: type.icon

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Dialog(
        onDismissRequest = onDismiss,
        properties = properties
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)) + slideInVertically(
                animationSpec = tween(300),
                initialOffsetY = { it / 8 }
            ),
            exit = fadeOut(tween(150))
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
                border = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // ── Animated Icon Container ──
                    InsightDialogAnimatedIcon(
                        icon = icon,
                        accentColor = accentColor,
                        type = type
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── Text Content with fade-in ──
                    InsightDialogTextContent(
                        title = title,
                        message = message
                    )

                    // ── Optional supporting content (e.g. extra info cards) ──
                    if (supportingContent != null) {
                        Spacer(Modifier.height(16.dp))
                        supportingContent()
                    }

                    Spacer(Modifier.height(28.dp))

                    // ── Buttons styled per dialog type ──
                    InsightDialogButtons(
                        type = type,
                        accentColor = accentColor,
                        confirmText = confirmText,
                        dismissText = dismissText,
                        showDismissButton = showDismissButton,
                        onConfirm = { onConfirm(); onDismiss() },
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ANIMATED ICON — unique spring per dialog type
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun InsightDialogAnimatedIcon(
    icon: ImageVector,
    accentColor: Color,
    type: DialogType
) {
    var iconVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { iconVisible = true }

    // ── Visually distinct Animatable + keyframes per dialog type ──
    when (type) {
        DialogType.SUCCESS -> {
            // Smooth settle: gentle bounce, no rotation
            val scale = remember { Animatable(0f) }
            val rotation = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                if (iconVisible) {
                    scale.animateTo(1f, keyframes {
                        durationMillis = 300
                        0f at 0 with LinearEasing
                        1.1f at 150  // overshoot
                        0.95f at 220 // settle back
                        1f at 300    // final
                    })
                }
            }
            IconContainer(icon, accentColor, scale.value, rotation.value)
        }
        DialogType.DELETE -> {
            // Sharp snap: aggressive bounce + quick rotation
            val scale = remember { Animatable(0f) }
            val rotation = remember { Animatable(-45f) }
            LaunchedEffect(Unit) {
                if (iconVisible) {
                    launch { scale.animateTo(1f, keyframes {
                        durationMillis = 250
                        0f at 0
                        1.3f at 100  // aggressive overshoot
                        0.85f at 170 // bounce back
                        1f at 250
                    }) }
                    launch { rotation.animateTo(0f, keyframes {
                        durationMillis = 200
                        -45f at 0
                        5f at 120    // slight overshoot past 0
                        0f at 200
                    }) }
                }
            }
            IconContainer(icon, accentColor, scale.value, rotation.value)
        }
        DialogType.ERROR -> {
            // Tense shake: scale up with horizontal wobble via Animatable + keyframes
            val scale = remember { Animatable(0f) }
            val shakeOffset = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                if (iconVisible) {
                    launch { scale.animateTo(1f, keyframes {
                        durationMillis = 250
                        0f at 0
                        1.2f at 80   // tense overshoot
                        0.9f at 160  // bounce back
                        1f at 250
                    }) }
                    launch {
                        delay(150)
                        shakeOffset.animateTo(0f, keyframes {
                            durationMillis = 240
                            0f at 0
                            6f at 40    // right
                            -6f at 80   // left
                            4f at 120   // right (smaller)
                            -3f at 160  // left (smaller)
                            2f at 200   // right (dampening)
                            0f at 240   // settle
                        })
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale.value)
                    .offset(x = shakeOffset.value.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        DialogType.WARNING -> {
            // Moderate pulse: scale with rotation
            val scale = remember { Animatable(0f) }
            val rotation = remember { Animatable(15f) }
            LaunchedEffect(Unit) {
                if (iconVisible) {
                    launch { scale.animateTo(1f, keyframes {
                        durationMillis = 300
                        0f at 0
                        1.15f at 120 // overshoot
                        0.97f at 200 // settle back
                        1f at 300
                    }) }
                    launch { rotation.animateTo(0f, keyframes {
                        durationMillis = 350
                        15f at 0
                        -5f at 180   // slight counter-overshoot
                        0f at 350
                    }) }
                }
            }
            IconContainer(icon, accentColor, scale.value, rotation.value)
        }
        DialogType.GOAL -> {
            // Celebratory: large bounce with full rotation
            val scale = remember { Animatable(0f) }
            val rotation = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                if (iconVisible) {
                    launch { scale.animateTo(1f, keyframes {
                        durationMillis = 400
                        0f at 0
                        1.4f at 150  // big overshoot
                        0.8f at 250  // deep bounce back
                        1.1f at 320  // recover
                        1f at 400
                    }) }
                    launch { rotation.animateTo(360f, keyframes {
                        durationMillis = 500
                        0f at 0 with LinearEasing
                        360f at 500
                    }) }
                }
            }
            IconContainer(icon, accentColor, scale.value, rotation.value)
        }
        DialogType.ALLOCATION -> {
            // Precise: smooth scale from 0.5, no rotation
            val scale = remember { Animatable(0.5f) }
            val rotation = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                if (iconVisible) {
                    scale.animateTo(1f, keyframes {
                        durationMillis = 280
                        0.5f at 0
                        1.08f at 120 // subtle overshoot
                        0.98f at 200 // settle
                        1f at 280
                    })
                }
            }
            IconContainer(icon, accentColor, scale.value, rotation.value)
        }
        DialogType.BUDGET -> {
            // Balanced: moderate bounce with slight rotation
            val scale = remember { Animatable(0f) }
            val rotation = remember { Animatable(-20f) }
            LaunchedEffect(Unit) {
                if (iconVisible) {
                    launch { scale.animateTo(1f, keyframes {
                        durationMillis = 320
                        0f at 0
                        1.2f at 130  // overshoot
                        0.95f at 220 // settle back
                        1f at 320
                    }) }
                    launch { rotation.animateTo(0f, keyframes {
                        durationMillis = 300
                        -20f at 0
                        3f at 180    // slight counter-overshoot
                        0f at 300
                    }) }
                }
            }
            IconContainer(icon, accentColor, scale.value, rotation.value)
        }
        DialogType.ACCOUNT -> {
            // Stable: smooth settle, minimal rotation
            val scale = remember { Animatable(0f) }
            val rotation = remember { Animatable(10f) }
            LaunchedEffect(Unit) {
                if (iconVisible) {
                    launch { scale.animateTo(1f, keyframes {
                        durationMillis = 300
                        0f at 0
                        1.1f at 140  // gentle overshoot
                        1f at 300
                    }) }
                    launch { rotation.animateTo(0f, keyframes {
                        durationMillis = 250
                        10f at 0
                        0f at 250
                    }) }
                }
            }
            IconContainer(icon, accentColor, scale.value, rotation.value)
        }
        DialogType.ARCHIVE -> {
            // Gentle: slow settle from 0.8, downward rotation hint
            val scale = remember { Animatable(0.8f) }
            val rotation = remember { Animatable(30f) }
            LaunchedEffect(Unit) {
                if (iconVisible) {
                    launch { scale.animateTo(1f, keyframes {
                        durationMillis = 400
                        0.8f at 0
                        1.05f at 200 // subtle overshoot
                        1f at 400
                    }) }
                    launch { rotation.animateTo(0f, keyframes {
                        durationMillis = 450
                        30f at 0
                        -3f at 300   // slight settle-back
                        0f at 450
                    }) }
                }
            }
            IconContainer(icon, accentColor, scale.value, rotation.value)
        }
        DialogType.INFORMATION -> {
            // Calm: gentle scale from 0.9, no rotation
            val scale = remember { Animatable(0.9f) }
            val rotation = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                if (iconVisible) {
                    scale.animateTo(1f, keyframes {
                        durationMillis = 350
                        0.9f at 0
                        1.03f at 180 // very subtle overshoot
                        1f at 350
                    })
                }
            }
            IconContainer(icon, accentColor, scale.value, rotation.value)
        }
    }
}

@Composable
private fun IconContainer(
    icon: ImageVector,
    accentColor: Color,
    scale: Float,
    rotation: Float
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer { rotationZ = rotation }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TEXT CONTENT — staggered fade-in
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun InsightDialogTextContent(
    title: String,
    message: String
) {
    var textVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(200)
        textVisible = true
    }

    AnimatedVisibility(
        visible = textVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppPalette.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.textMuted,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// BUTTONS — styled per [DialogButtonStyle]
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun InsightDialogButtons(
    type: DialogType,
    accentColor: Color,
    confirmText: String,
    dismissText: String,
    showDismissButton: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        when (type.buttonStyle) {
            DialogButtonStyle.DESTRUCTIVE -> {
                // Primary: filled red
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppPalette.error,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        text = confirmText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Secondary: outlined
                if (showDismissButton) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { onDismiss() },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, AppPalette.cardBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = dismissText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppPalette.textMuted
                            )
                        }
                    }
                }
            }

            DialogButtonStyle.POSITIVE -> {
                // Primary: filled accent
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        text = confirmText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Secondary: outlined
                if (showDismissButton) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { onDismiss() },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, AppPalette.cardBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = dismissText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppPalette.textMuted
                            )
                        }
                    }
                }
            }

            DialogButtonStyle.WARNING_STYLE -> {
                // Primary: filled amber/warning
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        text = confirmText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Secondary: outlined
                if (showDismissButton) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { onDismiss() },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, AppPalette.cardBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = dismissText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppPalette.textMuted
                            )
                        }
                    }
                }
            }

            DialogButtonStyle.NEUTRAL -> {
                // Primary: filled accent
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        text = confirmText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Secondary: text button (no border)
                if (showDismissButton) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = dismissText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppPalette.textMuted
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// LEGACY ALIAS — PremiumDialog (delegates to InsightDialog)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun PremiumDialog(
    type: PremiumDialogType,
    title: String,
    message: String,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(R.string.dialog_confirm),
    onConfirm: () -> Unit,
    dismissText: String = stringResource(R.string.cancel),
    showDismissButton: Boolean = true,
    customIcon: ImageVector? = null,
    customAccentColor: Color? = null,
    properties: DialogProperties = DialogProperties()
) {
    InsightDialog(
        type = type,
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmText = confirmText,
        onConfirm = onConfirm,
        dismissText = dismissText,
        showDismissButton = showDismissButton,
        customIcon = customIcon,
        customAccentColor = customAccentColor,
        properties = properties
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// SPECIALIZED DIALOG COMPOSABLES
// ══════════════════════════════════════════════════════════════════════════════

// ─── Success ──────────────────────────────────────────────────────────────────

@Composable
fun InsightSuccessDialog(
    title: String = stringResource(R.string.success),
    message: String,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(R.string.dialog_ok)
) {
    InsightDialog(
        type = DialogType.SUCCESS,
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmText = confirmText,
        onConfirm = { },
        showDismissButton = false
    )
}

// ─── Warning ──────────────────────────────────────────────────────────────────

@Composable
fun InsightWarningDialog(
    title: String = stringResource(R.string.warning),
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = stringResource(R.string.dialog_continue),
    dismissText: String = stringResource(R.string.cancel)
) {
    InsightDialog(
        type = DialogType.WARNING,
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmText = confirmText,
        onConfirm = onConfirm,
        dismissText = dismissText
    )
}

// ─── Error ────────────────────────────────────────────────────────────────────

@Composable
fun InsightErrorDialog(
    title: String = stringResource(R.string.error),
    message: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    retryText: String = stringResource(R.string.transaction_retry)
) {
    InsightDialog(
        type = DialogType.ERROR,
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmText = if (onRetry != null) retryText else stringResource(R.string.dialog_ok),
        onConfirm = { onRetry?.invoke() },
        showDismissButton = onRetry != null
    )
}

// ─── Delete ───────────────────────────────────────────────────────────────────

@Composable
fun InsightDeleteDialog(
    itemName: String,
    message: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = stringResource(R.string.delete)
) {
    InsightDialog(
        type = DialogType.DELETE,
        title = stringResource(R.string.dialog_delete_title, itemName),
        message = message ?: stringResource(R.string.dialog_delete_message),
        onDismiss = onDismiss,
        confirmText = confirmText,
        onConfirm = onConfirm,
        dismissText = stringResource(R.string.cancel)
    )
}

// ─── Archive ──────────────────────────────────────────────────────────────────

@Composable
fun InsightArchiveDialog(
    itemName: String,
    message: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    InsightDialog(
        type = DialogType.ARCHIVE,
        title = stringResource(R.string.dialog_archive_title, itemName),
        message = message ?: stringResource(R.string.dialog_archive_message),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.goals_archive_confirm),
        onConfirm = onConfirm,
        dismissText = stringResource(R.string.cancel)
    )
}

// ─── Allocation ───────────────────────────────────────────────────────────────

@Composable
fun InsightAllocationDialog(
    title: String = stringResource(R.string.allocation_review_title),
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = stringResource(R.string.dialog_allocate)
) {
    InsightDialog(
        type = DialogType.ALLOCATION,
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmText = confirmText,
        onConfirm = onConfirm,
        dismissText = stringResource(R.string.cancel)
    )
}

// ─── Goal ─────────────────────────────────────────────────────────────────────

@Composable
fun InsightGoalDialog(
    title: String = stringResource(R.string.goals_title),
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = stringResource(R.string.dialog_confirm),
    dismissText: String = stringResource(R.string.cancel)
) {
    InsightDialog(
        type = DialogType.GOAL,
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmText = confirmText,
        onConfirm = onConfirm,
        dismissText = dismissText
    )
}

// ─── Budget ───────────────────────────────────────────────────────────────────

@Composable
fun InsightBudgetDialog(
    title: String = stringResource(R.string.budgeting_title),
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = stringResource(R.string.dialog_confirm)
) {
    InsightDialog(
        type = DialogType.BUDGET,
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmText = confirmText,
        onConfirm = onConfirm,
        dismissText = stringResource(R.string.cancel)
    )
}

// ─── Account ──────────────────────────────────────────────────────────────────

@Composable
fun InsightAccountDialog(
    title: String = stringResource(R.string.accounts_title),
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = stringResource(R.string.dialog_confirm)
) {
    InsightDialog(
        type = DialogType.ACCOUNT,
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmText = confirmText,
        onConfirm = onConfirm,
        dismissText = stringResource(R.string.cancel)
    )
}

// ─── Information ──────────────────────────────────────────────────────────────

@Composable
fun InsightInfoDialog(
    title: String = stringResource(R.string.dialog_information),
    message: String,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(R.string.dialog_got_it)
) {
    InsightDialog(
        type = DialogType.INFORMATION,
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmText = confirmText,
        onConfirm = { },
        showDismissButton = false
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// LEGACY SPECIALIZED COMPOSABLES (backward compat wrappers)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun PremiumDeleteConfirmDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    message: String? = null
) {
    InsightDeleteDialog(
        itemName = itemName,
        message = message,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun PremiumArchiveConfirmDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    message: String? = null
) {
    InsightArchiveDialog(
        itemName = itemName,
        message = message,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun PremiumWarningDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = stringResource(R.string.dialog_confirm),
    dismissText: String = stringResource(R.string.cancel)
) {
    InsightWarningDialog(
        title = title,
        message = message,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        confirmText = confirmText,
        dismissText = dismissText
    )
}

@Composable
fun PremiumErrorDialog(
    title: String = stringResource(R.string.error),
    message: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    retryText: String = stringResource(R.string.transaction_retry)
) {
    InsightErrorDialog(
        title = title,
        message = message,
        onDismiss = onDismiss,
        onRetry = onRetry,
        retryText = retryText
    )
}

@Composable
fun PremiumInfoDialog(
    title: String = stringResource(R.string.dialog_information),
    message: String,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(R.string.dialog_got_it)
) {
    InsightInfoDialog(
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmText = confirmText
    )
}

@Composable
fun PremiumLogoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    InsightDialog(
        type = DialogType.DELETE,
        title = stringResource(R.string.dialog_logout_title),
        message = stringResource(R.string.dialog_logout_message),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.dialog_logout_confirm),
        onConfirm = onConfirm,
        dismissText = stringResource(R.string.dialog_stay)
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// PREMIUM SUCCESS OVERLAY (auto-dismiss variant)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun PremiumSuccessOverlay(
    message: String,
    subtitle: String? = null,
    onAutoDismiss: (() -> Unit)? = null,
    autoDismissMs: Long = 2000L
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(Unit) {
        if (onAutoDismiss != null) {
            delay(autoDismissMs)
            onAutoDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(150))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = false) { },
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = AppPalette.card),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                border = BorderStroke(1.dp, AppPalette.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Animated checkmark circle
                    var checkVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { checkVisible = true }

                    val scale by animateFloatAsState(
                        targetValue = if (checkVisible) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = 0.6f,
                            stiffness = 200f
                        ),
                        label = "checkScale"
                    )
                    val rotation by animateFloatAsState(
                        targetValue = if (checkVisible) 0f else -90f,
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = 150f
                        ),
                        label = "checkRotation"
                    )

                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(AppPalette.success.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = stringResource(R.string.cd_success),
                            tint = AppPalette.success,
                            modifier = Modifier
                                .size(52.dp)
                                .graphicsLayer { rotationZ = rotation }
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Message text with fade-in
                    var textVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(300)
                        textVisible = true
                    }

                    AnimatedVisibility(
                        visible = textVisible,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.success),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AppPalette.textPrimary
                            )
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppPalette.textMuted,
                                textAlign = TextAlign.Center
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppPalette.textMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
