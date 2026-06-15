package com.example.insightku.feature.budgeting.presentation
import com.example.insightku.core.ui.components.dialogs.IconOption
import com.example.insightku.core.ui.components.dialogs.BudgetLimitInput
import com.example.insightku.core.ui.components.dialogs.RecurringPeriodSelector
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.core.data.model.Category
import com.example.insightku.core.data.model.CategoryType
import kotlin.math.roundToInt

private val EditBorder  = Color(0xFFECE7F6)
private val EditBg      = Color(0xFFFAF9FE)

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
    val accentColor  = if (categoryType == CategoryType.EXPENSE) Color(0xFF7C4DFF) else Color(0xFF10B981)
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

    fun validate(): Boolean {
        return if (name.trim().length < 2) {
            nameError = "Category name must be at least 2 characters"
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

    ModalBottomSheet(
        onDismissRequest = { handleDismiss() },
        sheetState       = sheetState,
        containerColor   = Color.White,
        dragHandle       = {
            Box(
                modifier         = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(EditBorder)
                )
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 8.dp)
            ) {
                Surface(shape = RoundedCornerShape(50), color = accentColor.copy(alpha = 0.10f)) {
                    Text(
                        text       = if (categoryType == CategoryType.EXPENSE) "Expense Category" else "Income Category",
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = accentColor
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text       = "Edit Category",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF1A1A2E)
                )
                Text(
                    text  = "Update your budget category",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
                )
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(EditBorder))

            // Scrollable form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .background(EditBg)
                    .padding(24.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Name
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("CATEGORY NAME", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF9E9E9E))
                    OutlinedTextField(
                        value         = name,
                        onValueChange = { name = it; nameError = null },
                        modifier      = Modifier.fillMaxWidth().onFocusChanged { nameFocused = it.isFocused },
                        singleLine    = true,
                        isError       = nameError != null,
                        shape         = RoundedCornerShape(14.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = accentColor,
                            unfocusedBorderColor    = EditBorder,
                            errorBorderColor        = MaterialTheme.colorScheme.error,
                            focusedContainerColor   = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        placeholder = { Text("e.g. Food & Drinks", color = Color(0xFFBDBDBD)) }
                    )
                    if (nameError != null) {
                        Text(nameError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }

                // Icon picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ICON", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF9E9E9E))
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

                // Budget limit + recurring (EXPENSE only)
                if (categoryType == CategoryType.EXPENSE) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("BUDGET LIMIT & ALERT", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF9E9E9E))
                        BudgetLimitInput(
                            budgetLimitText         = budgetLimitText,
                            onBudgetLimitTextChange = { budgetLimitText = it },
                            alertThreshold          = alertThreshold,
                            onAlertThresholdChange  = { alertThreshold = it }
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("BUDGET RESET", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF9E9E9E))
                        RecurringPeriodSelector(selected = selectedPeriod, onSelect = { selectedPeriod = it })
                    }
                }

                // Save + Cancel
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).height(50.dp).clickable { handleDismiss() },
                        shape    = RoundedCornerShape(14.dp),
                        color    = Color.White,
                        border   = BorderStroke(1.dp, EditBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Cancel", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color(0xFF6B6B8A))
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
                                                budgetLimitText.filter { it.isDigit() }.toLongOrNull()?.toDouble() else null,
                                            alertThreshold  = alertThreshold.roundToInt(),
                                            recurringPeriod = if (categoryType == CategoryType.EXPENSE) selectedPeriod else null,
                                            categoryType    = categoryType.name
                                        )
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Save Changes", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Delete — hidden for protected categories
                if (!category.isProtected) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clickable { onCategoryDeleted(category.id) },
                        shape  = RoundedCornerShape(14.dp),
                        color  = Color(0xFFFFF5F5),
                        border = BorderStroke(1.dp, Color(0xFFE57373).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFE57373))
                            Spacer(Modifier.width(8.dp))
                            Text("Delete Category", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color(0xFFE57373))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditExpenseCategoryDialogPreview() {
    MaterialTheme {
        EditCategoryDialog(
            isOpen = true,
            onDismiss = {},
            category = Category(id = "1", name = "Food", color = "#F59E0B", budgetLimit = 1000.0, icon = "Food & Drinks", alertThreshold = 90, categoryType = CategoryType.EXPENSE.name),
            onCategoryEdited  = {},
            onCategoryDeleted = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EditIncomeCategoryDialogPreview() {
    MaterialTheme {
        EditCategoryDialog(
            isOpen = true,
            onDismiss = {},
            category = Category(id = "2", name = "Salary", color = "#10B981", icon = "Salary", categoryType = CategoryType.INCOME.name),
            onCategoryEdited  = {},
            onCategoryDeleted = {}
        )
    }
}






