package com.dertefter.wearfiles.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.dertefter.wearable.data.repository.FileInteractionHandler
import com.dertefter.wearable.design.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class FileInteractionHandlerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : FileInteractionHandler {

    override fun openFileByPath(path: String) {

        val file = File(path)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val mimeType = context.contentResolver.getType(uri) ?: "*/*"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.error_no_app), Toast.LENGTH_SHORT).show()
        }
    }

    override fun openFileByUri(uri: Uri) {
        val mime = context.contentResolver.getType(uri) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
    }
}
