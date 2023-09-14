package com.dertefter.wearfiles

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast


class PermissionManager {
    fun checkFilesPermissions(activity: Activity): Boolean {
        return activity.checkPermission("android.permission.READ_EXTERNAL_STORAGE", android.os.Process.myPid(), android.os.Process.myUid()) == 0 && activity.checkPermission("android.permission.WRITE_EXTERNAL_STORAGE", android.os.Process.myPid(), android.os.Process.myUid()) == PackageManager.PERMISSION_GRANTED
    }

    fun requestFilesPermissions(activity: Activity) {
        try{
            activity.requestPermissions(arrayOf("android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"), 1)
        } catch (e: Exception) {
            Log.e("PermissionManager", e.stackTraceToString())
            Toast.makeText(activity, "Error requesting permissions", Toast.LENGTH_LONG).show()
        }

    }
}