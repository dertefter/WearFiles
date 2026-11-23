package com.dertefter.wearfiles.data

import android.content.Context
import android.media.MediaScannerConnection
import com.dertefter.wearable.data.repository.ClipboardOperation
import com.dertefter.wearable.data.repository.ClipboardRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ClipboardRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ClipboardRepository {

    private var clipboard: List<String>? = null

    override var operation: ClipboardOperation? = null

    override fun cut(paths: List<String>) {
        operation = ClipboardOperation.CUT
        clipboard = paths
    }

    override fun copy(paths: List<String>) {
        operation = ClipboardOperation.COPY
        clipboard = paths
    }

    override fun cancel() {
        operation = null
        clipboard = null
    }

    override suspend fun insertTo(path: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val srcPaths = clipboard ?: return@withContext false
                val destDir = File(path)

                if (!destDir.exists() || !destDir.isDirectory) return@withContext false

                var allSucceeded = true

                for (srcPath in srcPaths) {
                    val srcFile = File(srcPath)
                    if (!srcFile.exists()) {
                        allSucceeded = false
                        continue
                    }

                    val target = findUniqueFile(destDir, srcFile.name, srcFile.isDirectory)

                    when (operation) {
                        ClipboardOperation.COPY -> {
                            if (srcFile.copyRecursively(target, overwrite = false)) {
                                MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), null, null)
                            } else {
                                allSucceeded = false
                            }
                        }
                        ClipboardOperation.CUT -> {
                            val moved = srcFile.renameTo(target) || srcFile.copyRecursively(target, overwrite = false) && srcFile.deleteRecursively()
                            if (moved) {
                                MediaScannerConnection.scanFile(context, arrayOf(srcPath, target.absolutePath), null, null)
                            } else {
                                allSucceeded = false
                            }
                        }
                        else -> {
                            allSucceeded = false
                        }
                    }
                }

                clipboard = null
                operation = null

                allSucceeded
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun findUniqueFile(destDir: File, name: String, isDirectory: Boolean): File {
        var newFile = File(destDir, name)
        if (!newFile.exists()) {
            return newFile
        }

        val baseName: String
        val extension: String

        if (isDirectory) {
            baseName = name
            extension = ""
        } else {
            val dotIndex = name.lastIndexOf('.')
            if (dotIndex > 0 && dotIndex < name.length - 1) { // file.ext
                baseName = name.substring(0, dotIndex)
                extension = name.substring(dotIndex) // .ext
            } else { // no extension, or .file
                baseName = name
                extension = ""
            }
        }

        var counter = 1
        while (newFile.exists()) {
            val newName = "$baseName ($counter)$extension"
            newFile = File(destDir, newName)
            counter++
        }
        return newFile
    }
}
