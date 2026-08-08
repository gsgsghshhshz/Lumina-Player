package com.example.luminaplayer

import android.app.Activity
import android.net.Uri
import android.view.WindowManager
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

data class SubtitleConfig(
    val fontPath: String = "",
    val subtitleSize: SubtitleSize = SubtitleSize.MEDIUM,
    val fontScale: Float = 100f,
    val fontColor: Color = Color.White,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val backgroundEnabled: Boolean = false,
    val borderEnabled: Boolean = true,
    val borderWidth: Float = 200f,
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
    var videoRotation by remember { mutableFloatStateOf(0f) }
    var subtitleEntries by remember { mutableStateOf<List<SubtitleEntry>>(emptyList()) }
    var customFontFamily by remember { mutableStateOf<FontFamily?>(null) }
    var showFontPicker by remember { mutableStateOf(false) }
    var availableFonts by remember { mutableStateOf<List<java.io.File>>(emptyList()) }

    // اسکن فونت‌ها با try-catch برای جلوگیری از کرش
    LaunchedEffect(Unit) {
        try {
            availableFonts = FontScanner.scanFonts(context)
        } catch (e: Exception) {
            availableFonts = emptyList()
        }
    }

    // پارس زیرنویس
    LaunchedEffect(subtitleUri) {
        try {
            if (subtitleUri != null) {
                subtitleEntries = SubtitleParser.detectAndParse(context, subtitleUri)
            }
        } catch (e: Exception) {
            subtitleEntries = emptyList()
        }
    }

    // اعمال روشنایی
    LaunchedEffect(brightness) {
        try {
            activity?.window?.attributes = activity.window.attributes.apply {
                screenBrightness = brightness
            }
        } catch (e: Exception) {}
    }

    // آپدیت فونت
    LaunchedEffect(subtitleConfig.fontPath) {
        try {
            customFontFamily = if (subtitleConfig.fontPath.isNotEmpty()) {
                FontScanner.loadFont(subtitleConfig.fontPath)
            } else null
        } catch (e: Exception) {
            customFontFamily = null
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.Builder().setUri(videoUri).build()
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
                .graphicsLayer { rotationZ = videoRotation }
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

        // رندر کاستوم زیرنویس
        SubtitleOverlay(
            entries = subtitleEntries,
            currentPositionMs = position,
            config = subtitleConfig,
            customFontFamily = customFontFamily,
            modifier = Modifier.fillMaxSize()
        )

        if (showControls) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { showSubtitleSettings = !showSubtitleSettings }) {
                        Icon(Icons.Default.List, contentDescription = "Subtitles", tint = Color(0xFFDAA520))
                    }
                    IconButton(onClick = { showSpeedMenu = !showSpeedMenu }) {
                        Icon(Icons.Default.Favorite, contentDescription = "Speed", tint = Color.White)
                    }
                    IconButton(onClick = { isLocked = !isLocked }) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color.White)
                    }
                    IconButton(onClick = { videoRotation = (videoRotation + 90f) % 360f }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rotate", tint = Color(0xFF00BFFF))
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
                        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                            Text("Subtitle Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Font Picker
                            Text("Font", color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showFontPicker = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                                ) { Text("Choose Font", fontSize = 12.sp) }
                                Button(
                                    onClick = { subtitleConfig = subtitleConfig.copy(fontPath = ""); customFontFamily = null },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                ) { Text("Default", fontSize = 12.sp) }
                            }
                            if (subtitleConfig.fontPath.isNotEmpty()) {
                                Text("Active: ${subtitleConfig.fontPath.split("/").last()}", color = Color(0xFFDAA520), fontSize = 10.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Subtitle Size - Low/Medium/High
                            Text("Size", color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SubtitleSize.entries.forEach { size ->
                                    Button(
                                        onClick = { subtitleConfig = subtitleConfig.copy(subtitleSize = size) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (subtitleConfig.subtitleSize == size) Color(0xFF2196F3) else Color.DarkGray
                                        )
                                    ) { Text(size.name, fontSize = 11.sp) }
                                }
                            }

                            // Scale
                            Text("Scale: ${subtitleConfig.fontScale.toInt()}%", color = Color.Gray)
                            Slider(value = subtitleConfig.fontScale, valueRange = 50f..200f,
                                onValueChange = { subtitleConfig = subtitleConfig.copy(fontScale = it) },
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF2196F3), activeTrackColor = Color(0xFF2196F3)))

                            // Color
                            Text("Color", color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                listOf(Color.White to "W", Color(0xFFDAA520) to "G", Color.Yellow to "Y", Color.Cyan to "C", Color(0xFF00FF00) to "R").forEach { (color, _) ->
                                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color)
                                        .clickable { subtitleConfig = subtitleConfig.copy(fontColor = color) })
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = subtitleConfig.isBold, onCheckedChange = { subtitleConfig = subtitleConfig.copy(isBold = it) })
                                Text("Bold", color = Color.White)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = subtitleConfig.isItalic, onCheckedChange = { subtitleConfig = subtitleConfig.copy(isItalic = it) })
                                Text("Italic", color = Color.White)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = subtitleConfig.borderEnabled, onCheckedChange = { subtitleConfig = subtitleConfig.copy(borderEnabled = it) })
                                Text("Border", color = Color.White)
                            }
                            if (subtitleConfig.borderEnabled) {
                                Slider(value = subtitleConfig.borderWidth, valueRange = 100f..500f,
                                    onValueChange = { subtitleConfig = subtitleConfig.copy(borderWidth = it) },
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF2196F3), activeTrackColor = Color(0xFF2196F3)))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = subtitleConfig.shadowEnabled, onCheckedChange = { subtitleConfig = subtitleConfig.copy(shadowEnabled = it) })
                                Text("Shadow", color = Color.White)
                            }

                            Text("Position: ${(subtitleConfig.position * 100).toInt()}%", color = Color.Gray)
                            Slider(value = subtitleConfig.position, valueRange = 0.1f..0.95f,
                                onValueChange = { subtitleConfig = subtitleConfig.copy(position = it) },
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF00BFFF), activeTrackColor = Color(0xFF00BFFF)))
                        }
                    }
                }

                // Font Picker Dialog
                if (showFontPicker) {
                    AlertDialog(
                        onDismissRequest = { showFontPicker = false },
                        title = { Text("Choose Font", color = Color.White) },
                        text = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                if (availableFonts.isEmpty()) {
                                    Text("No fonts found.", color = Color.Gray)
                                } else {
                                    availableFonts.forEach { fontFile ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .clickable {
                                                    subtitleConfig = subtitleConfig.copy(fontPath = fontFile.absolutePath)
                                                    showFontPicker = false
                                                }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.List, null, tint = Color(0xFFDAA520), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(fontFile.name, color = Color.White)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showFontPicker = false }) { Text("Cancel", color = Color(0xFFE94560)) }
                        },
                        containerColor = Color(0xFF2A2A2A)
                    )
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
                        IconButton(onClick = { exoPlayer.seekTo(maxOf(0, exoPlayer.currentPosition - 10000)) }) { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White) }
                        IconButton(onClick = { exoPlayer.seekTo(maxOf(0, exoPlayer.currentPosition - 5000)) }) { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White) }
                        IconButton(onClick = { exoPlayer.playWhenReady = !isPlaying },
                            modifier = Modifier.size(64.dp).background(Color(0xFFE94560), CircleShape)) {
                            Icon(if (isPlaying) Icons.Default.Star else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        IconButton(onClick = { exoPlayer.seekTo(minOf(exoPlayer.duration, exoPlayer.currentPosition + 5000)) }) { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White) }
                        IconButton(onClick = { exoPlayer.seekTo(minOf(exoPlayer.duration, exoPlayer.currentPosition + 10000)) }) { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White) }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(value = brightness, valueRange = 0.05f..1f, onValueChange = { brightness = it },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700)))
                            Text("Bri ${(brightness * 100).toInt()}%", color = Color(0xFFFFD700), fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF00BFFF), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(value = volume, valueRange = 0f..1f, onValueChange = { volume = it; exoPlayer.volume = it },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF00BFFF), activeTrackColor = Color(0xFF00BFFF)))
                            Text("Vol ${(volume * 100).toInt()}%", color = Color(0xFF00BFFF), fontSize = 11.sp)
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
