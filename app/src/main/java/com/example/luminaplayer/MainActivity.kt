package com.example.luminaplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private var videoUri by mutableStateOf<Uri?>(null)
    private var subtitleUri by mutableStateOf<Uri?>(null)
    
    private val videoLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            videoUri = it
        }
    }
    
    private val subtitleLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            subtitleUri = it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                if (videoUri != null) {
                    PlayerScreen(
                        videoUri = videoUri!!,
                        subtitleUri = subtitleUri,
                        onBack = { videoUri = null; subtitleUri = null }
                    )
                } else {
                    HomeScreen(
                        onPickVideo = { videoLauncher.launch(arrayOf("video/*")) },
                        onPickSubtitle = { subtitleLauncher.launch(arrayOf("*/*")) },
                        onOpenSettings = {
                            startActivity(Intent(this, SettingsActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPickVideo: () -> Unit,
    onPickSubtitle: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lumina Player") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A2E)),
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = Color(0xFFE94560))
                    }
                }
            )
        },
        containerColor = Color(0xFF0F0F23)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = Color(0xFFE94560)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text("Lumina Player", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Professional Video Player", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onPickVideo,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE94560)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Select Video", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onPickSubtitle,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDAA520)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.List, null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Select Subtitle (SRT/ASS)", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureCard(icon = Icons.Default.List, title = "Subtitles", modifier = Modifier.weight(1f))
                FeatureCard(icon = Icons.Default.Star, title = "Quality", modifier = Modifier.weight(1f))
                FeatureCard(icon = Icons.Default.Favorite, title = "Speed", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun FeatureCard(icon: ImageVector, title: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = Color(0xFFE94560), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, color = Color.White, fontSize = 11.sp)
        }
    }
}
