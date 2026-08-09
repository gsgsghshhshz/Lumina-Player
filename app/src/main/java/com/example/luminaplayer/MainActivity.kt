package com.example.luminaplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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

class MainActivity : ComponentActivity() {
    private var videoUri by mutableStateOf<Uri?>(null)
    private var subtitleUri by mutableStateOf<Uri?>(null)

    private val videoLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            videoUri = it
        }
    }
    private val subtitleLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            subtitleUri = it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Handle direct video intent
        val intentUri: Uri? = when (intent?.action) { Intent.ACTION_VIEW -> intent.data; else -> null }
        if (intentUri != null) { videoUri = intentUri }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                if (videoUri != null) {
                    PlayerScreen(videoUri = videoUri!!, subtitleUri = subtitleUri, onBack = { videoUri = null; subtitleUri = null })
                } else {
                    HomeScreen(
                        onPickVideo = { videoLauncher.launch(arrayOf("video/*")) },
                        onPickSubtitle = { subtitleLauncher.launch(arrayOf("*/*")) },
                        subtitleName = subtitleUri?.lastPathSegment
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(onPickVideo: () -> Unit, onPickSubtitle: () -> Unit, subtitleName: String?) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D0D)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFFE94560))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Lumina Player", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Advanced Video Player", color = Color.Gray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onPickVideo, modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE94560)), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Select Video")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onPickSubtitle, modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDAA520)), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.List, contentDescription = null); Spacer(modifier = Modifier.width(8.dp))
                Text(subtitleName?.take(30) ?: "Select Subtitle")
            }
        }
    }
}
