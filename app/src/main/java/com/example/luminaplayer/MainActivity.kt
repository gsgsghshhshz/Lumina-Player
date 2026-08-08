package com.example.luminaplayer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : ComponentActivity() {
    private var videoUri by mutableStateOf<Uri?>(null)
    private val launcher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            videoUri = it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                VideoPlayerScreen(
                    videoUri = videoUri,
                    onPickVideo = {
                        launcher.launch(arrayOf("video/*"))
                    }
                )
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(videoUri: Uri?, onPickVideo: () -> Unit) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(videoUri) {
        if (videoUri != null) {
            val player = ExoPlayer.Builder(context).build()
            player.setMediaItem(MediaItem.fromUri(videoUri))
            player.prepare()
            player.playWhenReady = true
            exoPlayer = player
            onDispose { player.release() }
        }
        onDispose { exoPlayer?.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (videoUri != null && exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🎬 Lumina Player",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ویدیو پلیر حرفه‌ای",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onPickVideo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Text("انتخاب ویدیو", fontSize = 16.sp)
                }
            }
        }
    }
}
