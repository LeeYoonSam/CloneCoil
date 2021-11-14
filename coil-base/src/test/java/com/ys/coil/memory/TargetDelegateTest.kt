package com.ys.coil.memory

import FakeBitmapPool
import android.content.Context
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import com.ys.coil.ImageLoader
import com.ys.coil.target.FakeTarget
import com.ys.coil.target.ImageViewTarget
import com.ys.coil.util.createBitmap
import com.ys.coil.util.createGetRequest
import com.ys.coil.util.createLoadRequest
import com.ys.coil.util.toDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.*

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TargetDelegateTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var dispatcher: TestCoroutineDispatcher
    private lateinit var imageLoader: ImageLoader
    private lateinit var pool: FakeBitmapPool
    private lateinit var counter: BitmapReferenceCounter
    private lateinit var delegateService: DelegateService

    @Before
    fun setUp() {
        dispatcher = TestCoroutineDispatcher()
        imageLoader = ImageLoader(context)
        pool = FakeBitmapPool()
        counter = BitmapReferenceCounter(pool)
        delegateService = DelegateService(imageLoader, counter)
    }

    @After
    fun after() {
        Dispatchers.resetMain()
        imageLoader.shutdown()
    }

    @Test
    fun `empty target does not invalidate`() {
        val request = createLoadRequest(context)
        val delegate = delegateService.createTargetDelegate(request)

        runBlocking {
            val bitmap = createBitmap()
            val drawable = bitmap.toDrawable(context)
            delegate.start(drawable, drawable)
            assertFalse(counter.invalid(bitmap))
        }

        runBlocking {
            val bitmap = createBitmap()
            delegate.success(bitmap.toDrawable(context), 0)
            assertFalse(counter.invalid(bitmap))
        }
    }

    @Test
    fun `get request invalidates the success bitmap`() {
        val request = createGetRequest()
        val delegate = delegateService.createTargetDelegate(request)

        runBlocking {
            val bitmap = createBitmap()
            delegate.success(bitmap.toDrawable(context), 0)
            assertTrue(counter.invalid(bitmap))
        }
    }

    @Test
    fun `target methods are called and bitmaps are invalidated`() {
        val target = FakeTarget()
        val request = createLoadRequest(context) {
            target(target)
        }
        val delegate = delegateService.createTargetDelegate(request)

        runBlocking {
            val bitmap = createBitmap()
            val drawable = bitmap.toDrawable(context)
            delegate.start(drawable, null)
            assertTrue(target.start)
            assertTrue(counter.invalid(bitmap))
        }

        runBlocking {
            val bitmap = createBitmap()
            delegate.success(bitmap.toDrawable(context), 0)
            assertTrue(target.success)
            assertTrue(counter.invalid(bitmap))
        }

        runBlocking {
            val bitmap = createBitmap()
            delegate.error(bitmap.toDrawable(context), 0)
            assertTrue(target.error)
            assertFalse(counter.invalid(bitmap))
        }
    }

    @Test
    fun `request with poolable target returns previous bitmap to pool`() {
        val request = createLoadRequest(context) {
            target(ImageViewTarget(ImageView(context)))
        }

        val delegate = delegateService.createTargetDelegate(request)

        val initialBitmap = createBitmap()
        runBlocking {
            val drawable = initialBitmap.toDrawable(context)
            delegate.start(drawable, drawable)
            assertFalse(counter.invalid(initialBitmap))
        }

        runBlocking {
            val bitmap = createBitmap()
            delegate.success(bitmap.toDrawable(context), 0)
            assertFalse(counter.invalid(bitmap))
        }

        assertTrue(pool.bitmaps.contains(initialBitmap))
    }
}