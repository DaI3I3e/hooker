package com.example

import android.app.Application
import com.example.di.AppModule

class FinTrackApp : Application() {
    lateinit var appModule: AppModule
        private set

    override fun onCreate() {
        super.onCreate()
        appModule = AppModule(this)
        // Trigger database creation and seed data loading
        appModule.database.openHelper.writableDatabase
    }
}
