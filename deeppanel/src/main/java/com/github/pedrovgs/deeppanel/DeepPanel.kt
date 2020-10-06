package com.github.pedrovgs.deeppanel

import android.content.Context
import android.graphics.Bitmap
import com.github.pedrovgs.deeppanel.Bitmaps.computeResizeScale
import com.github.pedrovgs.deeppanel.Bitmaps.convertBitmapToByteBuffer
import com.github.pedrovgs.deeppanel.Bitmaps.createBitmapFromPrediction
import com.github.pedrovgs.deeppanel.Bitmaps.generatePanelsBitmap
import com.github.pedrovgs.deeppanel.Bitmaps.resizeInput
import com.github.pedrovgs.deeppanel.Performance.logExecutionTime
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.tensorflow.lite.Interpreter

class DeepPanel {
    companion object {
        const val modelInputImageSize = 224
        private val ccl = NativeDeepPanel()
        private lateinit var interpreter: Interpreter

        fun initialize(context: Context) {
            val model = loadModel(context)
            interpreter = Interpreter(model)
            ccl.initialize()
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

    fun extractPanelsInfo(bitmap: Bitmap): PredictionResult {
        val resizedImage = resizeInput(bitmap)
        val modelInput = convertBitmapToByteBuffer(resizedImage)
        val prediction =
            Array(1) { Array(modelInputImageSize) { Array(modelInputImageSize) { FloatArray(3) } } }
        interpreter.run(modelInput, prediction)
        val scale = computeResizeScale(bitmap)
        val panelsInfo = ccl.extractPanelsInfo(prediction[0], scale, bitmap.width, bitmap.height)
        val panels = composePanels(panelsInfo)
        return PredictionResult(panelsInfo.connectedAreas, panels)
    }

    fun extractDetailedPanelsInfo(bitmap: Bitmap): DetailedPredictionResult {
        val resizedImage = logExecutionTime("Resize input") { resizeInput(bitmap) }
        val modelInput = logExecutionTime("Resized bitmap to model input") {
            convertBitmapToByteBuffer(resizedImage)
        }
        val prediction =
            Array(1) { Array(modelInputImageSize) { Array(modelInputImageSize) { FloatArray(3) } } }
        logExecutionTime("Evaluate model") { interpreter.run(modelInput, prediction) }
        val scale = computeResizeScale(bitmap)
        val panelsInfo =
            logExecutionTime("C++ code") {
                ccl.extractPanelsInfo(
                    prediction[0],
                    scale,
                    bitmap.width,
                    bitmap.height
                )
            }
        val labeledAreasBitmap =
            logExecutionTime("Bitmap from areas") { createBitmapFromPrediction(panelsInfo.connectedAreas) }
        val panels = logExecutionTime("Extract panels info") { composePanels(panelsInfo) }
        val panelsBitmap =
            logExecutionTime("Bitmap from panels") { generatePanelsBitmap(bitmap, panels) }
        return DetailedPredictionResult(
            bitmap,
            resizedImage,
            labeledAreasBitmap,
            panelsBitmap,
            PredictionResult(panelsInfo.connectedAreas, panels)
        )
    }

    private fun composePanels(panelsInfo: RawPanelsInfo): Panels {
        val panels = panelsInfo.panels.mapIndexed { index, rawInfo ->
            Panel(
                panelNumberInPage = index,
                left = rawInfo.left,
                top = rawInfo.top,
                right = rawInfo.right,
                bottom = rawInfo.bottom
            )
        }
        return Panels(panels)
    }
}
