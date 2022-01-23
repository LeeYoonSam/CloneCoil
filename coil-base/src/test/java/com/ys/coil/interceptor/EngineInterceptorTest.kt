package com.ys.coil.interceptor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Bitmap.Config.HARDWARE
import android.graphics.Bitmap.Config.RGBA_F16
import android.graphics.Bitmap.Config.RGB_565
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import com.ys.coil.EventListener
import com.ys.coil.ImageLoader
import com.ys.coil.RealImageLoader
import com.ys.coil.decode.DataSource
import com.ys.coil.interceptor.EngineInterceptor.Companion.EXTRA_IS_SAMPLED
import com.ys.coil.interceptor.EngineInterceptor.Companion.MEMORY_CACHE_KEY_HEIGHT
import com.ys.coil.interceptor.EngineInterceptor.Companion.MEMORY_CACHE_KEY_TRANSFORMATIONS
import com.ys.coil.interceptor.EngineInterceptor.Companion.MEMORY_CACHE_KEY_WIDTH
import com.ys.coil.interceptor.EngineInterceptor.Companion.TRANSFORMATIONS_DELIMITER
import com.ys.coil.interceptor.EngineInterceptor.ExecuteResult
import com.ys.coil.key.Keyer
import com.ys.coil.memory.MemoryCache.Key
import com.ys.coil.memory.MemoryCache.Value
import com.ys.coil.request.ImageRequest
import com.ys.coil.request.Options
import com.ys.coil.request.Parameters
import com.ys.coil.request.RequestService
import com.ys.coil.size
import com.ys.coil.size.OriginalSize
import com.ys.coil.size.PixelSize
import com.ys.coil.size.Precision
import com.ys.coil.size.Scale
import com.ys.coil.size.Size
import com.ys.coil.transform.CircleCropTransformation
import com.ys.coil.transform.Transformation
import com.ys.coil.util.SystemCallbacks
import com.ys.coil.util.createBitmap
import com.ys.coil.util.createRequest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalStdlibApi::class)
class EngineInterceptorTest {

	private lateinit var context: Context

	@Before
	fun before() {
		context = ApplicationProvider.getApplicationContext()
	}

	@Test
	fun `computeMemoryCache - null key`() {
		val interceptor = newInterceptor(key = null)
		val request = createRequest(context)
		val options = Options(context, size = OriginalSize)
		val key = runBlocking {
			interceptor.getMemoryCacheKey(request, Unit, options, EventListener.NONE)
		}

		assertNull(key)
	}

	@Test
	fun `computeMemoryCache - simple key`() {
		val interceptor = newInterceptor()
		val request = createRequest(context)
		val options = Options(context, size = OriginalSize)
		val actual = runBlocking {
			interceptor.getMemoryCacheKey(request, Unit, options, EventListener.NONE)
		}

		assertEquals(newMemoryCacheKey(), actual)
	}

	@Test
	fun `computeMemoryCacheKey - params only`() {
		val interceptor = newInterceptor()
		val parameters = createFakeParameters()
		val request = createRequest(context) {
			parameters(parameters)
		}
		val options = Options(context, size = OriginalSize)
		val actual = runBlocking {
			interceptor.getMemoryCacheKey(request, Unit, options, EventListener.NONE)
		}

		assertEquals(newMemoryCacheKey(parameters = parameters), actual)
	}

	@Test
	fun `computeMemoryCacheKey - transformations only`() {
		val interceptor = newInterceptor()
		val transformations = createFakeTransformations()
		val request = createRequest(context) {
			transformations(transformations)
		}
		val size = PixelSize(123, 332)
		val options = Options(context, size = size)
		val actual = runBlocking {
			interceptor.getMemoryCacheKey(request, Unit, options, EventListener.NONE)
		}

		assertEquals(newMemoryCacheKey(transformations = transformations, size = size), actual)
	}

