package com.example.luminaplayer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SubtitleOverlay(
    entries: List<SubtitleEntry>,
    currentPositionMs: Long,
    config: SubtitleConfig,
    customFontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    modifier: Modifier = Modifier
) {
    val currentEntry = entries.find { entry ->
        currentPositionMs in entry.startTimeMs..entry.endTimeMs
    }

    if (currentEntry != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = (40 * config.position).dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Border/Outline text
            if (config.borderEnabled) {
                Text(
                    text = currentEntry.text,
                    color = config.borderColor,
                    fontSize = (config.fontSize * config.fontScale / 100).sp,
                    fontWeight = if (config.isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (config.isItalic) FontStyle.Italic else FontStyle.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontFamily = customFontFamily,
                    style = TextStyle(
                        drawStyle = Stroke(
                            width = config.borderWidth / 80,
                            join = StrokeJoin.Round
                        )
                    )
                )
            }

            // Main text
            Text(
                text = currentEntry.text,
                color = config.fontColor,
                fontSize = (config.fontSize * config.fontScale / 100).sp,
                fontWeight = if (config.isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (config.isItalic) FontStyle.Italic else FontStyle.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
                fontFamily = customFontFamily,
                style = TextStyle(
                    shadow = if (config.shadowEnabled) {
                        Shadow(
                            color = Color.Black.copy(alpha = 0.8f),
                            blurRadius = if (config.shadowFadeOut) 8f else 4f
                        )
                    } else null
                )
            )
        }
    }
}
