package com.example.insightku.feature.planning.goal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.insightku.R
import com.example.insightku.core.i18n.NumberFormatter
import com.example.insightku.core.ui.theme.AppPalette
import com.example.insightku.core.i18n.DateFormatter
import com.example.insightku.core.ui.theme.LocalAccent
import com.example.insightku.core.ui.components.PremiumDatePicker
import com.example.insightku.core.utils.CurrencyUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

private val GoalPurple: Color @Composable get() = LocalAccent.current
private val GoalBorder: Color @Composable get() = AppPalette.cardBorder
private val GoalBg: Color @Composable get() = AppPalette.background

private data class GoalIcon(val name: String, val icon: ImageVector, val category: String)

private val goalIcons = listOf(
    GoalIcon("Piggy Bank", Icons.Default.Savings, "Savings"), GoalIcon("Wallet", Icons.Default.AccountBalanceWallet, "Savings"), GoalIcon("Cash", Icons.Default.Money, "Savings"), GoalIcon("Coins", Icons.Default.Paid, "Savings"),
    GoalIcon("Motorcycle", Icons.Default.TwoWheeler, "Transportation"), GoalIcon("Car", Icons.Default.DirectionsCar, "Transportation"), GoalIcon("Bus", Icons.Default.DirectionsBus, "Transportation"), GoalIcon("Train", Icons.Default.Train, "Transportation"), GoalIcon("Airplane", Icons.Default.Flight, "Transportation"),
    GoalIcon("House", Icons.Default.Home, "Home"), GoalIcon("Apartment", Icons.Default.Apartment, "Home"), GoalIcon("Furniture", Icons.Default.Chair, "Home"), GoalIcon("Construction", Icons.Default.Construction, "Home"),
    GoalIcon("Laptop", Icons.Default.Laptop, "Technology"), GoalIcon("Phone", Icons.Default.Smartphone, "Technology"), GoalIcon("Tablet", Icons.Default.Tablet, "Technology"), GoalIcon("Camera", Icons.Default.CameraAlt, "Technology"), GoalIcon("Headphones", Icons.Default.Headphones, "Technology"), GoalIcon("Gaming", Icons.Default.SportsEsports, "Technology"),
    GoalIcon("Beach", Icons.Default.BeachAccess, "Travel"), GoalIcon("Mountain", Icons.Default.Terrain, "Travel"), GoalIcon("Suitcase", Icons.Default.Luggage, "Travel"), GoalIcon("Hotel", Icons.Default.Hotel, "Travel"),
    GoalIcon("Graduation", Icons.Default.School, "Education"), GoalIcon("Book", Icons.Default.MenuBook, "Education"), GoalIcon("Course", Icons.Default.OndemandVideo, "Education"),
    GoalIcon("Heart", Icons.Default.Favorite, "Health"), GoalIcon("Hospital", Icons.Default.LocalHospital, "Health"), GoalIcon("Medicine", Icons.Default.MedicalServices, "Health"), GoalIcon("Fitness", Icons.Default.FitnessCenter, "Health"),
    GoalIcon("Shield", Icons.Default.Shield, "Emergency"), GoalIcon("Security", Icons.Default.Security, "Emergency"),
    GoalIcon("Shopping", Icons.Default.ShoppingBag, "Shopping"), GoalIcon("Cart", Icons.Default.ShoppingCart, "Shopping"), GoalIcon("Gift", Icons.Default.CardGiftcard, "Shopping"),
    GoalIcon("Gold", Icons.Default.Diamond, "Investment"), GoalIcon("Stocks", Icons.Default.TrendingUp, "Investment"), GoalIcon("Bitcoin", Icons.Default.CurrencyBitcoin, "Investment"), GoalIcon("Chart", Icons.Default.ShowChart, "Investment"),
    GoalIcon("Coffee", Icons.Default.Coffee, "Lifestyle"), GoalIcon("Restaurant", Icons.Default.Restaurant, "Lifestyle"), GoalIcon("Bicycle", Icons.Default.DirectionsBike, "Lifestyle"), GoalIcon("Sports", Icons.Default.Sports, "Lifestyle"),
    GoalIcon("Star", Icons.Default.Star, "General"), GoalIcon("Rocket", Icons.Default.RocketLaunch, "General"), GoalIcon("Flag", Icons.Default.Flag, "General"), GoalIcon("Target", Icons.Default.GpsFixed, "General"), GoalIcon("Celebration", Icons.Default.Celebration, "General"), GoalIcon("Child", Icons.Default.ChildCare, "General"), GoalIcon("Pet", Icons.Default.Pets, "General"), GoalIcon("Church", Icons.Default.Church, "General"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalDialog(initialDeadline: LocalDate? = null, onDismiss: () -> Unit, onCreateGoal: (name: String, targetAmount: Double, deadline: LocalDate?, icon: String, color: String) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }; var targetAmountText by remember { mutableStateOf("") }; var deadline by remember { mutableStateOf(initialDeadline) }; var selectedIcon by remember { mutableStateOf("Piggy Bank") }; var selectedColor by remember { mutableStateOf(AppPalette.GoalColors.first()) }; var showDatePicker by remember { mutableStateOf(false) };    var nameError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val isValid = name.isNotBlank() && targetAmountText.toDoubleOrNull()?.let { it > 0 } == true
    if (showDatePicker) { val initialMillis = deadline?.atStartOfDay()?.toInstant(ZoneId.systemDefault().rules.getOffset(Instant.now()))?.toEpochMilli() ?: System.currentTimeMillis(); PremiumDatePicker(initialMillis = initialMillis, onDateSelected = { millis -> deadline = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate(); showDatePicker = false }, onDismiss = { showDatePicker = false }) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = AppPalette.card, dragHandle = { Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) { Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(50.dp)).background(GoalBorder)) } }) {
        Column(modifier = Modifier.safeDrawingPadding().fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 8.dp)) { Surface(shape = RoundedCornerShape(50), color = GoalPurple.copy(alpha = 0.10f)) { Text(stringResource(R.string.add_goal_chip), Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = GoalPurple) }; Spacer(Modifier.height(6.dp)); Text(stringResource(R.string.add_goal_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppPalette.textPrimary); Text(stringResource(R.string.add_goal_subtitle), style = MaterialTheme.typography.bodySmall, color = AppPalette.textMuted) }
            Box(Modifier.fillMaxWidth().height(1.dp).background(GoalBorder))
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).background(GoalBg).padding(24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GoalFormField(label = stringResource(R.string.add_goal_name_label), value = name, onValueChange = { name = it; nameError = null }, placeholder = "e.g. Emergency Fund, Vacation", error = nameError, accentColor = GoalPurple)
                GoalFormField(label = stringResource(R.string.add_goal_amount_label), value = CurrencyUtils.formatInputThousands(targetAmountText), onValueChange = { targetAmountText = CurrencyUtils.stripThousands(it) }, placeholder = "e.g. 5.000.000", keyboardType = KeyboardType.Number, prefix = NumberFormatter.getCurrencySymbol(), accentColor = GoalPurple)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(stringResource(R.string.add_goal_date_label), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted); Surface(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, shape = RoundedCornerShape(14.dp), color = AppPalette.card, border = BorderStroke(1.dp, GoalBorder)) { Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {                    Text(deadline?.let { DateFormatter.formatFullDate(it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()) } ?: stringResource(R.string.add_goal_no_deadline), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textPrimary); Icon(Icons.Default.EditCalendar, contentDescription = null, tint = AppPalette.accent, modifier = Modifier.size(18.dp)) } } }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(stringResource(R.string.add_goal_icon_label), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted); GoalIconPicker(selectedIcon = selectedIcon, onIconSelected = { selectedIcon = it }, accentColor = GoalPurple) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(stringResource(R.string.add_goal_color_label), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted); GoalColorPicker(selectedColor = selectedColor, onColorSelected = { selectedColor = it }) }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Surface(modifier = Modifier.weight(1f).height(50.dp).clickable { onDismiss() }, shape = RoundedCornerShape(14.dp), color = AppPalette.card, border = BorderStroke(1.dp, GoalBorder)) { Box(contentAlignment = Alignment.Center) { Text(stringResource(R.string.cancel), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppPalette.textDialogMuted) } }; Box(modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp)).background(if (isValid) GoalPurple else AppPalette.textMuted).clickable(enabled = isValid) { if (name.trim().length < 2) { nameError = context.getString(R.string.add_goal_name_error); return@clickable }; onCreateGoal(name.trim(), targetAmountText.filter { it.isDigit() }.toLongOrNull()?.toDouble() ?: 0.0, deadline, selectedIcon, selectedColor) }, contentAlignment = Alignment.Center) { Text(stringResource(R.string.add_goal_create), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White) } }
            }
        }
    }
}

