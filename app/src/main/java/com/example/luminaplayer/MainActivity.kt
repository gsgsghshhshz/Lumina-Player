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
        uri?.let { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION); videoUri = it }
    }
    private val subtitleLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION); subtitleUri = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                if (videoUri != null) {
                    PlayerScreen(videoUri = videoUri!!, subtitleUri = subtitleUri, onBack = { videoUri = null; subtitleUri = null })
                } else {
                    Scaffold(containerColor = Color(0xFF0D0D0D)) { padding ->
                        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(72.dp), tint = Color(0xFFE94560))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Lumina Player", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("Advanced Video Player", color = Color.Gray, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(40.dp))
                            Button(onClick = { videoLauncher.launch(arrayOf("video/*")) }, modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE94560)), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Select Video", fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = { subtitleLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDAA520)), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Select Subtitle", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
