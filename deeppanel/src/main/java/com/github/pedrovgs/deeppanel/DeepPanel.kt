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
import kotlin.math.min
import org.tensorflow.lite.Interpreter

class DeepPanel {
    companion object {
        private const val inputImageWidth = 224
        private const val inputImageHeight = 224
        private const val borderSize = 3
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
            Array(1) { Array(inputImageWidth) { Array(inputImageHeight) { FloatArray(3) } } }
        interpreter.run(modelInput, prediction)
        val connectedAreas = ccl.extractPanelsInfo(prediction[0])
        //val connectedAreas = findPanels(labeledPrediction)
        val panels = extractPanelsInfo(connectedAreas)
        return PredictionResult(connectedAreas, panels)
    }

    fun extractDetailedPanelsInfo(bitmap: Bitmap): DetailedPredictionResult {
        val resizedImage = logExecutionTime("Resize input") { resizeInput(bitmap) }
        val modelInput = logExecutionTime("Resized bitmap to model input") {
            convertBitmapToByteBuffer(resizedImage)
        }
        val prediction =
            Array(1) { Array(inputImageWidth) { Array(inputImageHeight) { FloatArray(3) } } }
        logExecutionTime("Evaluate model") { interpreter.run(modelInput, prediction) }
        val connectedAreas = logExecutionTime("C++ code") { ccl.extractPanelsInfo(prediction[0]) }
        val labeledAreasBitmap =
            logExecutionTime("Bitmap from areas") { createBitmapFromPrediction(connectedAreas) }
        val panels = logExecutionTime("Extract panels info") { extractPanelsInfo(connectedAreas) }
        val panelsBitmap =
            logExecutionTime("Bitmap from panels") { generatePanelsBitmap(resizedImage, panels) }
        return DetailedPredictionResult(
            bitmap,
            resizedImage,
            labeledAreasBitmap,
            panelsBitmap,
            PredictionResult(connectedAreas, panels)
        )
    }

    private fun generatePanelsBitmap(resizedImage: Bitmap, panels: Panels): Bitmap {
        val tempBitmap = Bitmap.createScaledBitmap(
            resizedImage,
            resizedImage.getWidth(),
            resizedImage.getHeight(),
            true
        )
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

    private fun extractPanelsInfo(labeledAreas: Array<IntArray>): Panels {
        if (true) { //TODO: Remove this hack to avoid crash temporally
            return Panels(listOf())
        }
        val allLabels = labeledAreas.flatten().distinct()
        Log.d("DeepPanel", "Number of different areas = ${allLabels.count()}")
        allLabels.forEach { label ->
            Log.d("DeepPanel", "Number of different areas. Label = $label")
        }
        val panelLabels = allLabels.filter { it > 1 }.map { it - 2 }.sorted()
        val borderInfo = Array(panelLabels.count()) { Array(4) { -1 } }
        for (x in 0 until inputImageWidth) {
            for (y in 0 until inputImageHeight) {
                val labelInArea = labeledAreas[x][y]
                if (labelInArea <= 1) {
                    continue
                }
                val currentBorderInfo = borderInfo[labelInArea - 2]
                val currentMinX = currentBorderInfo[0]
                if (x < currentMinX || currentMinX == -1) {
                    currentBorderInfo[0] = x
                }
                val currentMaxX = currentBorderInfo[1]
                if (x > currentMaxX) {
                    currentBorderInfo[1] = x
                }
                val currentMinY = currentBorderInfo[2]
                if (y < currentMinY || currentMinY == -1) {
                    currentBorderInfo[2] = y
                }
                val currentMaxY = currentBorderInfo[3]
                if (y > currentMaxY) {
                    currentBorderInfo[3] = y
                }
            }
        }
        return Panels(borderInfo.mapIndexed { index, borderInfoPerLabel ->
            val minX = max(borderInfoPerLabel[0] - borderSize, 0)
            val maxX = min(borderInfoPerLabel[1] + borderSize, inputImageHeight)
            val minY = max(borderInfoPerLabel[2] - borderSize, 0)
            val maxY = min(borderInfoPerLabel[3] + borderSize, inputImageHeight)
            Panel(
                panelNumberInPage = index,
                left = minX,
                bottom = minY,
                width = maxX - minX,
                height = maxY - minY
            )
        })
    }

    private fun Array<IntArray>.flatten(): List<Int> {
        val result = mutableListOf(sumBy { it.size })
        for (element in this) {
            for (i in element) {
                result.add(i)
            }
        }
        return result
    }

    private fun resizeInput(bitmapToResize: Bitmap): Bitmap {
        val reqWidth = inputImageWidth.toFloat()
        val reqHeight = inputImageHeight.toFloat()
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
            floatTypeSizeInBytes * inputImageWidth * inputImageHeight * numberOfChannels
        val imgData = ByteBuffer.allocateDirect(modelInputSize)
        imgData.order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputImageWidth * inputImageHeight)
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

    private fun createBitmapFromPrediction(prediction: Array<IntArray>): Bitmap {
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
}

private fun <T> logExecutionTime(name: String, lambda: () -> T): T {
    val init = System.currentTimeMillis()
    val result = lambda()
    val now = System.currentTimeMillis()
    val time = now - init
    Log.d("DeepPanel", "Time needed for $name = $time ms")
    return result
}

typealias Prediction = Array<IntArray>

data class PredictionResult(
    val connectedAreas: Prediction,
    val panels: Panels
)

data class DetailedPredictionResult(
    val imageInput: Bitmap,
    val resizedImage: Bitmap,
    val labeledAreasBitmap: Bitmap,
    val panelsBitmap: Bitmap,
    val predictionResult: PredictionResult
)
