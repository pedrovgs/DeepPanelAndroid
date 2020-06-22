package com.github.pedrovgs.deeppanel

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val deepPanel = DeepPanel()
        deepPanel.initialize(this)
        val bitmapSamplePage = resources.getDrawable(R.drawable.sample_page_3, null).toBitmap()
        val result = deepPanel.extractPanels(bitmapSamplePage)
        image.setImageBitmap(result.imageInput)
        prediction.setImageBitmap(result.resizedImage)
        val predictedBitmap = createBitmapFromPrediction(result.prediction)
        mask.setImageBitmap(predictedBitmap)
    }

    private fun createBitmapFromPrediction(prediction: Array<Array<Int>>): Bitmap {
        val imageSize = 224
        val bitmap = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888)
        for (x in 0 until imageSize) {
            for (y in 0 until imageSize) {
                val color = when (prediction[x][y]) {
                        0 -> Color.BLUE
                        1 -> Color.RED
                        else -> Color.GREEN
                    }
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }
}
