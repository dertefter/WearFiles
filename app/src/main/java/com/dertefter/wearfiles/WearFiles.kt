package com.dertefter.wearfiles
import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.Log

//application class

class WearFiles : Application() {
    companion object {
        lateinit var fileManagerInstance: FileManager
        lateinit var appContext: Context
    }

    override fun onCreate() {
        super.onCreate()

        WearFiles.appContext = applicationContext
        WearFiles.fileManagerInstance = FileManager()

    }
}
