package com.ys.coil.size

import org.junit.Test
import kotlin.test.assertEquals

class PixelSizeTest {
    @Test
    fun `create view`() {
        val size = PixelSize(10, 20)
        assertEquals(10, size.width)
        assertEquals(20, size.height)
    }
}