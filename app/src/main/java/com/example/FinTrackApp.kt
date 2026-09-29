package com.example

import android.app.Application
import com.example.data.local.SeedData
import com.example.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FinTrackApp : Application() {
    lateinit var appModule: AppModule
        private set

    override fun onCreate() {
        super.onCreate()
        appModule = AppModule(this)

        // Seed default categories and account on first launch if database is empty
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val existingCategories = appModule.database.categoryDao().getAll().firstOrNull()
                if (existingCategories.isNullOrEmpty()) {
                    SeedData.insertDefaults(
                        accountDao = appModule.database.accountDao(),
                        categoryDao = appModule.database.categoryDao()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
