package com.github.pedrovgs.deeppanel

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import kotlinx.android.synthetic.main.activity_extract_panels.*

class ExtractPanelsActivity : AppCompatActivity() {

    companion object {
        private const val resource_id_extra = "resource_id_extra"
        fun open(activity: Activity, resId: Int) {
            val intent = Intent(activity, ExtractPanelsActivity::class.java)
            intent.putExtra(resource_id_extra, resId)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_extract_panels)
        val defaultRes = R.drawable.sample_page_0
        val resId = intent?.extras?.getInt(resource_id_extra, defaultRes) ?: defaultRes
        extractPanelsFromResource(resId)
    }

    private fun extractPanelsFromResource(resId: Int) {
        val bitmapResImage = resources.getDrawable(resId, null).toBitmap()
        val initialTime = System.currentTimeMillis()
        val result = DeepPanel().extractPanelsInfo(bitmapResImage)
        val now = System.currentTimeMillis()
        val timeElapsed = now - initialTime
        val message = "Page analyzed in $timeElapsed ms"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        val generatePanelsBitmap = Bitmaps.generatePanelsBitmap(bitmapResImage, result.panels)
        image.setImageBitmap(generatePanelsBitmap)
        renderPanelsInfo(result.panels, timeElapsed)
        image.setOnClickListener {
            FullScreenImageActivity.open(this, generatePanelsBitmap)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun renderPanelsInfo(panels: Panels, timeElapsed: Long) {
        numberOfPanelsView.text = "Number of panels: ${panels.numberOfPanels} found in ${timeElapsed}ms"
        panelsInfoView.text = panels.panelsInfo.joinToString("\n---------\n") { panel ->
            """Left: ${panel.left}, Top: ${panel.top}
                    |Right: ${panel.right}, Bottom: ${panel.bottom}
                    |Width: ${panel.width}, Height: ${panel.height}
                """.trimMargin()
        }
    }
}
