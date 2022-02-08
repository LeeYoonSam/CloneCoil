package com.ys.coil.test.util

import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.ys.coil.test.R

class TestActivity : AppCompatActivity(R.layout.ic_test_activity) {
	val imageView: ImageView by lazy { findViewById(R.id.image) }
}
