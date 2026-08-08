package com.example.luminaplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme

class VideoPlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoUri: Uri? = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            else -> null
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                if (videoUri != null) {
                    VideoPlayerScreen(
                        videoUri = videoUri,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}
