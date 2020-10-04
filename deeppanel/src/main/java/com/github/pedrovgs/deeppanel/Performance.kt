package com.github.pedrovgs.deeppanel

import android.util.Log

object Performance {

    fun <T> logExecutionTime(name: String, lambda: () -> T): T {
        val init = System.currentTimeMillis()
        val result = lambda()
        val now = System.currentTimeMillis()
        val time = now - init
        Log.d("DeepPanel", "Time needed for $name = $time ms")
        return result
    }
}