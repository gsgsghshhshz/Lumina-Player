package com.example.luminaplayer

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView

@Composable
fun LuminaPlayerScreen(
    viewModel: PlayerViewModel = viewModel(),
    mediaUri: Uri
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(mediaUri) {
        if (mediaUri != Uri.EMPTY) {
            val mediaItem = MediaItem.fromUri(mediaUri)
            viewModel.player?.setMediaItem(mediaItem)
            viewModel.player?.prepare()
            viewModel.player?.playWhenReady = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = viewModel.player
            },
            modifier = Modifier.fillMaxSize()
        )

        GestureOverlay(
            onVerticalLeft = { /* Brightness Control */ },
            onVerticalRight = { /* Volume Control */ },
            onHorizontalSeek = { deltaMs -> viewModel.onSeekDelta(deltaMs) },
            onSeekStart = viewModel::onSeekStart,
            onSeekEnd = viewModel::onSeekEnd,
            onDoubleTapLeft = { viewModel.player?.seekBack() },
            onDoubleTapRight = { viewModel.player?.seekForward() },
            onTap = { viewModel.togglePlayPause() }
        )
    }
}

@Composable
private fun GestureOverlay(
    onVerticalLeft: (Float) -> Unit,
    onVerticalRight: (Float) -> Unit,
    onHorizontalSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onTap: () -> Unit
) {
    var isSeeking by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2) onDoubleTapLeft() else onDoubleTapRight()
                    }
)
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val isLeftZone = offset.x < size.width / 3f
                        val isRightZone = offset.x > size.width * 2f / 3f
                        if (!isLeftZone && !isRightZone) {
                            isSeeking = true
                            onSeekStart()
                        }
                    },
                    onDragEnd = {
                        if (isSeeking) {
                            isSeeking = false
                            onSeekEnd()
                        }
                    },
                    onDragCancel = {
                        if (isSeeking) {
                            isSeeking = false
                            onSeekEnd()
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val isLeftZone = change.position.x < size.width / 3f
                        val isRightZone = change.position.x > size.width * 2f / 3f
                        
                        if (isSeeking) {
                            onHorizontalSeek((dragAmount.x * 50).toLong())
                        } else if (isLeftZone) {
                            onVerticalLeft(-dragAmount.y / size.height)
                        } else if (isRightZone) {
                            onVerticalRight(-dragAmount.y / size.height)
                        }
                    }
                )
            }
    )
}