	@Test
	fun `isCachedValueValid - fill`() {
		val interceptor = newInterceptor()
		val request = createRequest(context) {
			size(100, 100)
			precision(Precision.INEXACT)
			scale(Scale.FILL)
		}
		val cached = createBitmap()
		assertFalse(interceptor.isCachedValueValid(
			cached = cached,
			isSampled = true,
			request = request,
			size = PixelSize(200, 200)
		))
		assertFalse(interceptor.isCachedValueValid(
			cached = cached,
			isSampled = true,
			request = request,
			size = PixelSize(150, 50)
		))
		assertTrue(interceptor.isCachedValueValid(
			cached = cached,
			isSampled = true,
			request = request,
			size = PixelSize(100, 100)
		))
		assertTrue(interceptor.isCachedValueValid(
			cached = cached,
			isSampled = true,
			request = request,
			size = PixelSize(50, 100)
		))
		assertTrue(interceptor.isCachedValueValid(
			cached = cached,
			isSampled = true,
			request = request,
			size = PixelSize(50, 50)
		))
		assertTrue(interceptor.isCachedValueValid(
			cached = createBitmap(width = 400, height = 200),
			isSampled = true,
			request = request,
			size = PixelSize(400, 200)
		))
	}

	@Test
	fun `isCachedValueValid - fit`() {
		val interceptor = newInterceptor()
		val request = createRequest(context) {
			size(100, 100)
			precision(Precision.INEXACT)
			scale(Scale.FIT)
		}
		val cached = createBitmap()
		assertFalse(interceptor.isCachedValueValid(
			cached = cached,
			isSampled = true,
			request = request,
			size = PixelSize(200, 200)
		))
		assertTrue(interceptor.isCachedValueValid(
			cached = cached,
			isSampled = true,
			request = request,
			size = PixelSize(150, 50)
		))
		assertTrue(interceptor.isCachedValueValid(
			cached = cached,
			isSampled = true,
			request = request,
			size = PixelSize(100, 100)
		))
		assertTrue(interceptor.isCachedValueValid(
			cached = cached,
			isSampled = true,
			request = request,
			size = PixelSize(50, 100)
		))
		assertTrue(interceptor.isCachedValueValid(
			cached = cached,
			isSampled = true,
			request = request,
			size = PixelSize(50, 50)
		))
		assertFalse(interceptor.isCachedValueValid(
			cached = createBitmap(width = 200, height = 400),
			isSampled = true,
			request = request,
			size = PixelSize(400, 800)
		))
	}

	@Test
	fun `isCachedValueValid - small not sampled cached drawable is valid`() {
		val interceptor = newInterceptor()
		val cached = createBitmap()
		val isValid = interceptor.isCachedValueValid(
			cached = cached,
			isSampled = false,
			request = createRequest(context) {
				precision(Precision.INEXACT)
				scale(Scale.FILL)
			},
			size = PixelSize(200, 200)
		)
		assertTrue(isValid)
	}

	@Test
	fun `isCachedValueValid - allowHardware=false prevents using cached hardware bitmap`() {
		val interceptor = newInterceptor()
		fun isBitmapConfigValid(config: Bitmap.Config): Boolean {
			val cached = createBitmap(config = config)
			return interceptor.isCachedValueValid(
				cached = cached,
				isSampled = true,
				request = createRequest(context) {
					allowHardware(false)
					scale(Scale.FILL)
				},
				size = PixelSize(100, 100)
			)
		}

		assertFalse(isBitmapConfigValid(HARDWARE))
		assertTrue(isBitmapConfigValid(RGBA_F16))
		assertTrue(isBitmapConfigValid(ARGB_8888))
		assertTrue(isBitmapConfigValid(RGB_565))
	}

