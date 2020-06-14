package com.github.pedrovgs.deeppanel

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.tensorflow.lite.Interpreter

class DeepPanel {
    private lateinit var interpreter: Interpreter

    fun initialize(context: Context) {
        val model = loadModel(context)
        interpreter = Interpreter(model)
    }

    private fun loadModel(context: Context): ByteBuffer {
        val inputStream = context.resources.openRawResource(R.raw.model)
        val rawModelBytes = inputStream.readBytes()
        val byteBuffer = ByteBuffer.allocateDirect(rawModelBytes.size)
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.put(rawModelBytes)
        return byteBuffer
    }
}
