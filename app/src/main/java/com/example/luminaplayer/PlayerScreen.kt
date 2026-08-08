package com.example.luminaplayer

import android.app.Activity
import android.net.Uri
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

data class SubtitleConfig(
    val fontPath: String = "",
    val fontSize: Float = 22f,
    val fontScale: Float = 100f,
    val fontColor: Color = Color.White,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val backgroundColor: Color = Color.Black,
    val backgroundEnabled: Boolean = false,
    val borderColor: Color = Color.Black,
    val borderEnabled: Boolean = true,
    val borderWidth: Float = 300f,
    val shadowEnabled: Boolean = false,
    val shadowFadeOut: Boolean = true,
    val position: Float = 0.9f,
    val encoding: String = "UTF-8"
)

@Composable
fun PlayerScreen(videoUri: Uri, subtitleUri: Uri?, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var brightness by remember { mutableFloatStateOf(0.5f) }
    var volume by remember { mutableFloatStateOf(0.8f) }
    var isLocked by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSubtitleSettings by remember { mutableStateOf(false) }
    var subtitleConfig by remember { mutableStateOf(SubtitleConfig()) }
    var isPrepared by remember { mutableStateOf(false) }

    // اعمال روشنایی به صفحه
    LaunchedEffect(brightness) {
        activity?.window?.attributes = activity.window.attributes.apply {
            screenBrightness = brightness
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val subtitleConfigs = mutableListOf<SubtitleConfiguration>()
            if (subtitleUri != null) {
                subtitleConfigs.add(
                    SubtitleConfiguration.Builder(subtitleUri)
                        .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                        .setLanguage("fa")
                        .setLabel("فارسی")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            }
            val mediaItem = MediaItem.Builder()
                .setUri(videoUri)
                .setSubtitleConfigurations(subtitleConfigs)
                .build()
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            isPrepared = true
                            duration = this@apply.duration.coerceAtLeast(1)
                        }
                    }
                }
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (isPrepared) {
                position = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(1)
                isPlaying = exoPlayer.isPlaying
            }
            delay(200)
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls },
                        onDoubleTap = { offset ->
                            when {
                                offset.x < size.width / 3 -> exoPlayer.seekTo(maxOf(0, exoPlayer.currentPosition - 10000))
                                offset.x > size.width * 2 / 3 -> exoPlayer.seekTo(minOf(exoPlayer.duration, exoPlayer.currentPosition + 10000))
                                else -> exoPlayer.playWhenReady = !isPlaying
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val x = change.position.x
                        when {
                            x < size.width / 3f -> {
                                brightness = (brightness - dragAmount.y / size.height).coerceIn(0.05f, 1f)
                            }
                            x > size.width * 2f / 3f -> {
                                volume = (volume - dragAmount.y / size.height).coerceIn(0f, 1f)
                                exoPlayer.volume = volume
                            }
                            else -> {
                                val seekDelta = (dragAmount.x * 50).toLong()
                                exoPlayer.seekTo((exoPlayer.currentPosition + seekDelta).coerceIn(0, exoPlayer.duration))
                            }
                        }
                    }
                }
        )

        if (showControls) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { showSubtitleSettings = !showSubtitleSettings }) {
                        Icon(Icons.Default.List, "Subtitles", tint = Color(0xFFDAA520))
                    }
                    IconButton(onClick = { showSpeedMenu = !showSpeedMenu }) {
                        Icon(Icons.Default.Favorite, "Speed", tint = Color.White)
                    }
                    IconButton(onClick = { isLocked = !isLocked }) {
                        Icon(Icons.Default.Lock, "Lock", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Speed menu
                if (showSpeedMenu) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp)).padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                            Button(
                                onClick = { playbackSpeed = speed; exoPlayer.playbackParameters = PlaybackParameters(speed); showSpeedMenu = false },
                                colors = ButtonDefaults.buttonColors(containerColor = if (playbackSpeed == speed) Color(0xFFE94560) else Color.DarkGray),
                                modifier = Modifier.height(36.dp)
                            ) { Text("${speed}x", fontSize = 12.sp) }
                        }
                    }
                }

                // Subtitle Settings Panel
                if (showSubtitleSettings) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Subtitle Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Size: ${subtitleConfig.fontSize.toInt()}", color = Color.Gray)
                            Slider(value = subtitleConfig.fontSize, valueRange = 12f..48f,
                                onValueChange = { subtitleConfig = subtitleConfig.copy(fontSize = it) },
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF2196F3), activeTrackColor = Color(0xFF2196F3)))

                            Text("Scale: ${subtitleConfig.fontScale.toInt()}%", color = Color.Gray)
                            Slider(value = subtitleConfig.fontScale, valueRange = 50f..200f,
                                onValueChange = { subtitleConfig = subtitleConfig.copy(fontScale = it) },
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF2196F3), activeTrackColor = Color(0xFF2196F3)))

                            Text("Color", color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                listOf(Color.White to "W", Color(0xFFDAA520) to "G", Color.Yellow to "Y", Color.Cyan to "C", Color(0xFF00FF00) to "R").forEach { (color, label) ->
                                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color)
                                        .clickable { subtitleConfig = subtitleConfig.copy(fontColor = color) })
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = subtitleConfig.isBold, onCheckedChange = { subtitleConfig = subtitleConfig.copy(isBold = it) })
                                Text("Bold", color = Color.White)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = subtitleConfig.backgroundEnabled, onCheckedChange = { subtitleConfig = subtitleConfig.copy(backgroundEnabled = it) })
                                Text("Background", color = Color.White)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = subtitleConfig.borderEnabled, onCheckedChange = { subtitleConfig = subtitleConfig.copy(borderEnabled = it) })
                                Text("Border", color = Color.White)
                            }
                            if (subtitleConfig.borderEnabled) {
                                Text("Border Width: ${subtitleConfig.borderWidth.toInt()}%", color = Color.Gray)
                                Slider(value = subtitleConfig.borderWidth, valueRange = 100f..500f,
                                    onValueChange = { subtitleConfig = subtitleConfig.copy(borderWidth = it) },
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF2196F3), activeTrackColor = Color(0xFF2196F3)))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = subtitleConfig.shadowEnabled, onCheckedChange = { subtitleConfig = subtitleConfig.copy(shadowEnabled = it) })
                                Text("Shadow", color = Color.White)
                            }
                            if (subtitleConfig.shadowEnabled) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = subtitleConfig.shadowFadeOut, onCheckedChange = { subtitleConfig = subtitleConfig.copy(shadowFadeOut = it) })
                                    Text("Fade out", color = Color.White)
                                }
                            }

                            Text("Position: ${(subtitleConfig.position * 100).toInt()}%", color = Color.Gray)
                            Slider(value = subtitleConfig.position, valueRange = 0.1f..0.95f,
                                onValueChange = { subtitleConfig = subtitleConfig.copy(position = it) },
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF00BFFF), activeTrackColor = Color(0xFF00BFFF)))
                        }
                    }
                }

                // Bottom controls
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTime(position), color = Color.Gray, fontSize = 12.sp)
                        Slider(value = position.toFloat(), valueRange = 0f..duration.toFloat(),
                            onValueChange = { exoPlayer.seekTo(it.toLong()) },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(thumbColor = Color(0xFFE94560), activeTrackColor = Color(0xFFE94560)))
                        Text(formatTime(duration), color = Color.Gray, fontSize = 12.sp)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { exoPlayer.seekTo(maxOf(0, exoPlayer.currentPosition - 10000)) }) { Icon(Icons.Default.Refresh, null, tint = Color.White) }
                        IconButton(onClick = { exoPlayer.seekTo(maxOf(0, exoPlayer.currentPosition - 5000)) }) { Icon(Icons.Default.Refresh, null, tint = Color.White) }
                        IconButton(onClick = { exoPlayer.playWhenReady = !isPlaying },
                            modifier = Modifier.size(64.dp).background(Color(0xFFE94560), CircleShape)) {
                            Icon(if (isPlaying) Icons.Default.Star else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        IconButton(onClick = { exoPlayer.seekTo(minOf(exoPlayer.duration, exoPlayer.currentPosition + 5000)) }) { Icon(Icons.Default.Refresh, null, tint = Color.White) }
                        IconButton(onClick = { exoPlayer.seekTo(minOf(exoPlayer.duration, exoPlayer.currentPosition + 10000)) }) { Icon(Icons.Default.Refresh, null, tint = Color.White) }
                    }

                    // Brightness and Volume controls - بزرگ‌تر و بهتر
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brightness
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = brightness,
                                valueRange = 0.05f..1f,
                                onValueChange = { brightness = it },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFD700),
                                    activeTrackColor = Color(0xFFFFD700)
                                )
                            )
                            Text("🔆 ${(brightness * 100).toInt()}%", color = Color(0xFFFFD700), fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Volume
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.VolumeUp, null, tint = Color(0xFF00BFFF), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = volume,
                                valueRange = 0f..1f,
                                onValueChange = {
                                    volume = it
                                    exoPlayer.volume = it
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00BFFF),
                                    activeTrackColor = Color(0xFF00BFFF)
                                )
                            )
                            Text("🔊 ${(volume * 100).toInt()}%", color = Color(0xFF00BFFF), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (isLocked) {
            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { isLocked = false } }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
