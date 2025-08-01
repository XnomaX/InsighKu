package com.example.insightku.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.insightku.viewmodel.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onCategoryAdded: (Category) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var budgetAmount by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#F59E0B") }
    var selectedIcon by remember { mutableStateOf("restaurant") }

    val availableColors = listOf(
        "#F59E0B", "#3B82F6", "#8B5CF6", "#EC4899", 
        "#EF4444", "#10B981", "#F97316", "#6366F1",
        "#84CC16", "#06B6D4", "#F59E0B", "#E11D48"
    )

    val availableIcons = listOf(
        "restaurant" to Icons.Default.Restaurant,
        "directions_car" to Icons.Default.DirectionsCar,
        "sports_esports" to Icons.Default.SportsEsports,
        "shopping_bag" to Icons.Default.ShoppingBag,
        "home" to Icons.Default.Home,
        "local_hospital" to Icons.Default.LocalHospital,
        "fitness_center" to Icons.Default.FitnessCenter,
        "school" to Icons.Default.School,
        "work" to Icons.Default.Work,
        "flight" to Icons.Default.Flight,
        "movie" to Icons.Default.Movie,
        "music_note" to Icons.Default.MusicNote
    )

    LaunchedEffect(isOpen) {
        if (isOpen) {
            name = ""
            budgetAmount = ""
            selectedColor = "#F59E0B"
            selectedIcon = "restaurant"
        }
    }

    if (isOpen) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp, 16.dp, 20.dp, 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Category",
                                tint = Color(0xFF5A2A82),
                                modifier = Modifier.size(20.dp)
                            )
                            
                            Text(
                                text = "Add New Category",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    // Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Category Preview
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(selectedColor.removePrefix("#").toLong(16) or 0xFF000000).copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            Color(selectedColor.removePrefix("#").toLong(16) or 0xFF000000).copy(alpha = 0.2f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val iconVector = availableIcons.find { it.first == selectedIcon }?.second ?: Icons.Default.Category
                                    Icon(
                                        iconVector,
                                        contentDescription = name,
                                        tint = Color(selectedColor.removePrefix("#").toLong(16) or 0xFF000000),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = name.ifEmpty { "Category Name" },
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = "Budget: Rp ${budgetAmount.ifEmpty { "0" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        // Category Name
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Category Name",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = { Text("e.g., Food & Drinks") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF5A2A82),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                        
                        // Budget Amount
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Budget Amount",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            
                            OutlinedTextField(
                                value = budgetAmount,
                                onValueChange = { budgetAmount = it },
                                placeholder = { Text("0") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                leadingIcon = {
                                    Text(
                                        text = "Rp",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF5A2A82),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                        
                        // Color Selection
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Category Color",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(6),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(80.dp)
                            ) {
                                items(availableColors) { color ->
                                    ColorOption(
                                        color = color,
                                        isSelected = selectedColor == color,
                                        onClick = { selectedColor = color }
                                    )
                                }
                            }
                        }
                        
                        // Icon Selection
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Category Icon",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(6),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(120.dp)
                            ) {
                                items(availableIcons) { (iconName, iconVector) ->
                                    IconOption(
                                        icon = iconVector,
                                        iconName = iconName,
                                        isSelected = selectedIcon == iconName,
                                        onClick = { selectedIcon = iconName }
                                    )
                                }
                            }
                        }
                    }
                    
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                val amount = budgetAmount.toDoubleOrNull() ?: 0.0
                                val category = Category(
                                    id = System.currentTimeMillis().toString(),
                                    name = name,
                                    budgetAmount = amount,
                                    spentAmount = 0.0,
                                    color = selectedColor,
                                    icon = selectedIcon
                                )
                                onCategoryAdded(category)
                            },
                            enabled = name.isNotBlank() && budgetAmount.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5A2A82),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Category")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorOption(
    color: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                Color(color.removePrefix("#").toLong(16) or 0xFF000000),
                shape = CircleShape
            )
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier
                        .padding(4.dp)
                        .background(
                            Color.White,
                            shape = CircleShape
                        )
                        .padding(4.dp)
                        .background(
                            Color(color.removePrefix("#").toLong(16) or 0xFF000000),
                            shape = CircleShape
                        )
                } else {
                    Modifier
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun IconOption(
    icon: ImageVector,
    iconName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                if (isSelected) Color(0xFF5A2A82).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = iconName,
            tint = if (isSelected) Color(0xFF5A2A82) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}