package com.github.pedrovgs.deeppanel

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val deepPanel = DeepPanel()
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
            val initialTime = System.currentTimeMillis()
            val result = deepPanel.extractDetailedPanelsInfo(bitmapSamplePage)
            val now = System.currentTimeMillis()
            val timeElapsed = now - initialTime
            image.post {
                val message = "Page analyzed in $timeElapsed ms"
                Log.d("DeepPanel", message)
                Toast.makeText(loading.context, message, Toast.LENGTH_SHORT).show()
                image.setImageBitmap(result.imageInput)
                mask.setImageBitmap(result.labeledAreasBitmap)
                panelsInfo.setImageBitmap(result.panelsBitmap)
                image.setOnClickListener {
                    FullScreenImageActivity.open(this, result.resizedImage)
                }
                mask.setOnClickListener {
                    FullScreenImageActivity.open(this, result.labeledAreasBitmap)
                }
                panelsInfo.setOnClickListener {
                    FullScreenImageActivity.open(this, result.panelsBitmap)
                }
                loading.visibility = View.GONE
            }
        }.start()
    }
}
