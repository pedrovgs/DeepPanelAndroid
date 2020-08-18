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

    private val ccl = NativeConnectedComponentLabeling()
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
        val labeledPrediction = ccl.transformPredictionIntoLabels(prediction[0])
        val connectedAreas = findPanels(labeledPrediction)
        val panels = extractPanelsInfo(connectedAreas)
        return PredictionResult(labeledPrediction, connectedAreas, panels)
    }

    fun extractDetailedPanelsInfo(bitmap: Bitmap): DetailedPredictionResult {
        val resizedImage = resizeInput(bitmap)
        val modelInput = convertBitmapToByteBuffer(resizedImage)
        val prediction =
            Array(1) { Array(inputImageWidth) { Array(inputImageHeight) { FloatArray(3) } } }
        interpreter.run(modelInput, prediction)
        val labeledPrediction = ccl.transformPredictionIntoLabels(prediction[0])
        val predictedBitmap = createBitmapFromPrediction(labeledPrediction)
        val connectedAreas = findPanels(labeledPrediction)
        val labeledAreasBitmap = createBitmapFromPrediction(connectedAreas)
        val panels = extractPanelsInfo(connectedAreas)
        val panelsBitmap = generatePanelsBitmap(resizedImage, panels)
        return DetailedPredictionResult(
            bitmap,
            resizedImage,
            predictedBitmap,
            labeledAreasBitmap,
            panelsBitmap,
            PredictionResult(labeledPrediction, connectedAreas, panels)
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

    private fun findPanels(labeledPrediction: Array<IntArray>): Array<IntArray> {
        val rawAreas = ConnectedComponentLabeling.findAreas(labeledPrediction)
        val fixedAreas = removeSmallAreas(rawAreas)
        return normalizeAreas(fixedAreas)
    }

    private fun normalizeAreas(areas: Array<IntArray>): Array<IntArray> {
        val normalizedAreas = Array(inputImageWidth) { IntArray(inputImageHeight) }
        val labelsUsed = mutableMapOf<Int, Int>()
        var currentNormalizedLabel = 2
        for (x in 0 until inputImageWidth) {
            for (y in 0 until inputImageHeight) {
                val rawLabel = areas[x][y]
                if (rawLabel <= 1) {
                    normalizedAreas[x][y] = rawLabel
                } else {
                    val normalizedLabelForRawLabel = labelsUsed[rawLabel]
                    if (normalizedLabelForRawLabel == null) {
                        normalizedAreas[x][y] = currentNormalizedLabel
                        labelsUsed[rawLabel] = currentNormalizedLabel
                        currentNormalizedLabel++
                    } else {
                        normalizedAreas[x][y] = normalizedLabelForRawLabel
                    }
                }
            }
        }
        return normalizedAreas
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

    private fun removeSmallAreas(rawAreas: Array<IntArray>): Array<IntArray> {
        val flattenedLabels = rawAreas.flatten()
        val minimumAreaAllowed = inputImageWidth * inputImageHeight * 0.01
        val fixedAreas = Array(inputImageWidth) { IntArray(inputImageHeight) }
        for (x in 0 until inputImageWidth) {
            for (y in 0 until inputImageHeight) {
                val rawPixelLabel = rawAreas[x][y]
                if (rawPixelLabel <= 1) {
                    fixedAreas[x][y] = rawPixelLabel
                } else {
                    val numberOfPixelsPerLabel = flattenedLabels.count { it == rawPixelLabel }
                    if (numberOfPixelsPerLabel > minimumAreaAllowed) {
                        fixedAreas[x][y] = rawPixelLabel
                    } else {
                        Log.d(
                            "DeepPanel",
                            "Removing area with label $rawPixelLabel because the size is $numberOfPixelsPerLabel and the minimum allowed size is $minimumAreaAllowed"
                        )
                        fixedAreas[x][y] = -1
                    }
                }
            }
        }
        return fixedAreas
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

typealias Prediction = Array<IntArray>

data class PredictionResult(
    val prediction: Prediction,
    val connectedAreas: Prediction,
    val panels: Panels
)

data class DetailedPredictionResult(
    val imageInput: Bitmap,
    val resizedImage: Bitmap,
    val predictedBitmap: Bitmap,
    val labeledAreasBitmap: Bitmap,
    val panelsBitmap: Bitmap,
    val predictionResult: PredictionResult
)
