package com.example.luminaplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var fontSize by remember { mutableFloatStateOf(24f) }
    var outlineWidth by remember { mutableFloatStateOf(2f) }
    var verticalPos by remember { mutableFloatStateOf(0.9f) }
    var shadowEnabled by remember { mutableStateOf(true) }
    var bgEnabled by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subtitle Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        },
        containerColor = Color(0xFF0F0F23)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sample subtitle text\nSecond line",
                        color = Color(0xFFDAA520),
                        fontSize = fontSize.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1A1A2E),
                contentColor = Color.White
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Font") }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("Outline") }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) { Text("Position") }
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) { Text("Background") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    Text("Font Size: ${fontSize.toInt()}", color = Color.Gray)
                    Slider(
                        value = fontSize,
                        valueRange = 12f..48f,
                        onValueChange = { fontSize = it },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFDAA520),
                            activeTrackColor = Color(0xFFDAA520)
                        )
                    )
                }
                1 -> {
                    Text("Outline Width: ${outlineWidth.toInt()}", color = Color.Gray)
                    Slider(
                        value = outlineWidth,
                        valueRange = 0f..5f,
                        onValueChange = { outlineWidth = it },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFE94560),
                            activeTrackColor = Color(0xFFE94560)
                        )
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Shadow", color = Color.White, modifier = Modifier.weight(1f))
                        Switch(checked = shadowEnabled, onCheckedChange = { shadowEnabled = it })
                    }
                }
                2 -> {
                    Text("Vertical Position: ${(verticalPos * 100).toInt()}%", color = Color.Gray)
                    Slider(
                        value = verticalPos,
                        valueRange = 0.1f..0.95f,
                        onValueChange = { verticalPos = it },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00BFFF),
                            activeTrackColor = Color(0xFF00BFFF)
                        )
                    )
                }
                3 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Subtitle Background", color = Color.White, modifier = Modifier.weight(1f))
                        Switch(checked = bgEnabled, onCheckedChange = { bgEnabled = it })
                    }
                }
            }
        }
    }
}
