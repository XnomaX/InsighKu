package com.example.insightku.ui.components.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.insightku.ui.components.analytics.model.AnalyticsUtils
import com.example.insightku.ui.components.analytics.model.CategoryData
import kotlin.math.atan2

@Composable
fun InteractiveDonutChart(
    title: String,
    titleIcon: String,
    subtitle: String,
    categories: List<CategoryData>,
    selectedCategory: String?,
    onCategoryClick: (String?) -> Unit,
    centerColor: Color,
    monthName: String,
    modifier: Modifier = Modifier
) {
    val totalValue = remember(categories) { categories.sumOf { it.value } }

    val categoriesWithPercentage = remember(categories, totalValue) {
        categories.map { category ->
            val percentage = if (totalValue > 0) (category.value / totalValue) * 100 else 0.0
            category.copy(percentage = percentage)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Text(
                text = "$titleIcon $title - $monthName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            val subtitleText = remember(selectedCategory, categoriesWithPercentage, subtitle, totalValue) {
                selectedCategory?.let { selected ->
                    val selectedCat = categoriesWithPercentage.find { it.name == selected }
                    selectedCat?.let { "${it.name} - ${AnalyticsUtils.formatCurrency(it.value)}" }
                        ?: "$subtitle: ${AnalyticsUtils.formatCurrency(totalValue)}"
                } ?: "$subtitle: ${AnalyticsUtils.formatCurrency(totalValue)}"
            }
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Donut Chart dan Teks Tengah
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                DonutChartCanvas(
                    categories = categoriesWithPercentage,
                    selectedCategory = selectedCategory,
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.size(180.dp)
                )

                // Teks di Tengah Donut
                CenterText(
                    selectedCategory = selectedCategory,
                    categories = categories,
                    totalValue = totalValue,
                    centerColor = centerColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Daftar Kategori dalam Grid
            CategoryGrid(
                categories = categoriesWithPercentage,
                selectedCategory = selectedCategory,
                onCategoryClick = onCategoryClick
            )
        }
    }
}

@Composable
private fun DonutChartCanvas(
    categories: List<CategoryData>,
    selectedCategory: String?,
    onCategoryClick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Menggunakan Float untuk semua kalkulasi terkait Canvas
    val totalValue = remember(categories) { categories.sumOf { it.value }.toFloat() }

    val animatedAlphas = remember { mutableStateMapOf<String, Animatable<Float, *>>() }
    val animatedScales = remember { mutableStateMapOf<String, Animatable<Float, *>>() }

    categories.forEach { category ->
        animatedAlphas.getOrPut(category.name) { Animatable(1f) }
        animatedScales.getOrPut(category.name) { Animatable(1f) }
    }

    LaunchedEffect(selectedCategory, categories) {
        val animationSpec = tween<Float>(durationMillis = 300)
        animatedAlphas.forEach { (name, animatable) ->
            val targetAlpha = if (selectedCategory == null || selectedCategory == name) 1f else 0.5f
            animatable.animateTo(targetAlpha, animationSpec)
        }
        animatedScales.forEach { (name, animatable) ->
            val targetScale = if (selectedCategory == name) 1.15f else 1f
            animatable.animateTo(targetScale, animationSpec)
        }
    }

    Canvas(
        modifier = modifier.pointerInput(categories) {
            detectTapGestures { tapOffset ->
                if (categories.isEmpty() || totalValue == 0f) return@detectTapGestures

                val canvasCenter = Offset(size.width / 2f, size.height / 2f)

                // Melakukan kalkulasi sudut dengan Float secara eksplisit
                val touchAngle = (atan2(
                    (tapOffset.y - canvasCenter.y).toDouble(),
                    (tapOffset.x - canvasCenter.x).toDouble()
                ).toFloat() * (180f / Math.PI.toFloat()) + 450f) % 360f

                var startAngle = 0f
                val clickedCategory = categories.firstOrNull { category ->
                    val sweepAngle = (category.value.toFloat() / totalValue) * 360f
                    val endAngle = startAngle + sweepAngle
                    val found = touchAngle in startAngle..endAngle
                    startAngle = endAngle
                    found
                }?.name

                onCategoryClick(if (clickedCategory == selectedCategory) null else clickedCategory)
            }
        }
    ) {
        val strokeWidth = 28.dp.toPx()
        var currentStartAngle = -90f // Memulai dari atas (Float)

        categories.forEach { category ->
            // Memastikan sweepAngle adalah Float
            val sweepAngle = (category.value.toFloat() / totalValue) * 360f
            val alpha = animatedAlphas[category.name]?.value ?: 1f
            val scale = animatedScales[category.name]?.value ?: 1f

            if (sweepAngle > 0f) {
                drawArc(
                    color = category.color.copy(alpha = alpha),
                    startAngle = currentStartAngle,
                    sweepAngle = sweepAngle - 1f, // Mengurangi 1f (Float) untuk celah
                    useCenter = false,
                    style = Stroke(width = strokeWidth * scale, cap = StrokeCap.Butt)
                )
            }
            currentStartAngle += sweepAngle
        }
    }
}

@Composable
private fun CenterText(
    selectedCategory: String?,
    categories: List<CategoryData>,
    totalValue: Double,
    centerColor: Color
) {
    val (centerText, labelText) = remember(selectedCategory, categories, totalValue) {
        if (selectedCategory != null) {
            val selectedCat = categories.find { it.name == selectedCategory }
            Pair(
                AnalyticsUtils.formatCurrency(selectedCat?.value ?: 0.0),
                selectedCat?.name ?: "Total"
            )
        } else {
            Pair(AnalyticsUtils.formatCurrency(totalValue), "Total")
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = centerText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = centerColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = labelText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CategoryGrid(
    categories: List<CategoryData>,
    selectedCategory: String?,
    onCategoryClick: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        categories.chunked(2).forEach { rowCategories ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowCategories.forEach { category ->
                    CategoryItem(
                        category = category,
                        isSelected = selectedCategory == category.name,
                        onClick = {
                            onCategoryClick(if (selectedCategory == category.name) null else category.name)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowCategories.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: CategoryData,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (isSelected) BorderStroke(1.dp, borderColor) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(category.color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.name,
                    tint = category.color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${category.percentage.format(1)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = AnalyticsUtils.formatCurrencyShort(category.value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun Double.format(digits: Int): String {
    return "%.${digits}f".format(this)
}
