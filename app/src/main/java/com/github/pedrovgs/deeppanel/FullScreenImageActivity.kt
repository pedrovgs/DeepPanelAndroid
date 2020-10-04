package com.github.pedrovgs.deeppanel

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_full_screen_image.*

class FullScreenImageActivity : AppCompatActivity() {
    companion object {
        lateinit var extraImage: Bitmap
        fun open(activity: Activity, bitmap: Bitmap) {
            extraImage = bitmap
            val intent = Intent(activity, FullScreenImageActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)
        val bitmap: Bitmap = extraImage
        image.setImageBitmap(bitmap)
    }
}
