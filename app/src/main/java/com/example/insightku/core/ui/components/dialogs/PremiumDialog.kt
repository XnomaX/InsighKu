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
import com.example.insightku.core.ui.theme.AppPalette
import kotlinx.coroutines.delay

// ─── Dialog Types ──────────────────────────────────────────────────────────────

enum class PremiumDialogType(
    val icon: ImageVector,
    val accentColor: Color,
    val backgroundColor: Color,
    val title: String
) {
    SUCCESS(
        icon = Icons.Outlined.CheckCircle,
        accentColor = Color(0xFF10B981),
        backgroundColor = Color(0xFFECFDF5),
        title = "Success"
    ),
    WARNING(
        icon = Icons.Outlined.Warning,
        accentColor = Color(0xFFF59E0B),
        backgroundColor = Color(0xFFFFFBEB),
        title = "Warning"
    ),
    ERROR(
        icon = Icons.Outlined.Error,
        accentColor = Color(0xFFEF4444),
        backgroundColor = Color(0xFFFEF2F2),
        title = "Error"
    ),
    INFO(
        icon = Icons.Outlined.Info,
        accentColor = Color(0xFF3B82F6),
        backgroundColor = Color(0xFFEFF6FF),
        title = "Information"
    ),
    DELETE(
        icon = Icons.Outlined.Delete,
        accentColor = Color(0xFFEF4444),
        backgroundColor = Color(0xFFFEF2F2),
        title = "Delete"
    ),
    ARCHIVE(
        icon = Icons.Outlined.Archive,
        accentColor = Color(0xFFF59E0B),
        backgroundColor = Color(0xFFFFFBEB),
        title = "Archive"
    )
}

// ─── Premium Dialog ────────────────────────────────────────────────────────────

@Composable
fun PremiumDialog(
    type: PremiumDialogType,
    title: String,
    message: String,
    onDismiss: () -> Unit,
    confirmText: String = "Confirm",
    onConfirm: () -> Unit,
    dismissText: String = "Cancel",
    showDismissButton: Boolean = true,
    customIcon: ImageVector? = null,
    customAccentColor: Color? = null,
    properties: DialogProperties = DialogProperties()
) {
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
                    var iconVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { iconVisible = true }

                    val iconScale by animateFloatAsState(
                        targetValue = if (iconVisible) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = 0.6f,
                            stiffness = 200f
                        ),
                        label = "iconScale"
                    )
                    val iconRotation by animateFloatAsState(
                        targetValue = if (iconVisible) 0f else -90f,
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = 150f
                        ),
                        label = "iconRotation"
                    )

                    val accentColor = customAccentColor ?: type.accentColor
                    val icon = customIcon ?: type.icon

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(iconScale)
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
                                .graphicsLayer { rotationZ = iconRotation }
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Text Content with fade-in ──
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

                    Spacer(Modifier.height(28.dp))

                    // ── Buttons ──
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Primary button
                        Button(
                            onClick = {
                                onConfirm()
                                onDismiss()
                            },
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

                        // Secondary button
                        if (showDismissButton) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable { onDismiss() },
                                shape = RoundedCornerShape(14.dp),
                                color = AppPalette.card,
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
                }
            }
        }
    }
}

// ─── Premium Success Overlay ───────────────────────────────────────────────────

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
                            .background(Color(0xFF10B981).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF10B981),
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
                                text = "Success",
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

// ─── Premium Delete Confirmation ───────────────────────────────────────────────

@Composable
fun PremiumDeleteConfirmDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    message: String? = null
) {
    PremiumDialog(
        type = PremiumDialogType.DELETE,
        title = "Delete $itemName?",
        message = message ?: "This action cannot be undone. The item will be permanently removed.",
        onDismiss = onDismiss,
        confirmText = "Delete",
        onConfirm = onConfirm,
        dismissText = "Cancel"
    )
}

// ─── Premium Archive Confirmation ──────────────────────────────────────────────

@Composable
fun PremiumArchiveConfirmDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    message: String? = null
) {
    PremiumDialog(
        type = PremiumDialogType.ARCHIVE,
        title = "Archive $itemName?",
        message = message ?: "You can view archived items in settings.",
        onDismiss = onDismiss,
        confirmText = "Archive",
        onConfirm = onConfirm,
        dismissText = "Cancel"
    )
}

// ─── Premium Warning Dialog ────────────────────────────────────────────────────

@Composable
fun PremiumWarningDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel"
) {
    PremiumDialog(
        type = PremiumDialogType.WARNING,
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmText = confirmText,
        onConfirm = onConfirm,
        dismissText = dismissText
    )
}

// ─── Premium Error Dialog ──────────────────────────────────────────────────────

@Composable
fun PremiumErrorDialog(
    title: String = "Error",
    message: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    retryText: String = "Retry"
) {
    PremiumDialog(
        type = PremiumDialogType.ERROR,
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmText = if (onRetry != null) retryText else "OK",
        onConfirm = { onRetry?.invoke() },
        showDismissButton = onRetry != null
    )
}

// ─── Premium Info Dialog ───────────────────────────────────────────────────────

@Composable
fun PremiumInfoDialog(
    title: String = "Information",
    message: String,
    onDismiss: () -> Unit,
    confirmText: String = "Got it"
) {
    PremiumDialog(
        type = PremiumDialogType.INFO,
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmText = confirmText,
        onConfirm = { },
        showDismissButton = false
    )
}

// ─── Premium Logout Confirmation ───────────────────────────────────────────────

@Composable
fun PremiumLogoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    PremiumDialog(
        type = PremiumDialogType.DELETE,
        title = "Log Out?",
        message = "Are you sure you want to log out? You'll need to sign in again to access your account.",
        onDismiss = onDismiss,
        confirmText = "Log Out",
        onConfirm = onConfirm,
        dismissText = "Stay"
    )
}
