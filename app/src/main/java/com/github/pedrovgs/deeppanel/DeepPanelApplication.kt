package com.github.pedrovgs.deeppanel

import android.app.Application

class DeepPanelApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DeepPanel.initialize(this)
    }
}
