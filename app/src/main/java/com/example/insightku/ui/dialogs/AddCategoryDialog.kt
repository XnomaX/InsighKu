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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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

private val AddDialogPurple = Color(0xFF7C4DFF)
private val AddDialogBorder = Color(0xFFECE7F6)
private val AddDialogBg = Color(0xFFFAF9FE)

@Composable
fun AddCategoryDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onCategoryAdded: (Category) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var nameFocused by remember { mutableStateOf(false) }
    var budgetLimitText by remember { mutableStateOf("") }
    var alertThreshold by remember { mutableStateOf(80f) }
    var selectedIconName by remember { mutableStateOf("Food & Drinks") }
    var selectedPeriod by remember { mutableStateOf<String?>(null) }

    val selectedIcon = defaultCategoryIcons.find { it.name == selectedIconName } ?: defaultCategoryIcons.first()

    LaunchedEffect(isOpen) {
        if (!isOpen) {
            name = ""
            nameError = null
            budgetLimitText = ""
            alertThreshold = 80f
            selectedIconName = "Food & Drinks"
            selectedPeriod = null
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
        visible = isOpen,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200))
    ) {
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
                border = BorderStroke(1.dp, AddDialogBorder)
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
                                text = "Add Category",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A2E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Set a new budget category",
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
                                    .background(AddDialogBorder),
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
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AddDialogBorder))

                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .background(AddDialogBg)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Category name
                        val nameBorderColor by animateColorAsState(
                            targetValue = when {
                                nameError != null -> MaterialTheme.colorScheme.error
                                nameFocused -> AddDialogPurple
                                else -> AddDialogBorder
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
                                    focusedBorderColor = AddDialogPurple,
                                    unfocusedBorderColor = AddDialogBorder,
                                    errorBorderColor = MaterialTheme.colorScheme.error,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                placeholder = {
                                    Text("e.g. Groceries, Bills", color = Color(0xFFBDBDBD))
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
                                    AddDialogIconOption(
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

                        // Live preview
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, AddDialogBorder)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(selectedIcon.color.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        selectedIcon.icon,
                                        contentDescription = null,
                                        tint = selectedIcon.color,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name.ifEmpty { "Category Name" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (name.isEmpty()) Color(0xFFBDBDBD) else Color(0xFF1A1A2E)
                                    )
                                    Text(
                                        text = if (budgetLimitText.isNotBlank())
                                            "Rp ${budgetLimitText.filter { it.isDigit() }.toLongOrNull()?.let {
                                                java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(it)
                                            } ?: "0"} / month"
                                        else "No limit set",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF9E9E9E)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = AddDialogPurple.copy(alpha = 0.10f)
                                ) {
                                    Text(
                                        text = "Preview",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AddDialogPurple,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Action buttons
                    Column(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clickable(onClick = { handleBack() }),
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, AddDialogBorder)
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
                                    .background(AddDialogPurple)
                                    .clickable {
                                        if (validate()) {
                                            val newCategory = Category(
                                                name = name.trim(),
                                                color = "#" + selectedIcon.color.value.toString(16).substring(2, 8).uppercase(),
                                                icon = selectedIcon.name,
                                                budgetLimit = budgetLimitText.filter { it.isDigit() }.toLongOrNull()?.toDouble(),
                                                alertThreshold = alertThreshold.roundToInt(),
                                                recurringPeriod = selectedPeriod
                                            )
                                            onCategoryAdded(newCategory)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Add Category",
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
        }
    }
}

@Composable
private fun AddDialogIconOption(
    iconData: CategoryIconInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) AddDialogPurple.copy(alpha = 0.08f) else Color.White)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) AddDialogPurple else AddDialogBorder,
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
private fun AddDialogSlider(
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
        border = BorderStroke(1.dp, AddDialogBorder)
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
                    color = AddDialogPurple
                )
            }
            androidx.compose.material3.Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                steps = steps,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = AddDialogPurple,
                    activeTrackColor = AddDialogPurple,
                    inactiveTrackColor = AddDialogBorder
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
fun AddCategoryDialogPreview() {
    MaterialTheme {
        AddCategoryDialog(
            isOpen = true,
            onDismiss = {},
            onCategoryAdded = {}
        )
    }
}
