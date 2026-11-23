package com.dertefter.wearfiles.data

import android.Manifest
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.dertefter.wearable.data.repository.FileManagerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class FileManagerRepositoryImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context
) : FileManagerRepository {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    private val tickerFlow = flow {
        while (true) {
            emit(Unit)
            delay(2000.milliseconds)
        }
    }

    override fun getFiles(path: String): Flow<Result<List<File>>> = combine(
        refreshTrigger.onStart { emit(Unit) },
        tickerFlow
    ) { _, _ ->
        runCatching {
            val file = File(path)
            if (!file.exists()) throw IllegalArgumentException("Path does not exist: $path")
            if (!file.isDirectory) throw IllegalArgumentException("Path is not a directory: $path")

            file.listFiles()?.toList()
                ?: throw IllegalStateException("Cannot list files for: $path")
        }
    }.flowOn(Dispatchers.IO)

    override fun getFileName(path: String): Result<String> {
        return runCatching {
            val file = File(path)
            file.name
        }
    }

    override suspend fun rename(path: String, newName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(path)

            if (!file.exists()) throw IllegalArgumentException("File does not exist: $path")
            if (newName.isBlank()) throw IllegalArgumentException("New name cannot be empty")

            val newFile = File(file.parent, newName)

            val success = file.renameTo(newFile)
            if (success) {
                MediaScannerConnection.scanFile(context, arrayOf(path, newFile.absolutePath), null, null)
                refreshTrigger.emit(Unit)
            }
            success
        }
    }


    override suspend fun getParentFilePath(path: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            file.parent
        } catch (_: Exception) {
            null
        }
    }

    override fun hasFileAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val readStorage = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val writeStorage = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            readStorage && writeStorage
        }
    }

    override fun hasImagesAccess(): Boolean {
        if (hasFileAccess()) {
            return true
        }
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    val readImages = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    return readImages
                }

                else -> {
                    return ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            }
        } catch (_: Exception) {
            return false
        }
    }

    override fun hasVideosAccess(): Boolean {
        if (hasFileAccess()) {
            return true
        }
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    val readVideos = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_VIDEO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    return readVideos
                }

                else -> {
                    return ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            }
        } catch (_: Exception) {
            return false
        }
    }

    override fun hasAudioAccess(): Boolean {
        if (hasFileAccess()) {
            return true
        }
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    val readAudio = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    return readAudio
                }

                else -> {
                    return ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            }
        } catch (_: Exception) {
            return false
        }
    }

    override fun getFileByPath(path: String): File? {
        return try{
            File(path)
        }catch (_: Exception){
            null
        }
    }

    override suspend fun delete(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {

            val file = File(path)

            if (!file.exists()) throw IllegalArgumentException("File does not exist: ${file.path}")

            val success = if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            if (success) {
                MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
                refreshTrigger.emit(Unit)
            }
            success
        }
    }

    override suspend fun delete(paths: List<String>): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            var anyDeleted = false
            var allSucceeded = true
            for (path in paths) {
                delete(path).onSuccess {
                    anyDeleted = true
                }.onFailure {
                    allSucceeded = false
                }
            }
            if (anyDeleted) refreshTrigger.emit(Unit)
            allSucceeded
        }
    }

    override suspend fun createDirectory(path: String, name: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val newDir = File(path, name)

            if (newDir.exists()) throw IllegalStateException("Directory already exists: ${newDir.path}")

            val success = newDir.mkdirs()
            if (success) refreshTrigger.emit(Unit)
            success
        }
    }

    override fun getBasePath(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }




    override fun canNavigateUpFrom(path: String): Boolean {
        val homePath = getBasePath()
        return path.startsWith(homePath) && path != homePath
    }

    override suspend fun canBeRenamed(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)

            if (!file.exists()) return@withContext false

            val parent = file.parentFile ?: return@withContext false

            if (!parent.canWrite()) return@withContext false

            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun canBeRenamed(paths: List<String>): Boolean {
        return paths.all { canBeRenamed(it) }
    }

    override suspend fun canBeDeleted(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)

            if (!file.exists()) return@withContext false

            val parent = file.parentFile ?: return@withContext false
            if (!parent.canWrite()) return@withContext false

            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun canBeDeleted(paths: List<String>): Boolean {
        return paths.all { canBeDeleted(it) }
    }

    override suspend fun canCreateDirHere(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(path)

            if (!dir.exists()) return@withContext false
            if (!dir.isDirectory) return@withContext false
            if (!dir.canWrite()) return@withContext false

            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun isDirectory(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            file.isDirectory
        } catch (_: Exception) {
            false
        }
    }


}
