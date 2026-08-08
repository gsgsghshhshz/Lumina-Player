package com.example.luminaplayer

import android.app.Activity
import android.net.Uri
import android.view.WindowManager
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.graphicsLayer
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

enum class SubtitleSize(val scale: Float) { LOW(0.6f), MEDIUM(1.0f), HIGH(1.5f) }

data class SubtitleConfig(
    val fontPath: String = "",
    val subtitleSize: SubtitleSize = SubtitleSize.MEDIUM,
    val fontScale: Float = 100f,
    val fontColor: Color = Color.White,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val borderEnabled: Boolean = true,
    val borderWidth: Float = 200f,
    val shadowEnabled: Boolean = false,
    val shadowFadeOut: Boolean = true,
    val position: Float = 0.9f
)

@Composable
fun PlayerScreen(videoUri: Uri, subtitleUri: Uri?, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val density = LocalDensity.current

    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var brightness by remember { mutableFloatStateOf(0.5f) }
    var volume by remember { mutableFloatStateOf(0.8f) }
    var isLocked by remember { mutableStateOf(false) }
    var videoRotation by remember { mutableFloatStateOf(0f) }
    var isPrepared by remember { mutableStateOf(false) }
    var subtitleConfig by remember { mutableStateOf(SubtitleConfig()) }
    var subtitleEntries by remember { mutableStateOf<List<SubtitleEntry>>(emptyList()) }
    var customFontFamily by remember { mutableStateOf<FontFamily?>(null) }

    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showTrackDialog by remember { mutableStateOf(false) }
    var showFontPicker by remember { mutableStateOf(false) }
    var availableFonts by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    var embeddedTracks by remember { mutableStateOf<List<Pair<String,String>>>(emptyList()) }
    var selectedTrackLabel by remember { mutableStateOf("Off") }
    var subtitleEnabled by remember { mutableStateOf(true) }
    var videoTitle by remember { mutableStateOf("Lumina Player") }

    // Get title from URI
    LaunchedEffect(videoUri) {
        videoTitle = videoUri.lastPathSegment ?: "Lumina Player"
    }

    // Scan fonts safely
    LaunchedEffect(Unit) {
        try { availableFonts = FontScanner.scanFonts(context) } catch (_: Exception) {}
    }

    // Parse external subtitles
    LaunchedEffect(subtitleUri) {
        try { if (subtitleUri != null) subtitleEntries = SubtitleParser.detectAndParse(context, subtitleUri) } catch (_: Exception) {}
    }

    // Apply brightness to window
    LaunchedEffect(brightness) {
        try { activity?.window?.attributes = activity.window.attributes.apply { screenBrightness = brightness } } catch (_: Exception) {}
    }

    // Apply custom font
    LaunchedEffect(subtitleConfig.fontPath) {
        try { customFontFamily = if (subtitleConfig.fontPath.isNotEmpty()) FontScanner.loadFont(subtitleConfig.fontPath) else null } catch (_: Exception) { customFontFamily = null }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.Builder().setUri(videoUri).build())
            prepare()
            playWhenReady = true
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        isPrepared = true
                        duration = this@apply.duration.coerceAtLeast(1)
                        // Find embedded subtitle tracks
                        val tracks = this@apply.currentTracks
                        val subs = mutableListOf<Pair<String,String>>()
                        for (i in 0 until tracks.groups.size) {
                            val g = tracks.groups[i]
                            if (g.type == C.TRACK_TYPE_TEXT) {
                                for (j in 0 until g.length) {
                                    val fmt = g.getTrackFormat(j)
                                    subs.add((fmt.label ?: fmt.language ?: "Track") to (fmt.language ?: "und"))
                                }
                            }
                        }
                        embeddedTracks = subs
                    }
                }
                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
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

    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    val baseFontSize = with(density) {
        val screenH = context.resources.displayMetrics.heightPixels
        when { screenH > 2400 -> 20f; screenH > 1800 -> 16f; screenH > 1200 -> 14f; else -> 12f }
    }
    val finalFontSize = baseFontSize * subtitleConfig.subtitleSize.scale * (subtitleConfig.fontScale / 100f)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Video player
        AndroidView(
            factory = { PlayerView(it).apply { player = exoPlayer; useController = false; setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS) } },
            modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = videoRotation }
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
                    detectDragGestures { change, drag ->
                        change.consume()
                        val x = change.position.x
                        when {
                            x < size.width / 3f -> brightness = (brightness - drag.y / size.height).coerceIn(0.05f, 1f)
                            x > size.width * 2f / 3f -> { volume = (volume - drag.y / size.height).coerceIn(0f, 1f); exoPlayer.volume = volume }
                            else -> exoPlayer.seekTo((exoPlayer.currentPosition + drag.x * 50).toLong().coerceIn(0, exoPlayer.duration))
                        }
                    }
                }
        )

        // Subtitle overlay
        if (subtitleEnabled && subtitleEntries.isNotEmpty()) {
            SubtitleOverlay(subtitleEntries, position, finalFontSize.sp, subtitleConfig, customFontFamily, Modifier.fillMaxSize())
        }

        // Controls overlay
        if (!isLocked && showControls) {
            Column(modifier = Modifier.fillMaxSize()) {
            // Top bar - Just Player style
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                Spacer(modifier = Modifier.width(8.dp))
                Text(videoTitle, color = Color.White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
                IconButton(onClick = { showTrackDialog = true }) { Icon(Icons.Default.List, contentDescription = "Tracks", tint = Color(0xFFDAA520)) }
                IconButton(onClick = { showSpeedMenu = !showSpeedMenu }) { Icon(Icons.Default.Favorite, contentDescription = "Speed", tint = Color.White) }
                IconButton(onClick = { showSettings = !showSettings }) { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White) }
                IconButton(onClick = { videoRotation = (videoRotation + 90f) % 360f }) { Icon(Icons.Default.Refresh, contentDescription = "Rotate", tint = Color(0xFF00BFFF)) }
                IconButton(onClick = { isLocked = true }) { Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color.White) }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom bar - Just Player style
            Column(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                // Seek bar
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(formatTime(position), color = Color.Gray, fontSize = 12.sp)
                    Slider(value = position.toFloat(), valueRange = 0f..duration.toFloat(), onValueChange = { exoPlayer.seekTo(it.toLong()) },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White))
                    Text(formatTime(duration), color = Color.Gray, fontSize = 12.sp)
                }

                // Control buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { exoPlayer.seekTo(maxOf(0, exoPlayer.currentPosition - 10000)) }) { Icon(Icons.Default.Refresh, contentDescription = "-10s", tint = Color.White) }
                    IconButton(onClick = { exoPlayer.seekTo(maxOf(0, exoPlayer.currentPosition - 5000)) }) { Icon(Icons.Default.Refresh, contentDescription = "-5s", tint = Color.White) }
                    IconButton(onClick = { exoPlayer.playWhenReady = !isPlaying }, modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)) {
                        Icon(if (isPlaying) Icons.Default.Star else Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = { exoPlayer.seekTo(minOf(exoPlayer.duration, exoPlayer.currentPosition + 5000)) }) { Icon(Icons.Default.Refresh, contentDescription = "+5s", tint = Color.White) }
                    IconButton(onClick = { exoPlayer.seekTo(minOf(exoPlayer.duration, exoPlayer.currentPosition + 10000)) }) { Icon(Icons.Default.Refresh, contentDescription = "+10s", tint = Color.White) }
                }

                // Brightness + Volume
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = "Brightness", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                    Slider(value = brightness, valueRange = 0.05f..1f, onValueChange = { brightness = it }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Star, contentDescription = "Volume", tint = Color(0xFF00BFFF), modifier = Modifier.size(16.dp))
                    Slider(value = volume, valueRange = 0f..1f, onValueChange = { volume = it; exoPlayer.volume = it }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00BFFF), activeTrackColor = Color(0xFF00BFFF)))
                }
            } // Column end
            } // if controls end
        }

        // Speed menu
        if (showSpeedMenu && showControls && !isLocked) {
            Row(modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(12.dp)).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                    Button(onClick = { playbackSpeed = s; exoPlayer.playbackParameters = PlaybackParameters(s); showSpeedMenu = false },
                        colors = ButtonDefaults.buttonColors(containerColor = if (playbackSpeed == s) Color(0xFFE94560) else Color.DarkGray), modifier = Modifier.height(32.dp)) { Text("${s}x", fontSize = 11.sp) }
                }
            }
        }

        // Settings dialog
        if (showSettings) {
            AlertDialog(onDismissRequest = { showSettings = false }, title = { Text("Subtitle Settings", color = Color.White) },
                text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Size", color = Color.Gray); Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SubtitleSize.entries.forEach { sz -> Button(onClick = { subtitleConfig = subtitleConfig.copy(subtitleSize = sz) }, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (subtitleConfig.subtitleSize == sz) Color(0xFF2196F3) else Color.DarkGray)) { Text(sz.name, fontSize = 10.sp) } }
                    }
                    Text("Scale: ${subtitleConfig.fontScale.toInt()}%", color = Color.Gray)
                    Slider(value = subtitleConfig.fontScale, valueRange = 50f..200f, onValueChange = { subtitleConfig = subtitleConfig.copy(fontScale = it) }, colors = SliderDefaults.colors(thumbColor = Color(0xFF2196F3), activeTrackColor = Color(0xFF2196F3)))
                    Text("Color", color = Color.Gray); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        listOf(Color.White, Color(0xFFDAA520), Color.Yellow, Color.Cyan, Color(0xFF00FF00)).forEach { c -> Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(c).clickable { subtitleConfig = subtitleConfig.copy(fontColor = c) }) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = subtitleConfig.isBold, onCheckedChange = { subtitleConfig = subtitleConfig.copy(isBold = it) }); Text("Bold", color = Color.White, fontSize = 13.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = subtitleConfig.isItalic, onCheckedChange = { subtitleConfig = subtitleConfig.copy(isItalic = it) }); Text("Italic", color = Color.White, fontSize = 13.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = subtitleConfig.borderEnabled, onCheckedChange = { subtitleConfig = subtitleConfig.copy(borderEnabled = it) }); Text("Border", color = Color.White, fontSize = 13.sp) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = subtitleConfig.shadowEnabled, onCheckedChange = { subtitleConfig = subtitleConfig.copy(shadowEnabled = it) }); Text("Shadow", color = Color.White, fontSize = 13.sp) }
                    Text("Position: ${(subtitleConfig.position * 100).toInt()}%", color = Color.Gray)
                    Slider(value = subtitleConfig.position, valueRange = 0.1f..0.95f, onValueChange = { subtitleConfig = subtitleConfig.copy(position = it) }, colors = SliderDefaults.colors(thumbColor = Color(0xFF00BFFF), activeTrackColor = Color(0xFF00BFFF)))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Font", color = Color.Gray); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showFontPicker = true; showSettings = false }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Text("Choose Font", fontSize = 11.sp) }
                        Button(onClick = { subtitleConfig = subtitleConfig.copy(fontPath = ""); customFontFamily = null }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text("Default", fontSize = 11.sp) }
                    }
                    if (subtitleConfig.fontPath.isNotEmpty()) Text("Active: ${subtitleConfig.fontPath.split("/").last()}", color = Color(0xFFDAA520), fontSize = 10.sp)
                } },
                confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Done", color = Color(0xFF2196F3)) } },
                containerColor = Color(0xFF1E1E1E))
        }

        // Track dialog
        if (showTrackDialog) {
            AlertDialog(onDismissRequest = { showTrackDialog = false }, title = { Text("Subtitle Track", color = Color.White) },
                text = { Column {
                    Row(modifier = Modifier.fillMaxWidth().clickable { exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build(); selectedTrackLabel = "Off"; subtitleEnabled = false; showTrackDialog = false }.padding(8.dp)) {
                        Text("Off", color = if (selectedTrackLabel == "Off") Color(0xFFE94560) else Color.White) }
                    embeddedTracks.forEach { (label, lang) -> Row(modifier = Modifier.fillMaxWidth().clickable { exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).setPreferredTextLanguage(lang).build(); selectedTrackLabel = label; subtitleEnabled = true; showTrackDialog = false }.padding(8.dp)) {
                        Text("$label ($lang)", color = if (selectedTrackLabel == label) Color(0xFFE94560) else Color.White) } }
                    if (embeddedTracks.isEmpty()) Text("No embedded subtitles", color = Color.Gray, fontSize = 12.sp)
                } },
                confirmButton = { TextButton(onClick = { showTrackDialog = false }) { Text("Cancel", color = Color(0xFFE94560)) } },
                containerColor = Color(0xFF1E1E1E))
        }

        // Font picker
        if (showFontPicker) {
            AlertDialog(onDismissRequest = { showFontPicker = false }, title = { Text("Choose Font", color = Color.White) },
                text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (availableFonts.isEmpty()) Text("No fonts found.", color = Color.Gray) else
                        availableFonts.forEach { f -> Row(modifier = Modifier.fillMaxWidth().clickable { subtitleConfig = subtitleConfig.copy(fontPath = f.absolutePath); showFontPicker = false }.padding(8.dp)) { Text(f.name, color = Color.White) } }
                } },
                confirmButton = { TextButton(onClick = { showFontPicker = false }) { Text("Cancel", color = Color(0xFFE94560)) } },
                containerColor = Color(0xFF1E1E1E))
        }

        // Lock overlay
        if (isLocked) {
            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { isLocked = false } }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lock, contentDescription = "Unlock", tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val t = ms / 1000; val h = t / 3600; val m = (t % 3600) / 60; val s = t % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
