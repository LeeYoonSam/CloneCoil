package com.ys.coil.request

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import com.ys.coil.ImageLoader
import com.ys.coil.RealImageLoader
import com.ys.coil.size.Precision
import com.ys.coil.size.ViewSizeResolver
import com.ys.coil.target.ViewTarget
import com.ys.coil.util.SystemCallbacks
import com.ys.coil.util.allowInexactSize
import com.ys.coil.util.createRequest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class RequestServiceTest {

	private lateinit var context: Context
	private lateinit var service: RequestService

	@Before
	fun before() {
		context = ApplicationProvider.getApplicationContext()
		val imageLoader = ImageLoader(context) as RealImageLoader
		val systemCallbacks = SystemCallbacks(imageLoader, context, true)
		service = RequestService(imageLoader, systemCallbacks, null)
	}

	@Test
	fun `allowInexactSize - exact precision`() {
		val request = createRequest(context) {
			target(ImageView(context))
			precision(Precision.EXACT)
		}
		assertFalse(request.allowInexactSize)
	}

	@Test
	fun `allowInexactSize - inexact precision`() {
		val request = createRequest(context) {
			precision(Precision.INEXACT)
		}
		assertTrue(request.allowInexactSize)
	}

	@Test
	fun `allowInexactSize - ImageViewTarget`() {
		val request = createRequest(context) {
			target(ImageView(context))
			size(100, 100)
		}
		assertFalse(request.allowInexactSize)
	}

	@Test
	fun `allowInexactSize - ImageViewTarget explicit view size resolver`() {
		val request = createRequest(context) {
			val imageView = ImageView(context)
			target(imageView)
			size(ViewSizeResolver(imageView))
		}
		assertTrue(request.allowInexactSize)
	}

	@Test
	fun `allowInexactSize - ImageViewTarget explicit view size resolver, different views`() {
		val request = createRequest(context) {
			target(ImageView(context))
			size(ViewSizeResolver(ImageView(context)))
		}
		assertFalse(request.allowInexactSize)
	}

	@Test
	fun `allowInexactSize - OriginalSize`() {
		val request = createRequest(context)
		assertFalse(request.allowInexactSize)
	}

	@Test
	fun `allowInexactSize - CustomTarget`() {
		val request = createRequest(context) {
			target { /* Empty. */ }
			size(100, 100)
		}
		assertFalse(request.allowInexactSize)
	}

	@Test
	fun `allowInexactSize - CustomViewTarget`() {
		val request = createRequest(context) {
			target(object : ViewTarget<View> {
				override val view = View(context)
			})
		}
		assertFalse(request.allowInexactSize)
	}

	@Test
	fun `allowInexactSize - PixelSizeResolver explicit`() {
		val request = createRequest(context) {
			size(100, 100)
		}
		assertFalse(request.allowInexactSize)
	}
}
