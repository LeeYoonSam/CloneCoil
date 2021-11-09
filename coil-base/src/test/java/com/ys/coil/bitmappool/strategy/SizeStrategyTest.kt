package com.ys.coil.bitmappool.strategy

import android.graphics.Bitmap
import com.ys.coil.util.createBitmap

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SizeStrategyTest {

    private lateinit var strategy: SizeStrategy

    @Before
    fun setUp() {
        strategy = SizeStrategy()
    }

    @Test
    fun `equal size bitmap is reused`() {
        val bitmap = createBitmap()
        strategy.put(bitmap)

        kotlin.test.assertEquals(bitmap, strategy.get(100, 100, Bitmap.Config.ARGB_8888))
    }

    @Test
    fun `too small bitmap is not reused`() {
        val bitmap = createBitmap(width = 20, height = 20)
        strategy.put(bitmap)

        kotlin.test.assertEquals(null, strategy.get(100, 100, Bitmap.Config.ARGB_8888))
    }

    @Test
    fun `large enough bitmap with different config is reused`() {
        val bitmap = createBitmap(width = 250, height = 250, config = Bitmap.Config.RGB_565)
        strategy.put(bitmap)

        kotlin.test.assertEquals(bitmap, strategy.get(100, 100, Bitmap.Config.ARGB_8888))
    }
}