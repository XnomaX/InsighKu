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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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

private val DialogBorder  = Color(0xFFECE7F6)
private val DialogBg      = Color(0xFFFAF9FE)
private val ExpenseAccent = Color(0xFF7C4DFF)
private val IncomeAccent  = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onCategoryAdded: (Category) -> Unit,
    initialType: CategoryType = CategoryType.EXPENSE
) {
    if (!isOpen) return

    val focusManager       = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState         = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val accentColor = if (initialType == CategoryType.EXPENSE) ExpenseAccent else IncomeAccent
    val iconSet     = if (initialType == CategoryType.EXPENSE) expenseCategoryIcons else incomeCategoryIcons

    var name             by remember { mutableStateOf("") }
    var nameError        by remember { mutableStateOf<String?>(null) }
    var nameFocused      by remember { mutableStateOf(false) }
    var budgetLimitText  by remember { mutableStateOf("") }
    var alertThreshold   by remember { mutableStateOf(80f) }
    var selectedIconName by remember { mutableStateOf(iconSet.first().name) }
    var selectedPeriod   by remember { mutableStateOf<String?>(null) }

    val selectedIcon = iconSet.find { it.name == selectedIconName } ?: iconSet.first()

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
        name             = ""
        nameError        = null
        budgetLimitText  = ""
        alertThreshold   = 80f
        selectedIconName = iconSet.first().name
        selectedPeriod   = null
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
                        .background(DialogBorder)
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
                        text       = if (initialType == CategoryType.EXPENSE) "Expense Category" else "Income Category",
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = accentColor
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text       = if (initialType == CategoryType.EXPENSE) "Add Expense Category" else "Add Income Source",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF1A1A2E)
                )
                Text(
                    text  = if (initialType == CategoryType.EXPENSE) "Track and limit your spending" else "Track where your money comes from",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
                )
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DialogBorder))

            // Scrollable form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .background(DialogBg)
                    .padding(24.dp)
                    .navigationBarsPadding(),
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
                            unfocusedBorderColor    = DialogBorder,
                            errorBorderColor        = MaterialTheme.colorScheme.error,
                            focusedContainerColor   = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        placeholder = {
                            Text(
                                if (initialType == CategoryType.EXPENSE) "e.g. Groceries, Bills" else "e.g. Salary, Freelance",
                                color = Color(0xFFBDBDBD)
                            )
                        }
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
                if (initialType == CategoryType.EXPENSE) {
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

                // Live preview
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, DialogBorder)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier         = Modifier.size(44.dp).background(selectedIcon.color.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(selectedIcon.icon, contentDescription = null, tint = selectedIcon.color, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = name.ifEmpty { "Category Name" },
                                style      = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color      = if (name.isEmpty()) Color(0xFFBDBDBD) else Color(0xFF1A1A2E)
                            )
                            Text(
                                text  = if (initialType == CategoryType.EXPENSE) {
                                    if (budgetLimitText.isNotBlank()) "Rp ${budgetLimitText.filter { it.isDigit() }.toLongOrNull()?.let {
                                        java.text.NumberFormat.getNumberInstance(java.util.Locale("id","ID")).format(it)
                                    } ?: "0"} / month" else "No limit set"
                                } else "Income tracking",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                        Surface(shape = RoundedCornerShape(50), color = accentColor.copy(alpha = 0.10f)) {
                            Text("Preview", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).height(50.dp).clickable { handleDismiss() },
                        shape    = RoundedCornerShape(14.dp),
                        color    = Color.White,
                        border   = BorderStroke(1.dp, DialogBorder)
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
                                    onCategoryAdded(
                                        Category(
                                            name            = name.trim(),
                                            color           = colorHex,
                                            icon            = selectedIcon.name,
                                            budgetLimit     = if (initialType == CategoryType.EXPENSE)
                                                budgetLimitText.filter { it.isDigit() }.toLongOrNull()?.toDouble() else null,
                                            alertThreshold  = alertThreshold.roundToInt(),
                                            recurringPeriod = if (initialType == CategoryType.EXPENSE) selectedPeriod else null,
                                            categoryType    = initialType.name
                                        )
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(
                                if (initialType == CategoryType.EXPENSE) "Add Category" else "Add Source",
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddExpenseCategoryDialogPreview() {
    MaterialTheme {
        AddCategoryDialog(isOpen = true, onDismiss = {}, onCategoryAdded = {}, initialType = CategoryType.EXPENSE)
    }
}

@Preview(showBackground = true)
@Composable
fun AddIncomeCategoryDialogPreview() {
    MaterialTheme {
        AddCategoryDialog(isOpen = true, onDismiss = {}, onCategoryAdded = {}, initialType = CategoryType.INCOME)
    }
}






