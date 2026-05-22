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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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

private val DialogBorder  = Color(0xFFECE7F6)
private val DialogBg      = Color(0xFFFAF9FE)
private val ExpenseAccent = Color(0xFF7C4DFF)
private val IncomeAccent  = Color(0xFF10B981)

@Composable
fun AddCategoryDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onCategoryAdded: (Category) -> Unit,
    // Fixed type — caller decides context. Defaults to EXPENSE for backward compat.
    initialType: CategoryType = CategoryType.EXPENSE
) {
    val focusManager       = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val accentColor = if (initialType == CategoryType.EXPENSE) ExpenseAccent else IncomeAccent
    val iconSet     = if (initialType == CategoryType.EXPENSE) expenseCategoryIcons else incomeCategoryIcons

    var name            by remember { mutableStateOf("") }
    var nameError       by remember { mutableStateOf<String?>(null) }
    var nameFocused     by remember { mutableStateOf(false) }
    var budgetLimitText by remember { mutableStateOf("") }
    var alertThreshold  by remember { mutableStateOf(80f) }
    var selectedIconName by remember { mutableStateOf(iconSet.first().name) }
    var selectedPeriod  by remember { mutableStateOf<String?>(null) }

    val selectedIcon = iconSet.find { it.name == selectedIconName } ?: iconSet.first()

    LaunchedEffect(isOpen) {
        if (!isOpen) {
            name             = ""
            nameError        = null
            budgetLimitText  = ""
            alertThreshold   = 80f
            selectedIconName = iconSet.first().name
            selectedPeriod   = null
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
        enter   = fadeIn(tween(200)),
        exit    = fadeOut(tween(200))
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
                border = BorderStroke(1.dp, DialogBorder)
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
                            // Type badge
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = accentColor.copy(alpha = 0.10f)
                            ) {
                                Text(
                                    text     = if (initialType == CategoryType.EXPENSE) "Expense Category" else "Income Category",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style    = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color    = accentColor
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text       = if (initialType == CategoryType.EXPENSE) "Add Expense Category" else "Add Income Source",
                                style      = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color      = Color(0xFF1A1A2E)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text  = if (initialType == CategoryType.EXPENSE)
                                    "Track and limit your spending"
                                else
                                    "Track where your money comes from",
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
                                    .background(DialogBorder),
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
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DialogBorder))

                    // ── Scrollable content ────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .background(DialogBg)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Category name
                        val nameBorderColor by animateColorAsState(
                            targetValue   = when {
                                nameError != null -> MaterialTheme.colorScheme.error
                                nameFocused       -> accentColor
                                else              -> DialogBorder
                            },
                            animationSpec = tween(180),
                            label         = "nameBorder"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text       = "CATEGORY NAME",
                                style      = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFF9E9E9E)
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
                                    unfocusedBorderColor    = DialogBorder,
                                    errorBorderColor        = MaterialTheme.colorScheme.error,
                                    focusedContainerColor   = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                placeholder = {
                                    Text(
                                        text  = if (initialType == CategoryType.EXPENSE) "e.g. Groceries, Bills" else "e.g. Salary, Freelance",
                                        color = Color(0xFFBDBDBD)
                                    )
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

                        // Icon picker
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text       = "ICON",
                                style      = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFF9E9E9E)
                            )
                            LazyVerticalGrid(
                                columns             = GridCells.Adaptive(56.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier            = Modifier.height(220.dp)
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
                                Text(
                                    text       = "BUDGET LIMIT & ALERT",
                                    style      = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = Color(0xFF9E9E9E)
                                )
                                BudgetLimitInput(
                                    budgetLimitText        = budgetLimitText,
                                    onBudgetLimitTextChange = { budgetLimitText = it },
                                    alertThreshold         = alertThreshold,
                                    onAlertThresholdChange = { alertThreshold = it }
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text       = "BUDGET RESET",
                                    style      = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = Color(0xFF9E9E9E)
                                )
                                RecurringPeriodSelector(
                                    selected  = selectedPeriod,
                                    onSelect  = { selectedPeriod = it }
                                )
                            }
                        }

                        // Live preview
                        Surface(
                            shape  = RoundedCornerShape(16.dp),
                            color  = Color.White,
                            border = BorderStroke(1.dp, DialogBorder)
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
                                        tint     = selectedIcon.color,
                                        modifier = Modifier.size(22.dp)
                                    )
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
                                            if (budgetLimitText.isNotBlank())
                                                "Rp ${budgetLimitText.filter { it.isDigit() }.toLongOrNull()?.let {
                                                    java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(it)
                                                } ?: "0"} / month"
                                            else "No limit set"
                                        } else "Income tracking",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF9E9E9E)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = accentColor.copy(alpha = 0.10f)
                                ) {
                                    Text(
                                        text     = "Preview",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style    = MaterialTheme.typography.labelSmall,
                                        color    = accentColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // ── Action buttons ────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clickable { handleBack() },
                                shape  = RoundedCornerShape(14.dp),
                                color  = Color.White,
                                border = BorderStroke(1.dp, DialogBorder)
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
                                            onCategoryAdded(
                                                Category(
                                                    name           = name.trim(),
                                                    color          = colorHex,
                                                    icon           = selectedIcon.name,
                                                    budgetLimit    = if (initialType == CategoryType.EXPENSE)
                                                        budgetLimitText.filter { it.isDigit() }.toLongOrNull()?.toDouble()
                                                    else null,
                                                    alertThreshold = alertThreshold.roundToInt(),
                                                    recurringPeriod = if (initialType == CategoryType.EXPENSE) selectedPeriod else null,
                                                    categoryType   = initialType.name
                                                )
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint     = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text       = if (initialType == CategoryType.EXPENSE) "Add Category" else "Add Source",
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
    }
}

@Preview(showBackground = true)
@Composable
fun AddExpenseCategoryDialogPreview() {
    MaterialTheme {
        AddCategoryDialog(
            isOpen          = true,
            onDismiss       = {},
            onCategoryAdded = {},
            initialType     = CategoryType.EXPENSE
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddIncomeCategoryDialogPreview() {
    MaterialTheme {
        AddCategoryDialog(
            isOpen          = true,
            onDismiss       = {},
            onCategoryAdded = {},
            initialType     = CategoryType.INCOME
        )
    }
}
