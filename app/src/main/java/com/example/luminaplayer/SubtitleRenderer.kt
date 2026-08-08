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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SubtitleSize(val scale: Float) {
    LOW(0.6f),
    MEDIUM(1.0f),
    HIGH(1.5f)
}

@Composable
fun SubtitleOverlay(
    entries: List<SubtitleEntry>,
    currentPositionMs: Long,
    config: SubtitleConfig,
    customFontFamily: FontFamily? = null,
    modifier: Modifier = Modifier
) {
    val currentEntry = entries.find { entry ->
        currentPositionMs in entry.startTimeMs..entry.endTimeMs
    }

    val density = LocalDensity.current
    val screenHeightPx = with(density) { 600.dp.toPx() }
    val baseFontSize = when {
        screenHeightPx > 2400 -> 20f
        screenHeightPx > 1800 -> 16f
        screenHeightPx > 1200 -> 14f
        else -> 12f
    }

    val finalFontSize = baseFontSize * config.subtitleSize.scale * (config.fontScale / 100f)

    if (currentEntry != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = (50 * config.position).dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (config.borderEnabled) {
                Text(
                    text = currentEntry.text,
                    color = Color.Black,
                    fontSize = finalFontSize.sp,
                    fontWeight = if (config.isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (config.isItalic) FontStyle.Italic else FontStyle.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontFamily = customFontFamily,
                    style = TextStyle(
                        drawStyle = Stroke(
                            width = config.borderWidth / 100,
                            join = StrokeJoin.Round
                        )
                    )
                )
            }

            Text(
                text = currentEntry.text,
                color = config.fontColor,
                fontSize = finalFontSize.sp,
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
