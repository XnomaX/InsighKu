package com.example.insightku.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insightku.data.model.Category
import com.example.insightku.data.model.CategoryType
import kotlin.math.roundToInt

private val EditDialogBorder  = Color(0xFFECE7F6)
private val EditDialogBg      = Color(0xFFFAF9FE)

@Composable
fun EditCategoryDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    category: Category?,
    onCategoryEdited: (Category) -> Unit,
    onCategoryDeleted: (String) -> Unit
) {
    val focusManager       = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Type is fixed — cannot be changed after creation
    val categoryType = category?.type ?: CategoryType.EXPENSE
    val accentColor  = if (categoryType == CategoryType.EXPENSE) Color(0xFF7C4DFF) else Color(0xFF10B981)
    val iconSet      = if (categoryType == CategoryType.EXPENSE) expenseCategoryIcons else incomeCategoryIcons

    var name             by remember { mutableStateOf(category?.name ?: "") }
    var nameError        by remember { mutableStateOf<String?>(null) }
    var nameFocused      by remember { mutableStateOf(false) }
    var budgetLimitText  by remember { mutableStateOf(category?.budgetLimit?.toLong()?.toString() ?: "") }
    var alertThreshold   by remember { mutableFloatStateOf(category?.alertThreshold?.toFloat() ?: 80f) }
    var selectedIconName by remember { mutableStateOf(category?.icon ?: iconSet.first().name) }
    var selectedPeriod   by remember { mutableStateOf(category?.recurringPeriod) }

    val selectedIcon = iconSet.find { it.name == selectedIconName }
        ?: CategoryIconResolver.resolve(selectedIconName).let { resolved ->
            // If the stored icon name isn't in the current set, fall back to first
            iconSet.firstOrNull { it.name == resolved.name } ?: iconSet.first()
        }

    LaunchedEffect(category) {
        if (category != null) {
            name             = category.name
            budgetLimitText  = category.budgetLimit?.toLong()?.toString() ?: ""
            alertThreshold   = category.alertThreshold.toFloat()
            selectedIconName = category.icon ?: iconSet.first().name
            selectedPeriod   = category.recurringPeriod
        }
    }

    fun validate(): Boolean {
        return if (name.trim().length < 2) {
            nameError = "Category name must be at least 2 characters"
            false
        } else {
            nameError = null
            true
        }
    }

    fun handleBack() {
        focusManager.clearFocus()
        keyboardController?.hide()
        onDismiss()
    }

    BackHandler(enabled = isOpen) { handleBack() }

    AnimatedVisibility(
        visible = isOpen && category != null,
        enter   = fadeIn(tween(200)),
        exit    = fadeOut(tween(200))
    ) {
        if (category == null) return@AnimatedVisibility

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onDismiss()
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = 700.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(28.dp), clip = false)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        })
                    },
                shape  = RoundedCornerShape(28.dp),
                color  = Color.White,
                border = BorderStroke(1.dp, EditDialogBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // ── Header ────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                    ) {
                        Column(modifier = Modifier.align(Alignment.CenterStart)) {
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = accentColor.copy(alpha = 0.10f)
                            ) {
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
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text  = "Update your budget category",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                        IconButton(
                            onClick  = { handleBack() },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(EditDialogBorder),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint     = Color(0xFF6B6B8A),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Divider
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(EditDialogBorder))

                    // ── Scrollable content ────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .background(EditDialogBg)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Category name
                        val nameBorderColor by animateColorAsState(
                            targetValue   = when {
                                nameError != null -> MaterialTheme.colorScheme.error
                                nameFocused       -> accentColor
                                else              -> EditDialogBorder
                            },
                            animationSpec = tween(180),
                            label         = "nameBorder"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text          = "CATEGORY NAME",
                                style         = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.2.sp,
                                fontWeight    = FontWeight.SemiBold,
                                color         = Color(0xFF9E9E9E)
                            )
                            OutlinedTextField(
                                value         = name,
                                onValueChange = { name = it; nameError = null },
                                modifier      = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { nameFocused = it.isFocused },
                                singleLine    = true,
                                isError       = nameError != null,
                                shape         = RoundedCornerShape(14.dp),
                                colors        = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor      = accentColor,
                                    unfocusedBorderColor    = EditDialogBorder,
                                    errorBorderColor        = MaterialTheme.colorScheme.error,
                                    focusedContainerColor   = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                placeholder = {
                                    Text("e.g. Food & Drinks", color = Color(0xFFBDBDBD))
                                }
                            )
                            if (nameError != null) {
                                Text(
                                    text  = nameError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Icon picker — type-specific set
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text          = "ICON",
                                style         = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.2.sp,
                                fontWeight    = FontWeight.SemiBold,
                                color         = Color(0xFF9E9E9E)
                            )
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
                                Text(
                                    text          = "BUDGET LIMIT & ALERT",
                                    style         = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 1.2.sp,
                                    fontWeight    = FontWeight.SemiBold,
                                    color         = Color(0xFF9E9E9E)
                                )
                                BudgetLimitInput(
                                    budgetLimitText         = budgetLimitText,
                                    onBudgetLimitTextChange = { budgetLimitText = it },
                                    alertThreshold          = alertThreshold,
                                    onAlertThresholdChange  = { alertThreshold = it }
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text          = "BUDGET RESET",
                                    style         = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 1.2.sp,
                                    fontWeight    = FontWeight.SemiBold,
                                    color         = Color(0xFF9E9E9E)
                                )
                                RecurringPeriodSelector(
                                    selected = selectedPeriod,
                                    onSelect = { selectedPeriod = it }
                                )
                            }
                        }
                    }

                    // ── Action buttons ────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clickable { handleBack() },
                                shape  = RoundedCornerShape(14.dp),
                                color  = Color.White,
                                border = BorderStroke(1.dp, EditDialogBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text       = "Cancel",
                                        style      = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color      = Color(0xFF6B6B8A)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(accentColor)
                                    .clickable {
                                        if (validate()) {
                                            val colorHex = "#" + selectedIcon.color.value
                                                .toString(16)
                                                .padStart(8, '0')
                                                .substring(2, 8)
                                                .uppercase()
                                            onCategoryEdited(
                                                category.copy(
                                                    name            = name.trim(),
                                                    color           = colorHex,
                                                    icon            = selectedIcon.name,
                                                    budgetLimit     = if (categoryType == CategoryType.EXPENSE)
                                                        budgetLimitText.filter { it.isDigit() }.toLongOrNull()?.toDouble()
                                                    else null,
                                                    alertThreshold  = alertThreshold.roundToInt(),
                                                    recurringPeriod = if (categoryType == CategoryType.EXPENSE) selectedPeriod else null,
                                                    categoryType    = categoryType.name
                                                )
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text       = "Save Changes",
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White
                                )
                            }
                        }

                        // Delete button — hidden for system/protected categories
                        if (!category.isProtected) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
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
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint     = Color(0xFFE57373)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text       = "Delete Category",
                                        style      = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color      = Color(0xFFE57373)
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

@Preview(showBackground = true)
@Composable
fun EditExpenseCategoryDialogPreview() {
    MaterialTheme {
        EditCategoryDialog(
            isOpen = true,
            onDismiss = {},
            category = Category(
                id           = "1",
                name         = "Food",
                color        = "#F59E0B",
                budgetLimit  = 1000.0,
                icon         = "Food & Drinks",
                alertThreshold = 90,
                categoryType = CategoryType.EXPENSE.name
            ),
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
            category = Category(
                id           = "2",
                name         = "Salary",
                color        = "#10B981",
                icon         = "Salary",
                categoryType = CategoryType.INCOME.name
            ),
            onCategoryEdited  = {},
            onCategoryDeleted = {}
        )
    }
}
