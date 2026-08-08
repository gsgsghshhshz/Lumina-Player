package com.example.luminaplayer

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File

object FontScanner {

    fun scanFonts(context: Context): List<File> {
        val fonts = mutableListOf<File>()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ - فقط پوشه‌های امن
                val dirs = listOf(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                )
                for (dir in dirs) {
                    dir?.let { findTtfFiles(it, fonts) }
                }
            } else {
                // Android قدیمی‌تر - پوشه‌های استاندارد
                val dirs = listOf(
                    File("/storage/emulated/0/Fonts"),
                    File("/storage/emulated/0/Download"),
                    context.getExternalFilesDir(null),
                )
                for (dir in dirs) {
                    dir?.let { findTtfFiles(it, fonts) }
                }
            }
        } catch (e: Exception) {
            // اگه ارور داد، لیست خالی برگردان
        }

        return fonts.distinctBy { it.name }.sortedBy { it.name }.take(30)
    }

    private fun findTtfFiles(dir: File, result: MutableList<File>) {
        try {
            if (!dir.exists() || !dir.canRead()) return
            val files = dir.listFiles() ?: return
            for (file in files) {
                when {
                    file.isFile && file.extension.lowercase() == "ttf" -> result.add(file)
                    file.isDirectory && !file.name.startsWith(".") && result.size < 30 -> {
                        findTtfFiles(file, result)
                    }
                }
            }
        } catch (e: Exception) {
            // دسترسی رد شده
        }
    }

    fun loadFont(path: String): FontFamily? {
        return try {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                FontFamily(Font(file))
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
