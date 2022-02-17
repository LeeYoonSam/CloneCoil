package com.ys.coil.decode

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import com.ys.coil.ImageLoader
import com.ys.coil.fetch.SourceResult
import com.ys.coil.request.Options
import com.ys.coil.size.PixelSize
import com.ys.coil.size.Scale
import com.ys.coil.test.util.assertIsSimilarTo
import com.ys.coil.test.util.decodeBitmapAsset
import kotlinx.coroutines.runBlocking
import okio.BufferedSource
import okio.buffer
import okio.source
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SvgDecoderTest {

	private lateinit var context: Context
	private lateinit var decoderFactory: SvgDecoder.Factory

	@Before
	fun before() {
		context = ApplicationProvider.getApplicationContext()
		decoderFactory = SvgDecoder.Factory()
	}

	@Test
	fun handlesSvgMimeType() {
		val result = context.assets.open("coil_logo.svg").source().buffer()
			.asSourceResult(mimeType = "image/svg+xml")
		assertNotNull(decoderFactory.create(result, Options(context), ImageLoader(context)))
	}

	@Test
	fun doesNotHandlePngMimeType() {
		val result = context.assets.open("coil_logo_250.png").source().buffer()
			.asSourceResult(mimeType = "image/png")
		assertNull(decoderFactory.create(result, Options(context), ImageLoader(context)))
	}

	@Test
	fun doesNotHandleGeneralXmlFile() {
		val source = context.assets.open("document.xml").source().buffer()
		val result = source.asSourceResult()
		assertNull(decoderFactory.create(result, Options(context), ImageLoader(context)))
		assertFalse(source.exhausted())
		assertEquals(8192, source.buffer.size) // should buffer exactly 1 segment
	}

	@Test
	fun handlesSvgSource() {
		val options = Options(context)
		val imageLoader = ImageLoader(context)
		var source = context.assets.open("coil_logo.svg").source().buffer()
		assertNotNull(decoderFactory.create(source.asSourceResult(), options, imageLoader))

		source = context.assets.open("coil_logo_250.png").source().buffer()
		assertNull(decoderFactory.create(source.asSourceResult(), options, imageLoader))

		source = context.assets.open("instacart_logo.svg").source().buffer()
		assertNotNull(decoderFactory.create(source.asSourceResult(), options, imageLoader))

		source = context.assets.open("instacart_logo_326.png").source().buffer()
		assertNull(decoderFactory.create(source.asSourceResult(), options, imageLoader))
	}

	@Test
	fun basic() {
		val source = context.assets.open("coil_logo.svg").source().buffer()
		val (drawable, isSampled) = runBlocking {
			val options = Options(
				context = context,
				size = PixelSize(450, 250), // coil_logo.svg의 고유 치수는 200x200입니다.
				scale = Scale.FIT
			)
			assertNotNull(decoderFactory.create(source.asSourceResult(), options, ImageLoader(context))?.decode())
		}

		assertTrue(isSampled)
		assertTrue(drawable is BitmapDrawable)

		val expected = context.decodeBitmapAsset("coil_logo_250.png")
		drawable.bitmap.assertIsSimilarTo(expected)
	}

	@Test
	fun noViewBox() {
		val source = context.assets.open("instacart_logo.svg").source().buffer()
		val (drawable, isSampled) = runBlocking {
			val options = Options(
				context = context,
				size = PixelSize(326, 50),
				scale = Scale.FILL
			)
			assertNotNull(decoderFactory.create(source.asSourceResult(), options, ImageLoader(context))?.decode())
		}

		assertTrue(isSampled)
		assertTrue(drawable is BitmapDrawable)

		val expected = context.decodeBitmapAsset("instacart_logo_326.png")
		drawable.bitmap.assertIsSimilarTo(expected)
	}

	private fun BufferedSource.asSourceResult(
		mimeType: String? = null,
		dataSource: DataSource = DataSource.DISK
	): SourceResult {
		return SourceResult(
			source = ImageSource(this, context),
			mimeType = mimeType,
			dataSource = dataSource
		)
	}
}
