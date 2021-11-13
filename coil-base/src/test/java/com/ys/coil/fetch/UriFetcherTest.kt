package com.ys.coil.fetch

import FakeBitmapPool
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.size.PixelSize
import com.ys.coil.util.createOptions
import com.ys.coil.util.createTestMainDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
class UriFetcherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var mainDispatcher: TestCoroutineDispatcher
    private lateinit var loader: UriFetcher
    private lateinit var pool: BitmapPool

    @Before
    fun setUp() {
        mainDispatcher = createTestMainDispatcher()
        loader = UriFetcher(context)
        pool = FakeBitmapPool()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `basic asset load`() {
        val uri = Uri.parse("file:///android_asset/normal.jpg")
        assertTrue(loader.handles(uri))
        assertEquals(uri.toString(), loader.key(uri))

        val contentResolver: ShadowContentResolver = Shadow.extract(context.contentResolver)
        contentResolver.registerInputStream(uri, context.assets.open("normal.jpg"))

        val result = runBlocking {
            loader.fetch(pool, uri, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertFalse((result as SourceResult).source.exhausted())
    }
}