package com.github.pedrovgs.deeppanel

import android.graphics.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

object Bitmaps {

    fun computeResizeScale(bitmap: Bitmap): Float {
        val originalSize = max(bitmap.width, bitmap.height)
        return originalSize / DeepPanel.modelInputImageSize.toFloat()
    }

    fun resizeInput(bitmapToResize: Bitmap): Bitmap {
        val reqWidth = DeepPanel.modelInputImageSize.toFloat()
        val reqHeight = DeepPanel.modelInputImageSize.toFloat()
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

    fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val floatTypeSizeInBytes = 4
        val numberOfChannels = 3
        val modelInputSize =
            floatTypeSizeInBytes * DeepPanel.modelInputImageSize * DeepPanel.modelInputImageSize * numberOfChannels
        val imgData = ByteBuffer.allocateDirect(modelInputSize)
        imgData.order(ByteOrder.nativeOrder())
        val pixels = IntArray(DeepPanel.modelInputImageSize * DeepPanel.modelInputImageSize)
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

    fun createBitmapFromPrediction(prediction: Prediction): Bitmap {
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

    fun generatePanelsBitmap(sourceBitmap: Bitmap, panels: Panels): Bitmap {
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