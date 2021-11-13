package com.ys.coil.memory

import com.ys.coil.bitmappool.RealBitmapPool
import com.ys.coil.util.DEFAULT_BITMAP_SIZE
import com.ys.coil.util.createBitmap
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MemoryCacheTest {
    private lateinit var counter: BitmapReferenceCounter

    @Before
    fun setUp() {
        counter = BitmapReferenceCounter(RealBitmapPool(0))
    }

    @Test
    fun `can retrieve cached value`() {
        val cache = MemoryCache(
            referenceCounter = counter,
            maxSize = (2 * DEFAULT_BITMAP_SIZE).toInt()
        )

        val bitmap = createBitmap()
        cache.set("1", bitmap, false)

        assertEquals(bitmap, cache["1"]?.bitmap)
    }

    @Test
    fun `least recently used value is evicted`() {
        val cache = MemoryCache(
            referenceCounter = counter,
            maxSize = (2 * DEFAULT_BITMAP_SIZE).toInt()
        )

        val first = createBitmap()
        cache.set("1", first, false)

        val second = createBitmap()
        cache.set("2", second, false)

        val third = createBitmap()
        cache.set("3", third, false)

        assertNull(cache["1"])
    }
}