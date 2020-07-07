package com.github.pedrovgs.deeppanel

import android.content.Context
import android.graphics.*
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min


class DeepPanel {
    companion object {
        private const val inputImageWidth = 224
        private const val inputImageHeight = 224
        private const val borderSize = 4
    }

    private lateinit var interpreter: Interpreter

    fun initialize(context: Context) {
        val model = loadModel(context)
        interpreter = Interpreter(model)
    }

    fun extractPanels(bitmap: Bitmap): PredictionResult {
        val resizedImage = resizeInput(bitmap)
        val modelInput = convertBitmapToByteBuffer(resizedImage)
        val prediction =
            Array(1) { Array(inputImageWidth) { Array(inputImageHeight) { FloatArray(3) } } }
        interpreter.run(modelInput, prediction)
        val labeledPrediction = transformPredictionIntoLabels(prediction[0])
        val predictedBitmap = createBitmapFromPrediction(labeledPrediction)
        val labeledAreas = findPanels(labeledPrediction)
        val labeledAreasBitmap = createBitmapFromPrediction(labeledAreas)
        val panels = extractPanelsInfo(labeledAreas)
        val panelsBitmap = generatePanelsBitmap(resizedImage, panels)
        return PredictionResult(
            bitmap,
            resizedImage,
            labeledPrediction,
            predictedBitmap,
            labeledAreas,
            labeledAreasBitmap,
            panels,
            panelsBitmap
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

    private fun extractPanelsInfo(labeledAreas: Array<Array<Int>>): Panels {
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
            Log.d("DeepPanel", "====> ")
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

    private fun findPanels(labeledPrediction: Array<Array<Int>>): Array<Array<Int>> {
        //val rawAreas = CCL.twoPass(labeledPrediction)
        val rawAreas = ConnectedComponentLabeling.findAreas(labeledPrediction)
        val fixedAreas = removeSmallAreas(rawAreas)
        val normalizedAreas = normalizeAreas(fixedAreas)
        return normalizedAreas
    }

    private fun normalizeAreas(areas: Array<Array<Int>>): Array<Array<Int>> {
        val normalizedAreas = Array(inputImageWidth) { Array(inputImageHeight) { 0 } }
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

    private fun removeSmallAreas(rawAreas: Array<Array<Int>>): Array<Array<Int>> {
        val flattenedLabels = rawAreas.flatten()
        val minimumAreaAllowed = inputImageWidth * inputImageHeight * 0.01
        val fixedAreas = Array(inputImageWidth) { Array(inputImageHeight) { 0 } }
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

    private fun transformPredictionIntoLabels(prediction: Array<Array<FloatArray>>): Array<Array<Int>> {
        val labeledPrediction = Array(224) { Array(224) { 0 } }
        for (i in 0 until 224) {
            for (j in 0 until 224) {
                // This change in the index usage is needed because the input of the model
                // is turned 90 degress to the left
                val pixel = prediction[j][i]
                val background = pixel[0]
                val border = pixel[1]
                val content = pixel[2]
                if (background >= content && background >= border) {
                    labeledPrediction[i][j] = 0
                } else if (border >= background && border >= content) {
                    labeledPrediction[i][j] = 1
                } else if (content >= background && content >= border) {
                    labeledPrediction[i][j] = 2
                }
            }
        }
        return labeledPrediction
    }

    private fun createBitmapFromPrediction(prediction: Array<Array<Int>>): Bitmap {
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

typealias Prediction = Array<Array<Int>>

data class PredictionResult(
    val imageInput: Bitmap,
    val resizedImage: Bitmap,
    val prediction: Prediction,
    val predictedBitmap: Bitmap,
    val labeledAreas: Prediction,
    val labeledAreasBitmap: Bitmap,
    val panels: Panels,
    val panelsBitmap: Bitmap
)
