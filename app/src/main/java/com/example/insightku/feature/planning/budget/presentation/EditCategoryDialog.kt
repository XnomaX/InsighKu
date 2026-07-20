package com.example.insightku.feature.planning.budget.presentation

import com.example.insightku.core.ui.components.dialogs.IconOption
import com.example.insightku.core.ui.components.dialogs.BudgetLimitInput
import com.example.insightku.core.ui.components.dialogs.RecurringPeriodSelector
import com.example.insightku.core.utils.toAmountOrNull
import com.example.insightku.core.ui.components.dialogs.CategoryIconResolver
import com.example.insightku.core.ui.components.dialogs.CategoryIconInfo
import com.example.insightku.core.ui.components.dialogs.expenseCategoryIcons
import com.example.insightku.core.ui.components.dialogs.incomeCategoryIcons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.CategoryType
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.ui.theme.AppPalette
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategoryDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    category: Category?,
    onCategoryEdited: (Category) -> Unit,
    onCategoryDeleted: (String) -> Unit
) {
    if (!isOpen || category == null) return

    val focusManager       = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState         = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val categoryType = category.type
    val accentColor  = if (categoryType == CategoryType.EXPENSE) AppPalette.accent else AppPalette.success
    val iconSet      = if (categoryType == CategoryType.EXPENSE) expenseCategoryIcons else incomeCategoryIcons

    var name             by remember(category) { mutableStateOf(category.name) }
    var nameError        by remember { mutableStateOf<String?>(null) }
    var nameFocused      by remember { mutableStateOf(false) }
    var budgetLimitText  by remember(category) { mutableStateOf(category.budgetLimit?.toLong()?.toString() ?: "") }
    var alertThreshold   by remember(category) { mutableFloatStateOf(category.alertThreshold.toFloat()) }
    var selectedIconName by remember(category) { mutableStateOf(category.icon ?: iconSet.first().name) }
    var selectedPeriod   by remember(category) { mutableStateOf(category.recurringPeriod) }

    val selectedIcon = iconSet.find { it.name == selectedIconName }
        ?: iconSet.firstOrNull() ?: expenseCategoryIcons.first()
    val context = androidx.compose.ui.platform.LocalContext.current

    fun validate(): Boolean {
        return if (name.trim().length < 2) {
            nameError = context.getString(R.string.add_category_name_error)
            false
        } else {
            nameError = null
            true
        }
    }

    fun handleDismiss() {
        focusManager.clearFocus()
        keyboardController?.hide()
        onDismiss()
    }

    com.example.insightku.core.ui.components.bottomsheet.SafeBottomSheet(
        onDismissRequest = { handleDismiss() },
        containerColor   = AppPalette.card,
        contentWindowInsets = WindowInsets(0, 8, 0, 8),
        dragHandle       = {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(AppPalette.cardBorder)
                )
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 8.dp)
            ) {
                Surface(shape = RoundedCornerShape(50), color = accentColor.copy(alpha = 0.10f)) {
                    Text(
                        text       = stringResource(if (categoryType == CategoryType.EXPENSE) R.string.add_category_expense_chip else R.string.add_category_income_chip),
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = accentColor
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text       = stringResource(R.string.edit_category),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = AppPalette.textPrimary
                )
                Text(
                    text  = stringResource(R.string.edit_category_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppPalette.textMuted
                )
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppPalette.cardBorder))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .background(AppPalette.background)
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.label_category_name), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                    OutlinedTextField(
                        value         = name,
                        onValueChange = { name = it; nameError = null },
                        modifier      = Modifier.fillMaxWidth().onFocusChanged { nameFocused = it.isFocused },
                        singleLine    = true,
                        isError       = nameError != null,
                        shape         = RoundedCornerShape(14.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = accentColor,
                            unfocusedBorderColor    = AppPalette.cardBorder,
                            errorBorderColor        = MaterialTheme.colorScheme.error,
                            focusedContainerColor   = AppPalette.card,
                            unfocusedContainerColor = AppPalette.card
                        ),
                        placeholder = { Text(stringResource(R.string.hint_category_name), color = AppPalette.placeholder) }
                    )
                    if (nameError != null) {
                        Text(nameError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ICON", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                    LazyVerticalGrid(
                        columns               = GridCells.Adaptive(56.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement   = Arrangement.spacedBy(8.dp),
                        modifier              = Modifier.height(220.dp)
                    ) {
                        items(iconSet) { iconData ->
                            IconOption(
                                iconData    = iconData,
                                isSelected  = selectedIconName == iconData.name,
                                accentColor = accentColor,
                                onClick     = { selectedIconName = iconData.name }
                            )
                        }
                    }
                }

                if (categoryType == CategoryType.EXPENSE) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.label_budget_limit_alert), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                        BudgetLimitInput(
                            budgetLimitText         = budgetLimitText,
                            onBudgetLimitTextChange = { budgetLimitText = it },
                            alertThreshold          = alertThreshold,
                            onAlertThresholdChange  = { alertThreshold = it }
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.label_budget_reset), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted)
                        RecurringPeriodSelector(selected = selectedPeriod, onSelect = { selectedPeriod = it })
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).height(50.dp).clickable { handleDismiss() },
                        shape    = RoundedCornerShape(14.dp),
                        color    = AppPalette.card,
                        border   = BorderStroke(1.dp, AppPalette.cardBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.cancel), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textDialogMuted)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accentColor)
                            .clickable {
                                if (validate()) {
                                    val colorHex = "#" + selectedIcon.color.value.toString(16).padStart(8, '0').substring(2, 8).uppercase()
                                    onCategoryEdited(
                                        category.copy(
                                            name            = name.trim(),
                                            color           = colorHex,
                                            icon            = selectedIcon.name,
                                            budgetLimit     = if (categoryType == CategoryType.EXPENSE)
                                                budgetLimitText.toAmountOrNull() else null,
                                            alertThreshold  = alertThreshold.roundToInt(),
                                            recurringPeriod = if (categoryType == CategoryType.EXPENSE) selectedPeriod else null,
                                            categoryType    = categoryType.name
                                        )
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.transaction_save_changes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                if (!category.isSystemCategory) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clickable { onCategoryDeleted(category.id) },
                        shape  = RoundedCornerShape(14.dp),
                        color  = AppPalette.deleteBg,
                        border = BorderStroke(1.dp, AppPalette.deleteRed.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp), tint = AppPalette.deleteRed)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.delete), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.deleteRed)
                        }
                    }
                }
            }
        }
    }
}