@Composable private fun GoalIconPicker(selectedIcon: String, onIconSelected: (String) -> Unit, accentColor: Color) { Surface(shape = RoundedCornerShape(16.dp), color = AppPalette.card, border = BorderStroke(1.dp, GoalBorder)) { LazyRow(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { items(goalIcons) { goalIcon -> val isSelected = selectedIcon == goalIcon.name; Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent).then(if (isSelected) Modifier.border(2.dp, accentColor, CircleShape) else Modifier.border(1.dp, GoalBorder, CircleShape)).clickable { onIconSelected(goalIcon.name) }, contentAlignment = Alignment.Center) { Icon(imageVector = goalIcon.icon, contentDescription = goalIcon.name, tint = if (isSelected) accentColor else AppPalette.textMuted, modifier = Modifier.size(24.dp)) } } } } }

@Composable private fun GoalColorPicker(selectedColor: String, onColorSelected: (String) -> Unit) { Surface(shape = RoundedCornerShape(16.dp), color = AppPalette.card, border = BorderStroke(1.dp, GoalBorder)) { LazyRow(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { items(AppPalette.GoalColors) { colorHex -> val color = Color(android.graphics.Color.parseColor(colorHex)); val isSelected = selectedColor == colorHex; Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color).then(if (isSelected) Modifier.border(2.5.dp, AppPalette.textPrimary, CircleShape) else Modifier).clickable { onColorSelected(colorHex) }, contentAlignment = Alignment.Center) { if (isSelected) { Icon(imageVector = Icons.Default.Check, contentDescription = stringResource(R.string.content_selected), tint = Color.White, modifier = Modifier.size(18.dp)) } } } } } }

@Composable private fun GoalFormField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String, accentColor: Color, error: String? = null, keyboardType: KeyboardType = KeyboardType.Text, prefix: String? = null) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(label, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textMuted); OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text(placeholder, color = AppPalette.placeholder) }, prefix = if (prefix != null) { { Text(prefix, fontWeight = FontWeight.Bold, color = accentColor) } } else null, singleLine = true, isError = error != null, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, unfocusedBorderColor = GoalBorder, focusedContainerColor = AppPalette.card, unfocusedContainerColor = AppPalette.card, errorBorderColor = AppPalette.error)); if (error != null) { Text(error, style = MaterialTheme.typography.labelSmall, color = AppPalette.error) } } }
