package com.dertefter.wearfiles.common

import android.webkit.MimeTypeMap
import com.dertefter.wearfiles.R
import java.io.File

object Utils {

    var clipboardIsCut = false
    var clipboard: File? = null

    fun getFileIconResId(file: File): Int {

        if (file.isDirectory){
            return R.drawable.folder
        }

        val extension = file.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

        return when {
            // Текстовые файлы
            mimeType?.startsWith("text/") == true -> R.drawable.icon_doc

            // Документы
            mimeType == "application/pdf" -> R.drawable.pdf
            mimeType?.startsWith("application/msword") == true ||
                    mimeType?.startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml") == true -> R.drawable.icon_doc
            mimeType?.startsWith("application/vnd.ms-excel") == true ||
                    mimeType?.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml") == true -> R.drawable.icon_table
            mimeType?.startsWith("application/vnd.ms-powerpoint") == true ||
                    mimeType?.startsWith("application/vnd.openxmlformats-officedocument.presentationml") == true -> R.drawable.icon_presentation

            // Архивы
            mimeType?.startsWith("application/zip") == true ||
                    mimeType?.startsWith("application/x-7z-compressed") == true ||
                    mimeType?.startsWith("application/x-rar-compressed") == true ||
                    mimeType?.startsWith("application/gzip") == true -> R.drawable.icon_zip

            // Изображения
            mimeType?.startsWith("image/") == true -> R.drawable.image

            // Видео
            mimeType?.startsWith("video/") == true -> R.drawable.video

            // Аудио
            mimeType?.startsWith("audio/") == true -> R.drawable.music

            // Код / скрипты
            extension in listOf("c", "cpp", "h", "hpp", "java", "kt", "kts", "py", "js", "ts", "html", "css", "php", "sh", "bat") ->
                R.drawable.icon_code

            // Исполняемые файлы
            extension in listOf("exe", "apk", "appimage", "deb") -> R.drawable.icon_zip

            // Файлы конфигурации
            extension in listOf("ini", "cfg", "conf", "xml", "json", "yaml", "yml", "toml") -> R.drawable.icon_doc

            // Остальные
            else -> R.drawable.draft // Иконка по умолчанию
        }
    }
}