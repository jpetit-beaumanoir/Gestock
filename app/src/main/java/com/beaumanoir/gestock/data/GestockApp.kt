package com.beaumanoir.gestock.data

import android.app.Application

class GestockApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: GestockApp
            private set
    }
}