package com.ys.coil.fetch

import FakeBitmapPool
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.size.PixelSize
import com.ys.coil.util.createMockWebServer
import com.ys.coil.util.createOptions
import com.ys.coil.util.createTestMainDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HttpUrlFetcherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var mainDispatcher: TestCoroutineDispatcher
    private lateinit var server: MockWebServer
    private lateinit var fetcher: HttpUrlFetcher
    private lateinit var pool: BitmapPool

    @Before
    fun setUp() {
        mainDispatcher = createTestMainDispatcher()
        server = createMockWebServer(context, "normal.jpg")
        fetcher = HttpUrlFetcher(OkHttpClient())
        pool = FakeBitmapPool()
    }

    @After
    fun after() {
        Dispatchers.resetMain()
        server.shutdown()
    }

    @Test
    fun `basic network load`() {
        val url = server.url("/normal.jpg")
        assertTrue(fetcher.handles(url))
        assertEquals(fetcher.key(url), url.toString())

        val result = runBlocking {
            fetcher.fetch(pool, url, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertFalse((result as SourceResult).source.exhausted())
    }
}