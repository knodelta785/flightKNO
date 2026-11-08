package com.kno.flightinfo

import android.app.Application
import com.kno.flightinfo.notify.Notifier

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannels(this)
    }
}
