package com.github.pedrovgs.deeppanel

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_full_screen_image.*

class FullScreenImageActivity : AppCompatActivity() {
    companion object {
        private const val extraImage: String = "FullScreenImageActivity_extraImage"
        fun open(activity: Activity, bitmap: Bitmap) {
            val bundle = Bundle()
            val intent = Intent(activity, FullScreenImageActivity::class.java)
            intent.putExtra(extraImage, bitmap)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)
        val bitmap: Bitmap = intent.extras?.getParcelable(extraImage)!!
        image.setImageBitmap(bitmap)
    }
}