	@Test
	fun `isCachedValueValid - exact precision`() {
		val interceptor = newInterceptor()
		assertFalse(interceptor.isCachedValueValid(
			cached = createBitmap(width = 100, height = 100),
			isSampled = true,
			request = createRequest(context) {
				precision(Precision.EXACT)
				scale(Scale.FILL)
			},
			size = PixelSize(50, 50)
		))
		assertFalse(interceptor.isCachedValueValid(
			cached = createBitmap(width = 100, height = 100),
			isSampled = true,
			request = createRequest(context) {
				precision(Precision.EXACT)
				scale(Scale.FIT)
			},
			size = PixelSize(50, 50)
		))
		assertTrue(interceptor.isCachedValueValid(
			cached = createBitmap(width = 100, height = 100),
			isSampled = true,
			request = createRequest(context) {
				precision(Precision.EXACT)
				scale(Scale.FILL)
			},
			size = PixelSize(100, 50)
		))
		assertFalse(interceptor.isCachedValueValid(
			cached = createBitmap(width = 100, height = 100),
			isSampled = true,
			request = createRequest(context) {
				precision(Precision.EXACT)
				scale(Scale.FIT)
			},
			size = PixelSize(100, 50)
		))
		assertTrue(interceptor.isCachedValueValid(
			cached = createBitmap(width = 100, height = 100),
			isSampled = true,
			request = createRequest(context) {
				precision(Precision.EXACT)
				scale(Scale.FILL)
			},
			size = PixelSize(100, 100)
		))
		assertTrue(interceptor.isCachedValueValid(
			cached = createBitmap(width = 100, height = 100),
			isSampled = true,
			request = createRequest(context) {
				precision(Precision.EXACT)
				scale(Scale.FIT)
			},
			size = PixelSize(100, 100)
		))
		assertTrue(interceptor.isCachedValueValid(
			cached = createBitmap(width = 400, height = 200),
			isSampled = true,
			request = createRequest(context) {
				precision(Precision.EXACT)
				scale(Scale.FILL)
			},
			size = PixelSize(400, 200)
		))
		assertFalse(interceptor.isCachedValueValid(
			cached = createBitmap(width = 200, height = 400),
			isSampled = true,
			request = createRequest(context) {
				precision(Precision.EXACT)
				scale(Scale.FIT)
			},
			size = PixelSize(400, 800)
		))
	}

	@Test
	fun `isCachedValueValid - one pixel off`() {
		val interceptor = newInterceptor()
		assertTrue(interceptor.isCachedValueValid(
			cached = createBitmap(width = 244, height = 600),
			isSampled = true,
			request = createRequest(context) {
				precision(Precision.EXACT)
				scale(Scale.FIT)
			},
			size = PixelSize(245, 600)
		))
		assertTrue(interceptor.isCachedValueValid(
			cached = createBitmap(width = 244, height = 600),
			isSampled = true,
			request = createRequest(context) {
				precision(Precision.INEXACT)
				scale(Scale.FIT)
			},
			size = PixelSize(245, 600)
		))
		assertFalse(interceptor.isCachedValueValid(
			cached = createBitmap(width = 243, height = 595),
			isSampled = true,
			request = createRequest(context) {
				precision(Precision.EXACT)
				scale(Scale.FIT)
			},
			size = PixelSize(245, 600)
		))
		assertFalse(interceptor.isCachedValueValid(
			cached = createBitmap(width = 243, height = 595),
			isSampled = true,
			request = createRequest(context) {
				precision(Precision.INEXACT)
				scale(Scale.FIT)
			},
			size = PixelSize(245, 600)
		))
		// Regression test: https://github.com/coil-kt/coil/issues/817
		assertTrue(interceptor.isCachedValueValid(
			cached = createBitmap(width = 175, height = 117),
			isSampled = true,
			request = createRequest(context) {
				precision(Precision.INEXACT)
				scale(Scale.FIT)
			},
			size = PixelSize(176, 176)
		))
	}

	@Test
	fun `isCachedValueValid - transformation that reduces size of output bitmap`() {
		val interceptor = newInterceptor()
		val key = newMemoryCacheKey(
			key = "key",
			transformations = listOf(CircleCropTransformation()),
			size = PixelSize(1000, 500) // The size of the previous request.
		)
		val value = Value(
			bitmap = createBitmap(width = 200, height = 200), // The small cached bitmap.
			extras = mapOf(EXTRA_IS_SAMPLED to true)
		)
		val request = createRequest(context)

		assertTrue(interceptor.isCachedValueValid(
			cacheKey = key,
			cacheValue = value,
			request = request.newBuilder().precision(Precision.INEXACT).scale(Scale.FIT).build(),
			size = PixelSize(650, 400)
		))

		assertTrue(interceptor.isCachedValueValid(
			cacheKey = key,
			cacheValue = value,
			request = request.newBuilder().precision(Precision.EXACT).scale(Scale.FIT).build(),
			size = PixelSize(1000, 500)
		))

		assertFalse(interceptor.isCachedValueValid(
			cacheKey = key,
			cacheValue = value,
			request = request.newBuilder().precision(Precision.INEXACT).scale(Scale.FIT).build(),
			size = PixelSize(1500, 1000)
		))

		assertFalse(interceptor.isCachedValueValid(
			cacheKey = key,
			cacheValue = value,
			request = request.newBuilder().precision(Precision.EXACT).scale(Scale.FIT).build(),
			size = PixelSize(800, 500)
		))
	}

