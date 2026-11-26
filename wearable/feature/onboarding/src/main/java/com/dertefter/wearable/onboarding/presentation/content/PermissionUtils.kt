package com.dertefter.wearable.onboarding.presentation.content

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri

fun Context.openAppAllFilesAccessPermission(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && try {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = "package:$packageName".toUri()
        }
        startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}

fun Context.openAllFilesAccessPermission(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && try {
        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}

fun Context.openApplicationDetailsSettings(): Boolean {
    return try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:$packageName".toUri()
        }
        startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}

fun Context.openStoragePermissionSettings(onError: () -> Unit = {}) {
    if (openAllFilesAccessPermission()) return
    if (openAppAllFilesAccessPermission()) return
    if (openApplicationDetailsSettings()) return
    if (openApplicationDetailsSettings()) return

    onError()
}
