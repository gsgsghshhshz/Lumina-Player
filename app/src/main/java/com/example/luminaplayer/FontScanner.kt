package com.example.luminaplayer

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File

object FontScanner {

    fun scanFonts(context: Context): List<File> {
        val fonts = mutableListOf<File>()

        // اسکن پوشه‌های استاندارد فونت
        val searchPaths = listOf(
            "/storage/emulated/0/Fonts",
            "/storage/emulated/0/Download",
            "/storage/emulated/0/Documents",
            "/storage/emulated/0",
        )

        for (path in searchPaths) {
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) {
                findTtfFiles(dir, fonts)
            }
        }

        return fonts.distinctBy { it.name }.sortedBy { it.name }
    }

    private fun findTtfFiles(dir: File, result: MutableList<File>) {
        try {
            val files = dir.listFiles() ?: return
            for (file in files) {
                when {
                    file.isFile && file.extension.lowercase() == "ttf" -> result.add(file)
                    file.isDirectory && !file.name.startsWith(".") -> {
                        if (result.size < 50) { // محدودیت برای جلوگیری از اسکن بیش از حد
                            findTtfFiles(file, result)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // دسترسی رد شده
        }
    }

    fun loadFont(context: Context, path: String): FontFamily? {
        return try {
            val file = File(path)
            if (file.exists()) {
                Font(file)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
