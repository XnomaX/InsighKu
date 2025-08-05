package com.example.insightku.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun FloatingAIButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.CameraAlt,
    contentDescription: String = "AI Receipt Scanner"
) {
    var isPressed by remember { mutableStateOf(false) }
    
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .shadow(
                elevation = if (isPressed) 12.dp else 8.dp,
                shape = CircleShape
            ),
        containerColor = Color.Transparent,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF5A2A82),
                            Color(0xFF7C3AED),
                            Color(0xFF4A90E2)
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(100f, 100f)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // AI Scanner Icon (custom OCR-like icon)
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Custom OCR/Scanner icon using paths/primitives
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) { 
                    val strokeWidth = 2.dp.toPx()
                    val size = 24.dp.toPx()
                    val cornerLength = size * 0.25f
                    
                    // Top-left corner
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(0f, cornerLength),
                        end = androidx.compose.ui.geometry.Offset(0f, 0f),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(cornerLength, 0f),
                        strokeWidth = strokeWidth
                    )
                    
                    // Top-right corner
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(size - cornerLength, 0f),
                        end = androidx.compose.ui.geometry.Offset(size, 0f),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(size, 0f),
                        end = androidx.compose.ui.geometry.Offset(size, cornerLength),
                        strokeWidth = strokeWidth
                    )
                    
                    // Bottom-left corner
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(0f, size - cornerLength),
                        end = androidx.compose.ui.geometry.Offset(0f, size),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(0f, size),
                        end = androidx.compose.ui.geometry.Offset(cornerLength, size),
                        strokeWidth = strokeWidth
                    )
                    
                    // Bottom-right corner
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(size - cornerLength, size),
                        end = androidx.compose.ui.geometry.Offset(size, size),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(size, size),
                        end = androidx.compose.ui.geometry.Offset(size, size - cornerLength),
                        strokeWidth = strokeWidth
                    )
                    
                    // Center scanning lines
                    val centerY = size / 2
                    val lineSpacing = size * 0.15f
                    
                    for (i in -1..1) {
                        val y = centerY + (i * lineSpacing)
                        drawLine(
                            color = Color.White,
                            start = androidx.compose.ui.geometry.Offset(size * 0.25f, y),
                            end = androidx.compose.ui.geometry.Offset(size * 0.75f, y),
                            strokeWidth = strokeWidth * 0.8f
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Canvas(
    modifier: Modifier,
    onDraw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier,
        onDraw = onDraw
    )
}
