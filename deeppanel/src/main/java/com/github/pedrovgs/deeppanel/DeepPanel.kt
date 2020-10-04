package com.github.pedrovgs.deeppanel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import org.tensorflow.lite.Interpreter

class DeepPanel {
    companion object {
        private const val modelInputImageSize = 224
    }

    private val ccl = NativeDeepPanel()
    private lateinit var interpreter: Interpreter

    fun initialize(context: Context) {
        val model = loadModel(context)
        interpreter = Interpreter(model)
        ccl.initialize()
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
            logExecutionTime("C++ code") { ccl.extractPanelsInfo(prediction[0], scale, bitmap.width, bitmap.height) }
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

    private fun generatePanelsBitmap(sourceBitmap: Bitmap, panels: Panels): Bitmap {
        val tempBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(tempBitmap)
        panels.panelsInfo.forEach { panel ->
            val paint = Paint()
            paint.style = Paint.Style.FILL_AND_STROKE
            paint.isAntiAlias = true
            paint.isFilterBitmap = true
            paint.isDither = true
            paint.color = colorForLabel(panel.panelNumberInPage + 2)
            paint.alpha = 120
            canvas.drawRect(
                panel.left.toFloat(),
                panel.top.toFloat(),
                panel.right.toFloat(),
                panel.bottom.toFloat(),
                paint
            )
        }
        return tempBitmap
    }

    private fun resizeInput(bitmapToResize: Bitmap): Bitmap {
        val reqWidth = modelInputImageSize.toFloat()
        val reqHeight = modelInputImageSize.toFloat()
        val matrix = Matrix()
        matrix.setRectToRect(
            RectF(0f, 0f, bitmapToResize.width.toFloat(), bitmapToResize.height.toFloat()),
            RectF(0f, 0f, reqWidth, reqHeight),
            Matrix.ScaleToFit.CENTER
        )
        val blackBackgroundBitmap =
            Bitmap.createBitmap(reqWidth.toInt(), reqHeight.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(blackBackgroundBitmap)
        val paint = Paint()
        paint.color = Color.BLACK
        canvas.drawRect(0f, 0f, reqWidth, reqHeight, paint)
        canvas.drawBitmap(bitmapToResize, matrix, Paint())
        return blackBackgroundBitmap
    }

    private fun loadModel(context: Context): ByteBuffer {
        val inputStream = context.resources.openRawResource(R.raw.model)
        val rawModelBytes = inputStream.readBytes()
        val byteBuffer = ByteBuffer.allocateDirect(rawModelBytes.size)
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.put(rawModelBytes)
        return byteBuffer
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val floatTypeSizeInBytes = 4
        val numberOfChannels = 3
        val modelInputSize =
            floatTypeSizeInBytes * modelInputImageSize * modelInputImageSize * numberOfChannels
        val imgData = ByteBuffer.allocateDirect(modelInputSize)
        imgData.order(ByteOrder.nativeOrder())
        val pixels = IntArray(modelInputImageSize * modelInputImageSize)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in pixels.indices) {
            val pixelInfo: Int = pixels[i]
            val normalizedRedChannel = Color.red(pixelInfo) / 255f
            imgData.putFloat(normalizedRedChannel)
            val normalizedGreenChannel = Color.green(pixelInfo) / 255f
            imgData.putFloat(normalizedGreenChannel)
            val normalizedBlueChannel = Color.blue(pixelInfo) / 255f
            imgData.putFloat(normalizedBlueChannel)
        }
        return imgData
    }

    private fun createBitmapFromPrediction(prediction: Prediction): Bitmap {
        val imageSize = 224
        val bitmap = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888)
        for (x in 0 until imageSize) {
            for (y in 0 until imageSize) {
                val color = colorForLabel(prediction[x][y])
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }

    private fun colorForLabel(
        label: Int
    ): Int = when (label) {
        -1 -> Color.BLACK
        0 -> Color.BLUE
        1 -> Color.RED
        2 -> Color.GREEN
        3 -> Color.CYAN
        4 -> Color.YELLOW
        5 -> Color.MAGENTA
        6 -> Color.parseColor("#4c004f")
        7 -> Color.parseColor("#084518")
        8 -> Color.parseColor("#288b8f")
        9 -> Color.parseColor("#8f7928")
        10 -> Color.parseColor("#d993d4")
        11 -> Color.parseColor("#541b14")
        12 -> Color.parseColor("#a3560d")
        else -> Color.WHITE
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

    private fun computeResizeScale(bitmap: Bitmap): Float {
        val originalSize = max(bitmap.width, bitmap.height)
        return originalSize / modelInputImageSize.toFloat()
    }
}

private fun <T> logExecutionTime(name: String, lambda: () -> T): T {
    val init = System.currentTimeMillis()
    val result = lambda()
    val now = System.currentTimeMillis()
    val time = now - init
    Log.d("DeepPanel", "Time needed for $name = $time ms")
    return result
}