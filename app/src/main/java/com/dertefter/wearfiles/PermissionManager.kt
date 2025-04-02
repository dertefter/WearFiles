package com.dertefter.wearfiles

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.startActivity
import com.dertefter.wearfiles.ui.MainActivity


class PermissionManager {
    fun checkFilesPermissions(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT <= 29){
            return activity.checkPermission("android.permission.READ_EXTERNAL_STORAGE", android.os.Process.myPid(), android.os.Process.myUid()) == PackageManager.PERMISSION_GRANTED && activity.checkPermission("android.permission.WRITE_EXTERNAL_STORAGE", android.os.Process.myPid(), android.os.Process.myUid()) == PackageManager.PERMISSION_GRANTED
        }else{
            return Environment.isExternalStorageManager()
        }
    }

    fun requestFilesPermissions(activity: Activity) {
        try{
            if (SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    activity.startActivity(Intent(activity, MainActivity::class.java))
                } else { //request for the permission
                    val intent: Intent =
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    val uri = Uri.fromParts("package", activity.packageName, null)
                    intent.setData(uri)
                    activity.startActivity(intent)
                }
            } else {
                //below android 11=======
                activity.startActivity(Intent(activity, MainActivity::class.java))
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf<String>(WRITE_EXTERNAL_STORAGE),
                    0
                )
            }
        } catch (e: Exception) {
            Log.e("PermissionManager", e.stackTraceToString())
            Toast.makeText(activity, "Error requesting permissions", Toast.LENGTH_LONG).show()
        }

    }
}