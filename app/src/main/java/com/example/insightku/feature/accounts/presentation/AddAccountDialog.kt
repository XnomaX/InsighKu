package com.example.insightku.feature.accounts.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.insightku.core.data.model.AccountType
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.ui.theme.Dimens
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.utils.CurrencyUtils

private val SheetPurple: Color @Composable get() = LocalAccent.current
private val SheetBorder: Color @Composable get() = AppPalette.cardBorder
private val SheetBg: Color @Composable get() = AppPalette.background

/**
 * Add Account Dialog — Modal Bottom Sheet for creating new accounts.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddAccountDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onAccountCreated: () -> Unit,
    viewModel: AddAccountViewModel = hiltViewModel()
) {
    if (!isOpen) return

    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(isOpen) {
        if (isOpen) viewModel.resetForm()
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onAccountCreated()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppPalette.card,
        dragHandle = {
            Box(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(SheetBorder)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(SheetBg)
                .padding(24.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SheetPurple.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = "New Account",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SheetPurple
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Add Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.textPrimary
                )
                Text(
                    text = "Track your money, cards, and assets",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(SheetBorder))

            Spacer(Modifier.height(18.dp))

            // Account Type
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ACCOUNT TYPE",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountType.entries.forEach { type ->
                        AccountTypeChip(
                            type = type,
                            isSelected = uiState.selectedType == type,
                            onClick = { viewModel.onEvent(AddAccountEvent.UpdateAccountType(type)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Account Name
            AccountFormField(
                label = "ACCOUNT NAME",
                value = uiState.accountName,
                onValueChange = { viewModel.onEvent(AddAccountEvent.UpdateAccountName(it)) },
                placeholder = "e.g., BCA Savings, GoPay",
                error = uiState.nameError,
                accentColor = SheetPurple
            )

            Spacer(Modifier.height(18.dp))

            // Initial Balance — live currency formatting
            CurrencyTextField(
                label = "INITIAL BALANCE (OPTIONAL)",
                rawValue = uiState.balance,
                onValueChange = { viewModel.onEvent(AddAccountEvent.UpdateBalance(it)) },
                error = uiState.balanceError,
                accentColor = SheetPurple
            )

            Spacer(Modifier.height(18.dp))

            // Color
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "COLOR (OPTIONAL)",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.textMuted
                )
                ColorPicker(
                    selectedColor = uiState.selectedColor,
                    onColorSelected = { viewModel.onEvent(AddAccountEvent.UpdateColor(it)) }
                )
            }

            Spacer(Modifier.height(24.dp))

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clickable { onDismiss() },
                    shape = RoundedCornerShape(14.dp),
                    color = AppPalette.card,
                    border = BorderStroke(1.dp, SheetBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B6B8A)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (uiState.accountName.isBlank()) SheetBorder else SheetPurple
                        )
                        .clickable(enabled = uiState.accountName.isNotBlank() && !uiState.isLoading) {
                            viewModel.onEvent(AddAccountEvent.SaveAccount)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Add Account",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ── Text Field with Live Currency Formatting ───────────────────────────────────

/**
 * Mirrors the amount field in AddTransactionDialog exactly:
 * - leadingIcon shows "Rp" prefix (no duplication)
 * - TextField shows formatted digits only (e.g. "2.000.000")
 * - ViewModel stores raw digits only (e.g. "2000000")
 * - Cursor position is maintained relative to digit positions as separators shift
 */