	private fun EngineInterceptor.isCachedValueValid(
		cached: Bitmap,
		isSampled: Boolean,
		request: ImageRequest,
		size: Size
	) = isCachedValueValid(Key("key"), Value(cached, mapOf(EXTRA_IS_SAMPLED to isSampled)), request, size)

	@Test
	fun `applyTransformations - transformations convert drawable to bitmap`() {
		val interceptor = newInterceptor()
		val drawable = ColorDrawable(Color.BLACK)
		val size = PixelSize(100, 100)
		val result = runBlocking {
			interceptor.transform(
				result = ExecuteResult(
					drawable = drawable,
					isSampled = false,
					dataSource = DataSource.MEMORY,
					diskCacheKey = null
				),
				request = createRequest(context) { transformations(CircleCropTransformation()) },
				options = Options(context, size = size),
				eventListener = EventListener.NONE
			)
		}

		val resultDrawable = result.drawable
		assertTrue(resultDrawable is BitmapDrawable)
		assertEquals(resultDrawable.bitmap.size, size)
	}

	@Test
	fun `applyTransformations - empty transformations does not convert drawable to bitmap`() {
		val interceptor = newInterceptor()
		val drawable = ColorDrawable(Color.BLACK)
		val result = runBlocking {
			interceptor.transform(
				result = ExecuteResult(
					drawable = drawable,
					isSampled = false,
					dataSource = DataSource.MEMORY,
					diskCacheKey = null
				),
				request = createRequest(context) { transformations(emptyList()) },
				options = Options(context, size = PixelSize(100, 100)),
				eventListener = EventListener.NONE
			)
		}

		assertSame(drawable, result.drawable)
	}

	private fun createFakeTransformations(): List<Transformation> {
		return listOf(
			object : Transformation {
				override val cacheKey = "key1"
				override suspend fun transform(input: Bitmap, size: Size) = fail()
			},
			object : Transformation {
				override val cacheKey = "key2"
				override suspend fun transform(input: Bitmap, size: Size) = fail()
			}
		)
	}

	private fun createFakeParameters(): Parameters {
		return Parameters.Builder()
			.set("key1", "no_cache", cacheKey = null)
			.set("key2", "cached2")
			.set("key3", "cached3")
			.build()
	}

	private fun newInterceptor(key: String? = TEST_KEY): EngineInterceptor {
		val imageLoader = ImageLoader.Builder(context)
			.components {
				add(Keyer { _: Any, _ -> key })
			}
			.build()
		val systemCallbacks = SystemCallbacks(imageLoader as RealImageLoader, context, true)
		return EngineInterceptor(
			imageLoader = imageLoader,
			requestService = RequestService(imageLoader, systemCallbacks, null),
			logger = null
		)
	}

	private fun newMemoryCacheKey(
		key: String = TEST_KEY,
		transformations: List<Transformation> = emptyList(),
		size: Size = OriginalSize,
		parameters: Parameters = Parameters.EMPTY
	): Key {
		val extras = buildMap<String, String> {
			if (transformations.isNotEmpty()) {
				val transformationString = transformations.joinToString(separator = "") {
					it.cacheKey + TRANSFORMATIONS_DELIMITER
				}
				put(MEMORY_CACHE_KEY_TRANSFORMATIONS, transformationString)
			}
			if (size is PixelSize) {
				put(MEMORY_CACHE_KEY_WIDTH, size.width.toString())
				put(MEMORY_CACHE_KEY_HEIGHT, size.height.toString())
			}
			putAll(parameters.cacheKeys())
		}
		return Key(key, extras)
	}

	companion object {
		private const val TEST_KEY = "test_key"
	}
}