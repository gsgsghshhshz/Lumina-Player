package com.example.luminaplayer

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

data class SubtitleEntry(val startTimeMs: Long, val endTimeMs: Long, val text: String)

object SubtitleParser {

    fun parseSrt(context: Context, uri: Uri): List<SubtitleEntry> {
        val entries = mutableListOf<SubtitleEntry>()
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                val content = reader.readText().trim()
                val blocks = content.split(Regex("\n\n+"))
                for (block in blocks) {
                    val lines = block.trim().split("\n")
                    if (lines.size < 3) continue
                    val timeLine = lines[1]
                    val match = Regex("(\\d{2}):(\\d{2}):(\\d{2})[,.](\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2})[,.](\\d{3})").find(timeLine)
                    if (match != null) {
                        val g = match.groupValues
                        val start = g[1].toLong() * 3600000 + g[2].toLong() * 60000 + g[3].toLong() * 1000 + g[4].toLong()
                        val end = g[5].toLong() * 3600000 + g[6].toLong() * 60000 + g[7].toLong() * 1000 + g[8].toLong()
                        val text = lines.drop(2).joinToString("\n").replace(Regex("<[^>]+>"), "")
                        entries.add(SubtitleEntry(start, end, text))
                    }
                }
            }
        } catch (_: Exception) {}
        return entries
    }

    fun parseAss(context: Context, uri: Uri): List<SubtitleEntry> {
        val entries = mutableListOf<SubtitleEntry>()
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = BufferedReader(InputStreamReader(stream)).readLines()
                var inEvents = false
                for (line in lines) {
                    if (line.startsWith("[Events]")) { inEvents = true; continue }
                    if (line.startsWith("[") && inEvents) break
                    if (inEvents && line.startsWith("Dialogue:")) {
                        val parts = line.substringAfter("Dialogue:").split(",", 9)
                        if (parts.size >= 10) {
                            val start = parseAssTime(parts[1].trim())
                            val end = parseAssTime(parts[2].trim())
                            val text = parts[9].trim().replace("\\N", "\n").replace(Regex("\\{[^}]*\\}"), "").replace(Regex("<[^>]+>"), "")
                            entries.add(SubtitleEntry(start, end, text))
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return entries
    }

    private fun parseAssTime(time: String): Long {
        val match = Regex("(\\d):(\\d{2}):(\\d{2})[.](\\d{2})").find(time) ?: return 0
        val g = match.groupValues
        return g[1].toLong() * 3600000 + g[2].toLong() * 60000 + g[3].toLong() * 1000 + g[4].toLong() * 10
    }

    fun detectAndParse(context: Context, uri: Uri): List<SubtitleEntry> {
        val name = uri.lastPathSegment?.lowercase() ?: ""
        return when {
            name.endsWith(".ass") || name.endsWith(".ssa") -> parseAss(context, uri)
            else -> parseSrt(context, uri)
        }
    }
}
