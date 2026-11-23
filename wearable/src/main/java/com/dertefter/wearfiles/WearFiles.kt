package com.dertefter.wearfiles

import android.app.Application
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WearFiles : Application() {
    override fun onCreate() {
        super.onCreate()
        Wearable.getCapabilityClient(this).addLocalCapability("wear_files_app")
    }
}
