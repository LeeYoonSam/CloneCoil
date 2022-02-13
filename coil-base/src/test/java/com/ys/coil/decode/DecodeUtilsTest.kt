package com.ys.coil.decode

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ys.coil.size.Scale
import okio.buffer
import okio.source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DecodeUtilsTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `inSampleSize with FILL is calculated correctly`() {
        assertEquals(2, DecodeUtils.calculateInSampleSize(100, 100, 50, 50, Scale.FILL))

        assertEquals(1, DecodeUtils.calculateInSampleSize(100, 50, 50, 50, Scale.FILL))

        assertEquals(1, DecodeUtils.calculateInSampleSize(99, 99, 50, 50, Scale.FILL))

        assertEquals(1, DecodeUtils.calculateInSampleSize(200, 99, 50, 50, Scale.FILL))

        assertEquals(4, DecodeUtils.calculateInSampleSize(200, 200, 50, 50, Scale.FILL))

        assertEquals(8, DecodeUtils.calculateInSampleSize(1024, 1024, 80, 80, Scale.FILL))

        assertEquals(1, DecodeUtils.calculateInSampleSize(50, 50, 100, 100, Scale.FILL))
    }

    @Test
    fun `inSampleSize with FIT is calculated correctly`() {
        assertEquals(2, DecodeUtils.calculateInSampleSize(100, 100, 50, 50, Scale.FIT))

        assertEquals(2, DecodeUtils.calculateInSampleSize(100, 50, 50, 50, Scale.FIT))

        assertEquals(1, DecodeUtils.calculateInSampleSize(99, 99, 50, 50, Scale.FIT))

        assertEquals(4, DecodeUtils.calculateInSampleSize(200, 99, 50, 50, Scale.FIT))

        assertEquals(4, DecodeUtils.calculateInSampleSize(200, 200, 50, 50, Scale.FIT))

        assertEquals(8, DecodeUtils.calculateInSampleSize(160, 1024, 80, 80, Scale.FIT))

        assertEquals(1, DecodeUtils.calculateInSampleSize(50, 50, 100, 100, Scale.FIT))
    }

    @Test
    fun `isGif true positive`() {
        val source = context.assets.open("animated.gif").source().buffer()
        assertTrue(DecodeUtils.isGif(source))
    }

    @Test
    fun `isGif true negative`() {
        val source = context.assets.open("normal.jpg").source().buffer()
        assertFalse(DecodeUtils.isGif(source))
    }

    @Test
    fun `isWebP true positive`() {
        val source = context.assets.open("static.webp").source().buffer()
        assertTrue(DecodeUtils.isWebP(source))
    }

    @Test
    fun `isWebP true negative`() {
        val source = context.assets.open("normal.jpg").source().buffer()
        assertFalse(DecodeUtils.isWebP(source))
    }

    @Test
    fun `isAnimatedWebP true positive`() {
        val source = context.assets.open("animated.webp").source().buffer()
        assertTrue(DecodeUtils.isAnimatedWebP(source))
    }

    @Test
    fun `isAnimatedWebP true negative`() {
        val source = context.assets.open("static.webp").source().buffer()
        assertFalse(DecodeUtils.isAnimatedWebP(source))
    }

    @Test
    fun `isHeif true positive`() {
        val source = context.assets.open("static.heif").source().buffer()
        assertTrue(DecodeUtils.isHeif(source))
    }

    @Test
    fun `isHeif true negative`() {
        val source = context.assets.open("normal.jpg").source().buffer()
        assertFalse(DecodeUtils.isHeif(source))
    }

    @Test
    fun `isAnimatedHeif true positive`() {
        val source = context.assets.open("animated.heif").source().buffer()
        assertTrue(DecodeUtils.isAnimatedHeif(source))
    }

    @Test
    fun `isAnimatedHeif true negative`() {
        val source = context.assets.open("animated.webp").source().buffer()
        assertFalse(DecodeUtils.isAnimatedHeif(source))
    }


}
