package com.iliyateam.aseman

import android.app.Application
import android.content.Context

class AsemanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
    }
    companion object {
        var appContext: Context? = null
    }
}