@Composable
private fun CurrencyTextField(
    label: String,
    rawValue: String,
    onValueChange: (String) -> Unit,
    error: String? = null,
    accentColor: Color,
    placeholder: String = "0"
) {
    // Display value: formatted digits only (e.g. "2.000.000"), "0" when empty.
    // "Rp" prefix comes ONLY from leadingIcon, never duplicated in text.
    val displayValue = if (rawValue.isBlank()) "0" else CurrencyUtils.formatInputThousands(rawValue)

    var fieldValue by remember(rawValue) {
        mutableStateOf(TextFieldValue(text = displayValue, selection = TextRange(displayValue.length)))
    }

    // Sync external state → field when raw digits differ
    LaunchedEffect(rawValue) {
        val newDisplay = if (rawValue.isBlank()) "0" else CurrencyUtils.formatInputThousands(rawValue)
        if (fieldValue.text != newDisplay) {
            fieldValue = TextFieldValue(text = newDisplay, selection = TextRange(newDisplay.length))
        }
    }

    val borderColor by animateColorAsState(
        targetValue = if (error != null) Color(0xFFEF4444) else SheetBorder,
        animationSpec = tween(180),
        label = "currency_border"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.textMuted
        )

        OutlinedTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                // Strip all non-digits from the raw input
                val rawDigits = newValue.text.filter { it.isDigit() }
                onValueChange(rawDigits)

                // Format only the digits — "Rp" comes from leadingIcon only
                val newDisplay = if (rawDigits.isEmpty()) "0" else CurrencyUtils.formatInputThousands(rawDigits)

                // Map cursor from old display to new display by counting digit positions.
                // The text field shows ONLY the formatted number (no "Rp" in text),
                // so cursor is always relative to the digit string.
                val oldText = fieldValue.text
                val oldCursor = fieldValue.selection.start.coerceAtMost(oldText.length)

                // Count how many digits are to the LEFT of the cursor in the old display
                val digitsBeforeCursor = oldText.take(oldCursor).count { it.isDigit() }

                // Find the new cursor position in the new display
                var cursorTarget = 0
                var digitCount = 0
                for (char in newDisplay) {
                    if (digitCount >= digitsBeforeCursor) break
                    cursorTarget++
                    if (char.isDigit()) digitCount++
                }

                fieldValue = TextFieldValue(
                    text = newDisplay,
                    selection = TextRange(cursorTarget.coerceIn(0, newDisplay.length))
                )
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    placeholder,
                    color = Color(0xFFBDBDBD),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Text(
                    "Rp",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(start = 4.dp)
                )
            },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it, color = Color(0xFFEF4444)) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = borderColor,
                focusedContainerColor = AppPalette.card,
                unfocusedContainerColor = AppPalette.card,
                errorBorderColor = Color(0xFFEF4444),
                focusedTextColor = accentColor,
                unfocusedTextColor = accentColor
            ),
            textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        )
    }
}

// ── Generic Form Field ─────────────────────────────────────────────────────────

@Composable
private fun AccountFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accentColor: Color,
    error: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppPalette.textMuted
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Color(0xFFBDBDBD)) },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it, color = Color(0xFFEF4444)) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = SheetBorder,
                focusedContainerColor = AppPalette.card,
                unfocusedContainerColor = AppPalette.card,
                errorBorderColor = Color(0xFFEF4444)
            )
        )
    }
}

// ── Account Type Chip ──────────────────────────────────────────────────────────

@Composable
private fun AccountTypeChip(
    type: AccountType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (type) {
        AccountType.CASH -> Icons.Filled.Savings
        AccountType.BANK_ACCOUNT -> Icons.Filled.AccountBalance
        AccountType.E_WALLET -> Icons.Filled.Wallet
        AccountType.CREDIT_CARD -> Icons.Filled.CreditCard
    }

    val backgroundColor = if (isSelected) SheetPurple.copy(alpha = 0.10f) else AppPalette.card
    val borderColor = if (isSelected) SheetPurple else SheetBorder
    val iconColor = if (isSelected) SheetPurple else AppPalette.textMuted
    val textColor = if (isSelected) SheetPurple else AppPalette.textMuted

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = type.displayName,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when (type) {
                AccountType.CASH -> "Cash"
                AccountType.BANK_ACCOUNT -> "Bank"
                AccountType.E_WALLET -> "E-Wallet"
                AccountType.CREDIT_CARD -> "Card"
            },
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ── Color Picker ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        accountColors.forEach { color ->
            val isSelected = selectedColor == color
            val parsedColor = try {
                Color(android.graphics.Color.parseColor(color))
            } catch (e: Exception) {
                SheetPurple
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(parsedColor)
                    .then(
                        if (isSelected) {
                            Modifier.border(3.dp, Color.White, CircleShape)
                                .border(2.dp, parsedColor, CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}