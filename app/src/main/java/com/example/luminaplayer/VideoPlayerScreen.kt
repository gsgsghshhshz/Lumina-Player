package com.example.luminaplayer

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(videoUri: Uri, onBack: () -> Unit) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    var buffered by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var brightness by remember { mutableFloatStateOf(0.5f) }
    var volume by remember { mutableFloatStateOf(1f) }
    var isLocked by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSubtitleSettings by remember { mutableStateOf(false) }
    var subtitleEnabled by remember { mutableStateOf(true) }
    var subtitleSize by remember { mutableFloatStateOf(24f) }
    var subtitleColor by remember { mutableStateOf(Color(0xFFDAA520)) }
    var subtitleOutline by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            playWhenReady = true
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            exoPlayer.let { p ->
                position = p.currentPosition
                duration = p.duration.coerceAtLeast(1)
                buffered = p.bufferedPercentage
                isPlaying = p.isPlaying
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
                            if (offset.x < size.width / 3) {
                                exoPlayer.seekTo(maxOf(0, exoPlayer.currentPosition - 10000))
                            } else if (offset.x > size.width * 2 / 3) {
                                exoPlayer.seekTo(minOf(exoPlayer.duration, exoPlayer.currentPosition + 10000))
                            } else {
                                exoPlayer.playWhenReady = !isPlaying
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val x = change.position.x
                        val y = change.position.y
                        when {
                            x < size.width / 3f -> {
                                brightness = (brightness - dragAmount.y / size.height).coerceIn(0f, 1f)
                                val activity = context as? android.app.Activity
                                activity?.window?.attributes?.let { lp ->
                                    lp.screenBrightness = brightness
                                    activity.window.attributes = lp
                                }
                            }
                            x > size.width * 2f / 3f -> {
                                volume = (volume - dragAmount.y / size.height).coerceIn(0f, 1f)
                                exoPlayer.volume = volume
                            }
                            else -> {
                                val seekDelta = (dragAmount.x * 50).toLong()
                                exoPlayer.seekTo(
                                    (exoPlayer.currentPosition + seekDelta).coerceIn(0, exoPlayer.duration)
                                )
                            }
                        }
                    }
                }
        )

        if (showControls) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { showSubtitleSettings = !showSubtitleSettings }) {
                        Icon(Icons.Default.List, "Subtitles", tint = Color(0xFFDAA520))
                    }
                    IconButton(onClick = { showSpeedMenu = !showSpeedMenu }) {
                        Icon(Icons.Default.Favorite, "Speed", tint = Color.White)
                    }
                    IconButton(onClick = { isLocked = !isLocked }) {
                        Icon(
                            Icons.Default.Lock,
                            "Lock",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Speed menu
                if (showSpeedMenu) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                            Button(
                                onClick = {
                                    playbackSpeed = speed
                                    exoPlayer.playbackParameters = PlaybackParameters(speed)
                                    showSpeedMenu = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (playbackSpeed == speed) Color(0xFFE94560) else Color.DarkGray
                                ),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("${speed}x", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Subtitle settings panel
                if (showSubtitleSettings) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Subtitle Settings", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Subtitle", color = Color.White, modifier = Modifier.weight(1f))
                                Switch(checked = subtitleEnabled, onCheckedChange = { subtitleEnabled = it })
                            }

                            Text("Size: ${subtitleSize.toInt()}", color = Color.Gray)
                            Slider(
                                value = subtitleSize,
                                valueRange = 12f..48f,
                                onValueChange = { subtitleSize = it },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFDAA520),
                                    activeTrackColor = Color(0xFFDAA520)
                                )
                            )

                            Text("Color", color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    Color(0xFFDAA520) to "Gold",
                                    Color.White to "White",
                                    Color.Yellow to "Yellow",
                                    Color.Cyan to "Cyan"
                                ).forEach { (color, name) ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Outline", color = Color.White, modifier = Modifier.weight(1f))
                                Switch(checked = subtitleOutline, onCheckedChange = { subtitleOutline = it })
                            }
                        }
                    }
                }

                // Bottom controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(16.dp)
                ) {
                    // Progress bar
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTime(position), color = Color.Gray, fontSize = 12.sp)
                        Slider(
                            value = position.toFloat(),
                            valueRange = 0f..duration.toFloat(),
                            onValueChange = { exoPlayer.seekTo(it.toLong()) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFE94560),
                                activeTrackColor = Color(0xFFE94560)
                            )
                        )
                        Text(formatTime(duration), color = Color.Gray, fontSize = 12.sp)
                    }

                    // Control buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { exoPlayer.seekTo(maxOf(0, exoPlayer.currentPosition - 10000)) }) {
                            Icon(Icons.Default.Refresh, null, tint = Color.White)
                        }
                        IconButton(onClick = { exoPlayer.seekTo(maxOf(0, exoPlayer.currentPosition - 5000)) }) {
                            Icon(Icons.Default.Refresh, null, tint = Color.White)
                        }
                        IconButton(
                            onClick = { exoPlayer.playWhenReady = !isPlaying },
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFE94560), CircleShape)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Star else Icons.Default.PlayArrow,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        IconButton(onClick = { exoPlayer.seekTo(minOf(exoPlayer.duration, exoPlayer.currentPosition + 5000)) }) {
                            Icon(Icons.Default.Refresh, null, tint = Color.White)
                        }
                        IconButton(onClick = { exoPlayer.seekTo(minOf(exoPlayer.duration, exoPlayer.currentPosition + 10000)) }) {
                            Icon(Icons.Default.Refresh, null, tint = Color.White)
                        }
                    }

                    // Extra controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                            Slider(
                                value = brightness,
                                valueRange = 0f..1f,
                                onValueChange = { brightness = it },
                                modifier = Modifier.width(80.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFD700),
                                    activeTrackColor = Color(0xFFFFD700)
                                )
                            )
                            Text("Brightness", color = Color.Gray, fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VolumeUp, null, tint = Color(0xFF00BFFF), modifier = Modifier.size(18.dp))
                            Slider(
                                value = volume,
                                valueRange = 0f..1f,
                                onValueChange = {
                                    volume = it
                                    exoPlayer.volume = it
                                },
                                modifier = Modifier.width(80.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00BFFF),
                                    activeTrackColor = Color(0xFF00BFFF)
                                )
                            )
                            Text("Volume", color = Color.Gray, fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${playbackSpeed}x", color = Color(0xFFE94560), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Speed", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Lock overlay
        if (isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { isLocked = false }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
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
