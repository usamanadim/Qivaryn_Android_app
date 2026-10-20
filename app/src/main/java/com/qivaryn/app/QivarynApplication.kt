package com.qivaryn.app

import android.app.Application
import com.chaquo.python.Python

class QivarynApplication : Application() {
    companion object {
        lateinit var instance: QivarynApplication
        fun context() = instance.applicationContext
    }
    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            if (!Python.isStarted()) {
                Python.start(com.chaquo.python.android.AndroidPlatform(this))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}