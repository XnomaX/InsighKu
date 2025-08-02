package com.example.insightku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.example.insightku.ui.InsightKuApp
import com.example.insightku.ui.theme.InsightKuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        // Allow status bar to be drawn behind
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            InsightKuMainApp()
        }
    }
}

@Composable
private fun InsightKuMainApp() {
    InsightKuTheme(
        darkTheme = false // Default to light theme for now
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            InsightKuApp()
        }
    }
}
