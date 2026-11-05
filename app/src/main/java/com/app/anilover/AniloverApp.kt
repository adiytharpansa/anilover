package com.app.anilover

import android.app.Application

class AniloverApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Str.init(this)
    }
}
