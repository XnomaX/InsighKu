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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlin.math.roundToInt

private val PremiumPurple = Color(0xFF7C4DFF)
private val BorderColor = Color(0xFFECE7F6)
private val BgColor = Color(0xFFFAF9FE)

@Composable
fun EditCategoryDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    category: Category?,
    onCategoryEdited: (Category) -> Unit,
    onCategoryDeleted: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var name by remember { mutableStateOf(category?.name ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var nameFocused by remember { mutableStateOf(false) }
    var budgetLimitText by remember { mutableStateOf(category?.budgetLimit?.toLong()?.toString() ?: "") }
    var alertThreshold by remember { mutableFloatStateOf(category?.alertThreshold?.toFloat() ?: 80f) }
    var selectedIconName by remember { mutableStateOf(category?.icon ?: "Food & Drinks") }
    var selectedPeriod by remember { mutableStateOf(category?.recurringPeriod) }

    val selectedIcon = defaultCategoryIcons.find { it.name == selectedIconName } ?: defaultCategoryIcons.first()

    LaunchedEffect(category) {
        if (category != null) {
            name = category.name
            budgetLimitText = category.budgetLimit?.toLong()?.toString() ?: ""
            alertThreshold = category.alertThreshold.toFloat()
            selectedIconName = category.icon ?: "Food & Drinks"
            selectedPeriod = category.recurringPeriod
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
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200))
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
                    .heightIn(max = 680.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(28.dp), clip = false)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        })
                    },
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                    ) {
                        Column(modifier = Modifier.align(Alignment.CenterStart)) {
                            Text(
                                text = "Edit Category",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A2E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Update your budget category",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                        IconButton(
                            onClick = { handleBack() },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(BorderColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFF6B6B8A),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Divider
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))

                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .background(BgColor)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Category name
                        val nameBorderColor by animateColorAsState(
                            targetValue = when {
                                nameError != null -> MaterialTheme.colorScheme.error
                                nameFocused -> PremiumPurple
                                else -> BorderColor
                            },
                            animationSpec = tween(180),
                            label = "nameBorder"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "CATEGORY NAME",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF9E9E9E)
                            )
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it; nameError = null },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { nameFocused = it.isFocused },
                                singleLine = true,
                                isError = nameError != null,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PremiumPurple,
                                    unfocusedBorderColor = BorderColor,
                                    errorBorderColor = MaterialTheme.colorScheme.error,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                placeholder = {
                                    Text("e.g. Food & Drinks", color = Color(0xFFBDBDBD))
                                }
                            )
                            if (nameError != null) {
                                Text(
                                    text = nameError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Icon & Color
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "ICON & COLOR",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF9E9E9E)
                            )
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(56.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(200.dp)
                            ) {
                                items(defaultCategoryIcons) { iconData ->
                                    PremiumIconOption(
                                        iconData = iconData,
                                        isSelected = selectedIconName == iconData.name,
                                        onClick = { selectedIconName = iconData.name }
                                    )
                                }
                            }
                        }

                        // Budget limit + alert
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "BUDGET LIMIT & ALERT",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF9E9E9E)
                            )
                            BudgetLimitInput(
                                budgetLimitText = budgetLimitText,
                                onBudgetLimitTextChange = { budgetLimitText = it },
                                alertThreshold = alertThreshold,
                                onAlertThresholdChange = { alertThreshold = it }
                            )
                        }

                        // Recurring period
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "BUDGET RESET",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF9E9E9E)
                            )
                            RecurringPeriodSelector(
                                selected = selectedPeriod,
                                onSelect = { selectedPeriod = it }
                            )
                        }
                    }

                    // Action buttons
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
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, BorderColor)
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
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(PremiumPurple)
                                    .clickable {
                                        if (validate()) {
                                            val updatedCategory = category.copy(
                                                name = name.trim(),
                                                color = "#" + selectedIcon.color.value.toString(16).substring(2, 8).uppercase(),
                                                icon = selectedIcon.name,
                                                budgetLimit = budgetLimitText.filter { it.isDigit() }.toLongOrNull()?.toDouble(),
                                                alertThreshold = alertThreshold.roundToInt(),
                                                recurringPeriod = selectedPeriod
                                            )
                                            onCategoryEdited(updatedCategory)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Save Changes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Delete
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clickable { onCategoryDeleted(category.id) },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFF5F5),
                            border = BorderStroke(1.dp, Color(0xFFE57373).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFFE57373)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Delete Category",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE57373)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumIconOption(
    iconData: CategoryIconInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) PremiumPurple.copy(alpha = 0.08f) else Color.White)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) PremiumPurple else BorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconData.color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                iconData.icon,
                contentDescription = iconData.name,
                tint = iconData.color,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PremiumSliderSection(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    prefix: String = "",
    suffix: String = ""
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B6B8A)
                )
                Text(
                    text = "$prefix${value.roundToInt().formatCurrency()}$suffix",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = PremiumPurple
                )
            }
            androidx.compose.material3.Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                steps = steps,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = PremiumPurple,
                    activeTrackColor = PremiumPurple,
                    inactiveTrackColor = BorderColor
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$prefix${range.start.roundToInt().formatCurrency()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFBDBDBD)
                )
                Text(
                    text = "$prefix${range.endInclusive.roundToInt().formatCurrency()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFBDBDBD)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditCategoryDialogPreview() {
    MaterialTheme {
        EditCategoryDialog(
            isOpen = true,
            onDismiss = {},
            category = Category(
                id = "1",
                name = "Food",
                color = "#F59E0B",
                budgetLimit = 1000.0,
                icon = "Food & Drinks",
                alertThreshold = 90
            ),
            onCategoryEdited = {},
            onCategoryDeleted = {}
        )
    }
}
