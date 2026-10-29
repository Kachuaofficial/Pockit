package com.invatech.pockit

import android.app.Application
import com.invatech.pockit.data.local.database.PockitDatabase

class PockitApplication : Application() {

    val database: PockitDatabase by lazy {
        PockitDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.invatech.pockit.ui.theme.ThemeManager.init(this)
    }

    companion object {
        lateinit var instance: PockitApplication
            private set
    }
}
