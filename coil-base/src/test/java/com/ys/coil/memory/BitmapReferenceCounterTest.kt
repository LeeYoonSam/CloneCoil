package com.ys.coil.memory

import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.bitmappool.RealBitmapPool
import com.ys.coil.util.DEFAULT_BITMAP_SIZE
import com.ys.coil.util.createBitmap
import org.junit.Assert.*

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitmapReferenceCounterTest {

    private lateinit var pool: BitmapPool
    private lateinit var counter: BitmapReferenceCounter

    @Before
    fun setUp() {
        pool = RealBitmapPool(DEFAULT_BITMAP_SIZE)
        counter = BitmapReferenceCounter(pool)
    }

    @Test
    fun `count is incremented`() {
        val bitmap = createBitmap()
        counter.increment(bitmap)

        assertEquals(1, counter.count(bitmap))
    }

    @Test
    fun `count is decremented`() {
        val bitmap = createBitmap()
        counter.increment(bitmap)
        counter.increment(bitmap)

        assertEquals(2, counter.count(bitmap))

        counter.decrement(bitmap)

        assertEquals(1, counter.count(bitmap))
    }

    @Test
    fun `bitmap is added to pool if count reaches zero`() {
        val bitmap = createBitmap()
        counter.increment(bitmap)

        assertEquals(1, counter.count(bitmap))

        counter.decrement(bitmap)

        assertEquals(0, counter.count(bitmap))
        assertEquals(bitmap, pool.getDirtyOrNull(bitmap.width, bitmap.height, bitmap.config))
    }

    @Test
    fun `invalid bitmap is not added to pool if count reached zero`() {
        val bitmap = createBitmap()
        counter.increment(bitmap)
        counter.invalidate(bitmap)

        assertEquals(1, counter.count(bitmap))

        counter.decrement(bitmap)

        assertEquals(0, counter.count(bitmap))
        assertNull(pool.getDirtyOrNull(bitmap.width, bitmap.height, bitmap.config))
    }
}