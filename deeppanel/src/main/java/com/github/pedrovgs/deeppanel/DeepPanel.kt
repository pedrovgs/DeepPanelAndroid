package com.github.pedrovgs.deeppanel

import android.content.Context
import android.graphics.*
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder


class DeepPanel {
    companion object {
        private const val inputImageWidth = 224
        private const val inputImageHeight = 224
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
        return PredictionResult(bitmap, resizedImage, labeledPrediction)
    }

    private fun resizeInput(b: Bitmap): Bitmap {
        val reqWidth = inputImageWidth.toFloat()
        val reqHeight = inputImageHeight.toFloat()
        val m = Matrix()
        m.setRectToRect(
            RectF(0f, 0f, b.width.toFloat(), b.height.toFloat()),
            RectF(0f, 0f, reqWidth, reqHeight),
            Matrix.ScaleToFit.CENTER
        )
        val blackBackgroundBitmap = Bitmap.createBitmap(reqWidth.toInt(), reqHeight.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(blackBackgroundBitmap)
        val paint = Paint()
        paint.color = Color.BLACK
        canvas.drawRect(0f, 0f, reqWidth, reqHeight, paint)
        canvas.drawBitmap(b, m, Paint())
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
        for (i in 0 until 224) {
            for (j in 0 until 224) {
                val pixelInfo: Int = bitmap.getPixel(i, j)
                val normalizedRedChannel = Color.red(pixelInfo) / 255f
                print("[$normalizedRedChannel,")
                imgData.putFloat(normalizedRedChannel)
                val normalizedGreenChannel = Color.green(pixelInfo) / 255f
                print("$normalizedGreenChannel,")
                imgData.putFloat(normalizedGreenChannel)
                val normalizedBlueChannel = Color.blue(pixelInfo) / 255f
                print("$normalizedBlueChannel],")
                imgData.putFloat(normalizedBlueChannel)
            }
            println()
        }
        imgData.rewind()
        return imgData
    }

    private fun transformPredictionIntoLabels(prediction: Array<Array<FloatArray>>): Array<Array<Int>> {
        val labeledPrediction = Array(224) { Array(224) { 0 } }
        for (i in 0 until 224) {
            for (j in 0 until 224) {
                val pixel = prediction[i][j]
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

}

typealias Prediction = Array<Array<Int>>

data class PredictionResult(
    val imageInput: Bitmap,
    val resizedImage: Bitmap,
    val prediction: Prediction
)
