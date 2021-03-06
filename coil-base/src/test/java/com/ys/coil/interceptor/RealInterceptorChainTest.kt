package com.ys.coil.interceptor

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import com.ys.coil.EventListener
import com.ys.coil.lifecycle.FakeLifecycle
import com.ys.coil.memory.MemoryCache
import com.ys.coil.request.ImageRequest
import com.ys.coil.request.ImageResult
import com.ys.coil.size.OriginalSize
import com.ys.coil.size.PixelSize
import com.ys.coil.size.Size
import com.ys.coil.transform.CircleCropTransformation
import com.ys.coil.util.createRequest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
class RealInterceptorChainTest {

	private lateinit var context: Context

	@Before
	fun before() {
		context = ApplicationProvider.getApplicationContext()
	}

	@Test
	fun `interceptor cannot set data to null`() {
		val request = createRequest(context) {
			data("https://www.example.com/image.jpg")
		}
		val interceptor = Interceptor { chain ->
			chain.proceed(chain.request.newBuilder().data(null).build())
		}
		assertFailsWith<IllegalStateException> {
			testChain(request, listOf(interceptor))
		}
	}

	@Test
	fun `interceptor cannot modify target`() {
		val request = createRequest(context) {
			target(ImageView(context))
		}
		val interceptor = Interceptor { chain ->
			chain.proceed(chain.request.newBuilder(context).target(ImageView(context)).build())
		}
		assertFailsWith<java.lang.IllegalStateException> {
			testChain(request, listOf(interceptor))
		}
	}

	@Test
	fun `interceptor cannot modify lifecycle`() {
		val request = createRequest(context) {
			lifecycle(FakeLifecycle())
		}
		val interceptor = Interceptor { chain ->
			chain.proceed(chain.request.newBuilder().lifecycle(FakeLifecycle()).build())
		}
		assertFailsWith<IllegalStateException> {
			testChain(request, listOf(interceptor))
		}
	}

	@Test
	fun `interceptor cannot modify sizeResolver`() {
		val request = createRequest(context)
		val interceptor = Interceptor { chain ->
			chain.proceed(chain.request.newBuilder().size(PixelSize(100, 100)).build())
		}
		assertFailsWith<IllegalStateException> {
			testChain(request, listOf(interceptor))
		}
	}

	@Test
	fun `request modifications are passed to subsequent interceptors`() {
		val initialRequest = createRequest(context)
		var request = initialRequest
		val interceptor1 = Interceptor { chain ->
			assertSame(request, chain.request)
			request = chain.request.newBuilder().memoryCacheKey(MemoryCache.Key("test")).build()
			chain.proceed(request)
		}
		val interceptor2 = Interceptor { chain ->
			assertSame(request, chain.request)
			request = chain.request.newBuilder().bitmapConfig(Bitmap.Config.RGB_565).build()
			chain.proceed(request)
		}
		val interceptor3 = Interceptor { chain ->
			assertSame(request, chain.request)
			request = chain.request.newBuilder().transformations(CircleCropTransformation()).build()
			chain.proceed(request)
		}
		val result = testChain(request, listOf(interceptor1, interceptor2, interceptor3))

		assertNotEquals(initialRequest, result.request)
		assertSame(request, result.request)
	}

	@Test
	fun `withSize is passed to subsequent interceptors`() {
		var size: Size = PixelSize(100, 100)
		val request = createRequest(context) {
			size(size)
		}
		val interceptor1 = Interceptor { chain ->
			assertEquals(size, chain.size)
			size = PixelSize(123, 456)
			chain.withSize(size).proceed(chain.request)
		}
		val interceptor2 = Interceptor { chain ->
			assertEquals(size, chain.size)
			size = PixelSize(1728, 400)
			chain.withSize(size).proceed(chain.request)
		}
		val interceptor3 = Interceptor { chain ->
			assertEquals(size, chain.size)
			size = OriginalSize
			chain.withSize(size).proceed(chain.request)
		}
		val result = testChain(request, listOf(interceptor1, interceptor2, interceptor3))

		assertEquals(OriginalSize, size)
		assertSame(request, result.request)
	}

	private fun testChain(request: ImageRequest, interceptors: List<Interceptor>): ImageResult {
		val chain = RealInterceptorChain(
			initialRequest = request,
			interceptors = interceptors + FakeInterceptor(),
			index = 0,
			request = request,
			size = PixelSize(100, 100),
			eventListener = EventListener.NONE,
			isPlaceholderCached = false
		)
		return runBlocking { chain.proceed(request) }
	}
}
