package com.github.pedrovgs.deeppanel.app

import android.app.Application
import com.github.pedrovgs.deeppanel.DeepPanel

class DeepPanelApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DeepPanel.initialize(this)
    }
}
