package com.example.luminaplayer

import android.os.Build
import android.os.Environment
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File

object FontScanner {
    fun scanFonts(context: android.content.Context): List<File> {
        val fonts = mutableListOf<File>()
        try {
            val dirs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOfNotNull(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS))
            } else {
                listOfNotNull(File("/storage/emulated/0/Fonts"), File("/storage/emulated/0/Download"), context.getExternalFilesDir(null))
            }
            for (dir in dirs) { if (dir.exists() && dir.canRead()) findTtf(dir, fonts) }
        } catch (_: Exception) {}
        return fonts.distinctBy { it.name }.sortedBy { it.name }.take(30)
    }
    private fun findTtf(dir: File, result: MutableList<File>) {
        try { val files = dir.listFiles() ?: return; for (f in files) { when { f.isFile && f.extension.lowercase() == "ttf" -> result.add(f); f.isDirectory && !f.name.startsWith(".") && result.size < 30 -> findTtf(f, result) } } } catch (_: Exception) {}
    }
    fun loadFont(path: String): FontFamily? = try { val f = File(path); if (f.exists() && f.canRead()) FontFamily(Font(f)) else null } catch (_: Exception) { null }
}
