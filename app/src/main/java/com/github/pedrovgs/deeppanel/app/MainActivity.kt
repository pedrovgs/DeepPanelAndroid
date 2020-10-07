package com.github.pedrovgs.deeppanel.app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.github.pedrovgs.deeppanel.DeepPanel
import com.github.pedrovgs.deeppanel.app.Pages.resList
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val deepPanel = DeepPanel()
        var currentPage = 0
        val size: Int = resList.size
        showPredictionForPage(deepPanel, resList[currentPage % size])
        nextPageButton.setOnClickListener {
            currentPage = (currentPage + 1) % resList.count()
            showPredictionForPage(deepPanel, resList[currentPage % size])
        }
        detailPageButton.setOnClickListener {
            ExtractPanelsActivity.open(this, resList[currentPage])
        }
    }

    private fun showPredictionForPage(deepPanel: DeepPanel, pageResource: Int) {
        val bitmapSamplePage = resources.getDrawable(pageResource, null).toBitmap()
        val initialTime = System.currentTimeMillis()
        val result = deepPanel.extractDetailedPanelsInfo(bitmapSamplePage)
        val now = System.currentTimeMillis()
        val timeElapsed = now - initialTime
        image.post {
            val message = "Page analyzed in $timeElapsed ms"
            Log.d("DeepPanel", message)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
        }
    }
}
