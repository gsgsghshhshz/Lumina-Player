package com.example.luminaplayer

import android.app.Activity
import android.content.Context
import android.media.AudioManager
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.graphicsLayer
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

enum class SubtitleSize(val scale: Float) { LOW(0.6f), MEDIUM(1.0f), HIGH(1.5f) }
data class SubtitleConfig(val fontPath: String = "", val subtitleSize: SubtitleSize = SubtitleSize.MEDIUM, val fontScale: Float = 100f, val fontColor: Color = Color.White, val isBold: Boolean = false, val isItalic: Boolean = false, val borderEnabled: Boolean = true, val borderWidth: Float = 200f, val shadowEnabled: Boolean = false, val position: Float = 0.9f)

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(videoUri: Uri, subtitleUri: Uri?, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val density = LocalDensity.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    var isPlaying by remember { mutableStateOf(false) }
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
    var embeddedTracks by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var embeddedAudioTracks by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var selectedTrackLabel by remember { mutableStateOf("Off") }
    var selectedAudioLabel by remember { mutableStateOf("Default") }
    var subtitleEnabled by remember { mutableStateOf(true) }
    var videoTitle by remember { mutableStateOf("Lumina Player") }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitlePanel by remember { mutableStateOf(false) }
    var subtitleSync by remember { mutableFloatStateOf(0f) }
    var subtitleSpeed by remember { mutableFloatStateOf(100f) }
    var showShortcutPanel by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var videoInfo by remember { mutableStateOf("") }
    var volumeBoost by remember { mutableIntStateOf(100) }
    var aspectRatio by remember { mutableIntStateOf(0) }

    LaunchedEffect(videoUri) { videoTitle = videoUri.lastPathSegment?.replace("+", " ") ?: "Lumina Player" }
    LaunchedEffect(Unit) { try { availableFonts = FontScanner.scanFonts(context) } catch (_: Exception) {} }
    LaunchedEffect(subtitleUri) { try { if (subtitleUri != null) subtitleEntries = SubtitleParser.detectAndParse(context, subtitleUri) } catch (_: Exception) {} }
    LaunchedEffect(brightness) { try { activity?.window?.attributes = activity.window.attributes.apply { screenBrightness = brightness } } catch (_: Exception) {} }
    LaunchedEffect(subtitleConfig.fontPath) { try { customFontFamily = if (subtitleConfig.fontPath.isNotEmpty()) FontScanner.loadFont(subtitleConfig.fontPath) else null } catch (_: Exception) { customFontFamily = null } }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.Builder().setUri(videoUri).build())
            prepare(); playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        isPrepared = true; duration = this@apply.duration.coerceAtLeast(1)
                        val tracks = this@apply.currentTracks
                        val subs = mutableListOf<Pair<String, String>>()
                        val audios = mutableListOf<Pair<String, String>>()
                        for (i in 0 until tracks.groups.size) {
                            val g = tracks.groups[i]
                            if (g.type == C.TRACK_TYPE_TEXT) {
                                for (j in 0 until g.length) {
                                    val fmt = g.getTrackFormat(j)
                                    subs.add((fmt.label ?: fmt.language ?: "Track") to (fmt.language ?: "und"))
                                }
                            }
                            if (g.type == C.TRACK_TYPE_AUDIO) {
                                for (j in 0 until g.length) {
                                    val fmt = g.getTrackFormat(j)
                                    audios.add((fmt.label ?: fmt.language ?: "Audio") to (fmt.language ?: "und"))
                                }
                            }
                        }
                        embeddedTracks = subs; embeddedAudioTracks = audios
                        try { videoInfo = "Resolution: ${this@apply.videoFormat?.width}x${this@apply.videoFormat?.height}\nCodec: ${this@apply.videoFormat?.codecName ?: "N/A"}\nAudio: ${this@apply.audioFormat?.codecName ?: "N/A"}\nDuration: ${formatTime(duration)}\nFile: $videoTitle" } catch (_: Exception) {}
                    }
                }
                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            })
        }
    }

    LaunchedEffect(Unit) { while (true) { if (isPrepared) { position = exoPlayer.currentPosition; duration = exoPlayer.duration.coerceAtLeast(1); isPlaying = exoPlayer.isPlaying }; delay(200) } }
    DisposableEffect(Unit) { onDispose { exoPlayer.release(); try { activity?.window?.attributes = activity.window.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE } } catch (_: Exception) {} } }

    val baseFontSize = with(density) { val h = context.resources.displayMetrics.heightPixels; when { h > 2400 -> 20f; h > 1800 -> 16f; h > 1200 -> 14f; else -> 12f } }
    val finalFontSize = baseFontSize * subtitleConfig.subtitleSize.scale * (subtitleConfig.fontScale / 100f)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Video
        AndroidView(
            factory = { PlayerView(it).apply { player = exoPlayer; useController = false; setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS) } },
            modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = videoRotation }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { if (!isLocked) showControls = !showControls },
                        onDoubleTap = { offset ->
                            when { offset.x < size.width / 3f -> exoPlayer.seekTo(maxOf(0, exoPlayer.currentPosition - 10000)); offset.x > size.width * 2f / 3f -> exoPlayer.seekTo(minOf(exoPlayer.duration, exoPlayer.currentPosition + 10000)); else -> exoPlayer.playWhenReady = !isPlaying }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        when { change.position.x < size.width / 3f -> brightness = (brightness - drag.y / size.height).coerceIn(0.05f, 1f); change.position.x > size.width * 2f / 3f -> { volume = (volume - drag.y / size.height).coerceIn(0f, 1f); exoPlayer.volume = volume }; else -> exoPlayer.seekTo((exoPlayer.currentPosition + drag.x * 50).toLong().coerceIn(0, exoPlayer.duration)) }
                    }
                }
        )

        // Subtitles
        if (subtitleEnabled && subtitleEntries.isNotEmpty()) {
            SubtitleOverlay(subtitleEntries, position, finalFontSize.sp, subtitleConfig, customFontFamily, Modifier.fillMaxSize())
        }

        // Controls
        if (!isLocked && showControls) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar (MX Player style)
                Row(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp)) }
                    Text(videoTitle, color = Color.White, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
                    IconButton(onClick = { showSubtitlePanel = true }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.List, contentDescription = "Subtitles", tint = Color(0xFFDAA520), modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = { showAudioDialog = true }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Favorite, contentDescription = "Audio", tint = Color(0xFF00BFFF), modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = { showSpeedMenu = !showSpeedMenu }, modifier = Modifier.size(36.dp)) { Text("HW+", color = Color.White, fontSize = 10.sp, modifier = Modifier.background(Color.DarkGray, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) }
                    IconButton(onClick = { showSettings = true }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(18.dp)) }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Quick settings bar (MX Player style)
                if (showControls && !isLocked) {
                    Row(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).padding(4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("EQ" to "Equalizer", "1x" to "Speed", "📷" to "Screenshot", "🔊" to "Audio", "↻" to "Rotate", ">" to "More").forEach { (icon, label) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                                when (label) { "Speed" -> showSpeedMenu = !showSpeedMenu; "Rotate" -> videoRotation = (videoRotation + 90f) % 360f; "Audio" -> showAudioDialog = true; "More" -> showShortcutPanel = true }
                            }) {
                                Text(icon, fontSize = 14.sp, color = Color.White)
                                Text(label, fontSize = 8.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // Bottom bar (MX Player style)
                Column(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                    // Seek bar
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(formatTime(position), color = Color.Gray, fontSize = 11.sp)
                        Slider(value = position.toFloat(), valueRange = 0f..duration.toFloat(), onValueChange = { exoPlayer.seekTo(it.toLong()) }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), colors = SliderDefaults.colors(thumbColor = Color(0xFF00BFFF), activeTrackColor = Color(0xFF00BFFF), inactiveTrackColor = Color.DarkGray))
                        Text(formatTime(duration), color = Color.Gray, fontSize = 11.sp)
                    }
                    // Controls row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isLocked = true }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color.White, modifier = Modifier.size(18.dp)) }
                        IconButton(onClick = { exoPlayer.seekTo(maxOf(0, exoPlayer.currentPosition - 10000)) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Refresh, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(20.dp)) }
                        IconButton(onClick = { exoPlayer.playWhenReady = !isPlaying }, modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)) {
                            Icon(if (isPlaying) Icons.Default.Star else Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        IconButton(onClick = { exoPlayer.seekTo(minOf(exoPlayer.duration, exoPlayer.currentPosition + 10000)) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Refresh, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(20.dp)) }
                        IconButton(onClick = { aspectRatio = (aspectRatio + 1) % 3 }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Refresh, contentDescription = "Aspect", tint = Color.White, modifier = Modifier.size(18.dp)) }
                    }
                    // Brightness + Volume
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = "Brightness", tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                        Slider(value = brightness, valueRange = 0.05f..1f, onValueChange = { brightness = it }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700), inactiveTrackColor = Color.DarkGray))
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Star, contentDescription = "Volume", tint = Color(0xFF00BFFF), modifier = Modifier.size(14.dp))
                        Slider(value = volume, valueRange = 0f..1f, onValueChange = { volume = it; exoPlayer.volume = it }, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), colors = SliderDefaults.colors(thumbColor = Color(0xFF00BFFF), activeTrackColor = Color(0xFF00BFFF), inactiveTrackColor = Color.DarkGray))
                    }
                }
            }
        }

        // Speed menu
        if (showSpeedMenu && showControls && !isLocked) {
            Row(modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.9f), RoundedCornerShape(12.dp)).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                    Button(onClick = { playbackSpeed = s; exoPlayer.playbackParameters = PlaybackParameters(s); showSpeedMenu = false }, colors = ButtonDefaults.buttonColors(containerColor = if (playbackSpeed == s) Color(0xFFE94560) else Color.DarkGray), modifier = Modifier.height(32.dp)) { Text("${s}x", fontSize = 11.sp) }
                }
            }
        }

        // Subtitle Panel (MX Player style)
        if (showSubtitlePanel) {
            AlertDialog(onDismissRequest = { showSubtitlePanel = false }, title = { Row { Text("Subtitle", color = Color.White, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.weight(1f)); Text("Online subtitles", color = Color(0xFF2196F3), fontSize = 12.sp, modifier = Modifier.clickable { showSubtitlePanel = false }) } },
                text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(modifier = Modifier.fillMaxWidth().clickable { showSubtitlePanel = false }.padding(vertical = 8.dp)) { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Open", color = Color.White) }
                    if (subtitleEntries.isNotEmpty()) { Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Loaded subtitle", color = Color.White) } }
                    HorizontalDivider(color = Color.DarkGray)
                    Text("Synchronization", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { subtitleSync = (subtitleSync - 0.5f).coerceAtLeast(-10f) }, modifier = Modifier.size(32.dp)) { Text("-", color = Color.White, fontSize = 16.sp) }
                        Text("${String.format("%.1f", subtitleSync)}s", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(onClick = { subtitleSync = (subtitleSync + 0.5f).coerceAtMost(10f) }, modifier = Modifier.size(32.dp)) { Text("+", color = Color.White, fontSize = 16.sp) }
                    }
                    Text("Speed", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { subtitleSpeed = (subtitleSpeed - 5f).coerceAtLeast(50f) }, modifier = Modifier.size(32.dp)) { Text("-", color = Color.White, fontSize = 16.sp) }
                        Text("${String.format("%.0f", subtitleSpeed)}%", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(onClick = { subtitleSpeed = (subtitleSpeed + 5f).coerceAtMost(200f) }, modifier = Modifier.size(32.dp)) { Text("+", color = Color.White, fontSize = 16.sp) }
                    }
                    HorizontalDivider(color = Color.DarkGray)
                    Row(modifier = Modifier.fillMaxWidth().clickable { showSubtitlePanel = false; showSettings = true }.padding(vertical = 8.dp)) { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Customization", color = Color.White) }
                } },
                confirmButton = { TextButton(onClick = { showSubtitlePanel = false }) { Text("OK", color = Color(0xFF2196F3)) } },
                containerColor = Color(0xFF1E1E1E))
        }

        // Audio Track dialog (MX Player style)
        if (showAudioDialog) {
            AlertDialog(onDismissRequest = { showAudioDialog = false }, title = { Text("Audio Track", color = Color.White) },
                text = { Column {
                    embeddedAudioTracks.forEach { (label, lang) -> Row(modifier = Modifier.fillMaxWidth().clickable { exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false).setPreferredTextLanguage(lang).build(); selectedAudioLabel = label; showAudioDialog = false }.padding(8.dp)) { RadioButton(selected = selectedAudioLabel == label, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2196F3))); Spacer(modifier = Modifier.width(8.dp)); Text("$label ($lang)", color = Color.White) } }
                    Row(modifier = Modifier.fillMaxWidth().clickable { exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true).build(); selectedAudioLabel = "Disabled"; showAudioDialog = false }.padding(8.dp)) { RadioButton(selected = selectedAudioLabel == "Disabled", onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2196F3))); Spacer(modifier = Modifier.width(8.dp)); Text("Disable", color = Color.White) }
                    HorizontalDivider(color = Color.DarkGray)
                    Text("Synchronization", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { subtitleSync = (subtitleSync - 0.5f).coerceAtLeast(-10f) }, modifier = Modifier.size(32.dp)) { Text("-", color = Color.White, fontSize = 16.sp) }; Text("${String.format("%.1f", subtitleSync)}s", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp)); IconButton(onClick = { subtitleSync = (subtitleSync + 0.5f).coerceAtMost(10f) }, modifier = Modifier.size(32.dp)) { Text("+", color = Color.White, fontSize = 16.sp) } }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Checkbox(checked = true, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2196F3))); Text("AV sync", color = Color.White) }
                } },
                confirmButton = { TextButton(onClick = { showAudioDialog = false }) { Text("OK", color = Color(0xFF2196F3)) } },
                containerColor = Color(0xFF1E1E1E))
        }

        // Shortcut Panel (MX Player style)
        if (showShortcutPanel) {
            AlertDialog(onDismissRequest = { showShortcutPanel = false }, title = { Text("Shortcuts", color = Color.White) },
                text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            listOf("Screen Rotation" to { videoRotation = (videoRotation + 90f) % 360f }, "Background Play" to {}, "Mute" to { exoPlayer.volume = 0f }, "Equalizer" to {}, "Sleep Timer" to {}, "Night Mode" to {}).forEach { (name, action) ->
                                Row(modifier = Modifier.fillMaxWidth().clickable { action() }.padding(vertical = 4.dp)) { Checkbox(checked = false, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2196F3))); Text(name, color = Color.White, fontSize = 12.sp) }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            listOf("Playback Speed" to { showSpeedMenu = true; showShortcutPanel = false }, "Loop" to {}, "Shuffle" to {}, "Audio Effect" to {}, "A-B Repeat" to {}, "Screenshot" to {}).forEach { (name, action) ->
                                Row(modifier = Modifier.fillMaxWidth().clickable { action() }.padding(vertical = 4.dp)) { Checkbox(checked = false, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2196F3))); Text(name, color = Color.White, fontSize = 12.sp) }
                            }
                        }
                    }
                } },
                confirmButton = { TextButton(onClick = { showShortcutPanel = false }) { Text("OK", color = Color(0xFF2196F3)) } },
                containerColor = Color(0xFF1E1E1E))
        }

        // Subtitle Settings (MX Player style)
        if (showSettings) {
            AlertDialog(onDismissRequest = { showSettings = false }, title = { Text("Subtitle Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    // Layout section
                    Text("Layout", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Alignment: Center", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
                    Text("Bottom margins: ${subtitleConfig.position}", color = Color.White, fontSize = 13.sp)
                    Slider(value = subtitleConfig.position, valueRange = 0.1f..0.95f, onValueChange = { subtitleConfig = subtitleConfig.copy(position = it) }, colors = SliderDefaults.colors(thumbColor = Color(0xFF2196F3), activeTrackColor = Color(0xFF2196F3)))
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = false, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2196F3))); Text("Background", color = Color.White, fontSize = 13.sp) }
                    HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
                    // Text section
                    Text("Text", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Font: ${subtitleConfig.fontPath.split("/").lastOrNull() ?: "Default"}", color = Color.White, fontSize = 13.sp, modifier = Modifier.clickable { showFontPicker = true; showSettings = false }.padding(vertical = 4.dp))
                    Text("Size: ${subtitleConfig.fontScale.toInt()}", color = Color.White, fontSize = 13.sp)
                    Slider(value = subtitleConfig.fontScale, valueRange = 50f..200f, onValueChange = { subtitleConfig = subtitleConfig.copy(fontScale = it) }, colors = SliderDefaults.colors(thumbColor = Color(0xFF2196F3), activeTrackColor = Color(0xFF2196F3)))
                    Text("Scale: ${subtitleConfig.fontScale.toInt()}%", color = Color.White, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Color", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f)); Checkbox(checked = subtitleConfig.isBold, onCheckedChange = { subtitleConfig = subtitleConfig.copy(isBold = it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2196F3))); Text("Bold", color = Color.White, fontSize = 12.sp) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        listOf(Color.White, Color(0xFFDAA520), Color.Yellow, Color.Cyan, Color(0xFF00FF00)).forEach { c -> Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(c).clickable { subtitleConfig = subtitleConfig.copy(fontColor = c) }) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = subtitleConfig.borderEnabled, onCheckedChange = { subtitleConfig = subtitleConfig.copy(borderEnabled = it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2196F3))); Text("Border", color = Color.White, fontSize = 13.sp) }
                    Slider(value = subtitleConfig.borderWidth, valueRange = 50f..500f, onValueChange = { subtitleConfig = subtitleConfig.copy(borderWidth = it) }, colors = SliderDefaults.colors(thumbColor = Color(0xFF2196F3), activeTrackColor = Color(0xFF2196F3)))
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = subtitleConfig.shadowEnabled, onCheckedChange = { subtitleConfig = subtitleConfig.copy(shadowEnabled = it) }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2196F3))); Text("Shadow", color = Color.White, fontSize = 13.sp) }
                } },
                confirmButton = { TextButton(onClick = { showSettings = false }) { Text("OK", color = Color(0xFF2196F3)) } },
                containerColor = Color(0xFF1E1E1E))
        }

        // Info dialog
        if (showInfoDialog) {
            AlertDialog(onDismissRequest = { showInfoDialog = false }, title = { Text("Information", color = Color.White) },
                text = { Text(videoInfo, color = Color.White, fontSize = 12.sp) },
                confirmButton = { TextButton(onClick = { showInfoDialog = false }) { Text("OK", color = Color(0xFF2196F3)) } },
                containerColor = Color(0xFF1E1E1E))
        }

        // Font picker
        if (showFontPicker) {
            AlertDialog(onDismissRequest = { showFontPicker = false }, title = { Text("Choose Font", color = Color.White) },
                text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (availableFonts.isEmpty()) Text("No fonts found.", color = Color.Gray) else availableFonts.forEach { f -> Row(modifier = Modifier.fillMaxWidth().clickable { subtitleConfig = subtitleConfig.copy(fontPath = f.absolutePath); showFontPicker = false }.padding(8.dp)) { Text(f.name, color = Color.White) } }
                } },
                confirmButton = { TextButton(onClick = { showFontPicker = false }) { Text("Cancel", color = Color(0xFF2196F3)) } },
                containerColor = Color(0xFF1E1E1E))
        }

        // Track dialog
        if (showTrackDialog) {
            AlertDialog(onDismissRequest = { showTrackDialog = false }, title = { Text("Subtitle Track", color = Color.White) },
                text = { Column {
                    Row(modifier = Modifier.fillMaxWidth().clickable { exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build(); selectedTrackLabel = "Off"; subtitleEnabled = false; showTrackDialog = false }.padding(8.dp)) { RadioButton(selected = selectedTrackLabel == "Off", onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2196F3))); Spacer(modifier = Modifier.width(8.dp)); Text("Off", color = Color.White) }
                    embeddedTracks.forEach { (label, lang) -> Row(modifier = Modifier.fillMaxWidth().clickable { exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).setPreferredTextLanguage(lang).build(); selectedTrackLabel = label; subtitleEnabled = true; showTrackDialog = false }.padding(8.dp)) { RadioButton(selected = selectedTrackLabel == label, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2196F3))); Spacer(modifier = Modifier.width(8.dp)); Text("$label ($lang)", color = Color.White) } }
                    if (embeddedTracks.isEmpty()) Text("No embedded subtitles", color = Color.Gray, fontSize = 12.sp)
                } },
                confirmButton = { TextButton(onClick = { showTrackDialog = false }) { Text("Cancel", color = Color(0xFF2196F3)) } },
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
