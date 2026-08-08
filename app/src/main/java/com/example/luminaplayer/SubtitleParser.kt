package com.example.luminaplayer

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

data class SubtitleEntry(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

object SubtitleParser {

    fun parseSrt(context: Context, uri: Uri): List<SubtitleEntry> {
        val entries = mutableListOf<SubtitleEntry>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                val content = reader.readText()
                val blocks = content.split(Regex("\n\\s*\n"))

                for (block in blocks) {
                    val lines = block.trim().split("\n")
                    if (lines.size >= 3) {
                        val timeLine = lines[1]
                        val timeParts = timeLine.split(" --> ")
                        if (timeParts.size == 2) {
                            val startMs = parseSrtTime(timeParts[0].trim())
                            val endMs = parseSrtTime(timeParts[1].trim())
                            val text = lines.drop(2).joinToString("\n").trim()
                            if (text.isNotEmpty()) {
                                entries.add(SubtitleEntry(startMs, endMs, text))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return entries
    }

    fun parseAss(context: Context, uri: Uri): List<SubtitleEntry> {
        val entries = mutableListOf<SubtitleEntry>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                val lines = reader.readLines()
                var inEvents = false
                var format: List<String> = emptyList()

                for (line in lines) {
                    val trimmed = line.trim()
                    when {
                        trimmed == "[Events]" -> inEvents = true
                        trimmed.startsWith("[") && trimmed.endsWith("]") -> inEvents = false
                        inEvents && trimmed.startsWith("Format:") -> {
                            format = trimmed.removePrefix("Format:").split(",").map { it.trim() }
                        }
                        inEvents && trimmed.startsWith("Dialogue:") -> {
                            val data = trimmed.removePrefix("Dialogue:").split(",", limit = format.size)
                            if (data.size >= format.size) {
                                val startIdx = format.indexOf("Start")
                                val endIdx = format.indexOf("End")
                                val textIdx = format.indexOf("Text")

                                if (startIdx >= 0 && endIdx >= 0 && textIdx >= 0) {
                                    val startMs = parseAssTime(data[startIdx].trim())
                                    val endMs = parseAssTime(data[endIdx].trim())
                                    var text = data[textIdx].trim()
                                    // Remove ASS style tags
                                    text = text.replace(Regex("\\{[^}]*\\}"), "")
                                    text = text.replace(Regex("\\\\N"), "\n")
                                    text = text.replace(Regex("\\\\n"), "\n")
                                    text = text.trim()
                                    if (text.isNotEmpty()) {
                                        entries.add(SubtitleEntry(startMs, endMs, text))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return entries
    }

    fun detectAndParse(context: Context, uri: Uri): List<SubtitleEntry> {
        val fileName = getFileName(context, uri).lowercase()
        return when {
            fileName.endsWith(".srt") -> parseSrt(context, uri)
            fileName.endsWith(".ass") || fileName.endsWith(".ssa") -> parseAss(context, uri)
            else -> parseSrt(context, uri) // Default to SRT
        }
    }

    private fun parseSrtTime(time: String): Long {
        try {
            val parts = time.replace(",", ".").split(":")
            if (parts.size == 3) {
                val h = parts[0].trim().toLong()
                val m = parts[1].trim().toLong()
                val sParts = parts[2].trim().split(".")
                val s = sParts[0].toLong()
                val ms = if (sParts.size > 1) sParts[1].toLong() * (if (sParts[1].length == 1) 100 else if (sParts[1].length == 2) 10 else 1) else 0
                return h * 3600000 + m * 60000 + s * 1000 + ms
            }
        } catch (e: Exception) {}
        return 0L
    }

    private fun parseAssTime(time: String): Long {
        try {
            val parts = time.trim().split(":")
            if (parts.size == 3) {
                val h = parts[0].trim().toLong()
                val m = parts[1].trim().toLong()
                val sParts = parts[2].trim().split(".")
                val s = sParts[0].toLong()
                val ms = if (sParts.size > 1) sParts[1].toLong() * 10 else 0
                return h * 3600000 + m * 60000 + s * 1000 + ms
            }
        } catch (e: Exception) {}
        return 0L
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "subtitle.srt"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    name = cursor.getString(idx) ?: name
                }
            }
        }
        return name
    }
}
