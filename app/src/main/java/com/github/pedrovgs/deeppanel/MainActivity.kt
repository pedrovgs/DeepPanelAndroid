package com.github.pedrovgs.deeppanel

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val deepPanel = DeepPanel()
        deepPanel.initialize(this)
        var currentPage = 0
        val resList = listOf(
            R.drawable.sample_page_0,
            R.drawable.sample_page_1,
            R.drawable.sample_page_2,
            R.drawable.sample_page_3,
            R.drawable.sample_page_4,
            R.drawable.sample_page_5,
            R.drawable.sample_page_6,
            R.drawable.sample_page_7,
            R.drawable.sample_page_8,
            R.drawable.sample_page_9,
            R.drawable.sample_page_10
        ).reversed()
        val size: Int = resList.size
        showPredictionForPage(deepPanel, resList[currentPage % size])
        toolbar.setOnClickListener {
            currentPage += 1
            showPredictionForPage(deepPanel, resList[currentPage % size])
        }
    }

    private fun showPredictionForPage(deepPanel: DeepPanel, pageResource: Int) {
        loading.visibility = View.VISIBLE
        val bitmapSamplePage = resources.getDrawable(pageResource, null).toBitmap()
        Thread {
            val result = deepPanel.extractPanels(bitmapSamplePage)
            image.post {
                image.setImageBitmap(result.imageInput)
                prediction.setImageBitmap(result.predictedBitmap)
                mask.setImageBitmap(result.labeledAreasBitmap)
                panelsInfo.setImageBitmap(result.panelsBitmap)
                image.setOnClickListener {
                    FullScreenImageActivity.open(this, result.resizedImage)
                }
                prediction.setOnClickListener {
                    FullScreenImageActivity.open(this, result.predictedBitmap)
                }
                mask.setOnClickListener {
                    FullScreenImageActivity.open(this, result.labeledAreasBitmap)
                }
                panelsInfo.setOnClickListener {
                    FullScreenImageActivity.open(this, result.panelsBitmap)
                }
                Log.d("DeepPanel", "Misunsi =====> ")
                result.panels.panelsInfo.forEach {
                    Log.d("DeepPanel", "=====> $it")
                }
                Log.d("DeepPanel", "Misunsi =====> ")
                loading.visibility = View.GONE
            }
        }.start()
    }
}
