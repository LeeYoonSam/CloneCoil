package com.ys.coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import com.ys.coil.ImageLoader
import com.ys.coil.decode.DataSource.DISK
import com.ys.coil.fetch.SourceResultNew
import com.ys.coil.request.Options
import com.ys.coil.size.PixelSize
import com.ys.coil.size.Scale
import com.ys.coil.size.Size
import com.ys.coil.util.size
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.source
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BitmapFactoryDecoderNewTest {
	private lateinit var context: Context
	private lateinit var decoderFactory: BitmapFactoryDecoderNew.Factory

	@Before
	fun before() {
		context = ApplicationProvider.getApplicationContext()
		decoderFactory = BitmapFactoryDecoderNew.Factory()
	}

	@Test
	fun basic() {
		val (drawable, isSample) = decode(
			assetName = "normal.jpg",
			size = PixelSize(100, 100)
		)

		assertTrue(isSample)
		assertTrue(drawable is BitmapDrawable)
		assertEquals(PixelSize(100, 125), drawable.bitmap.size)
		assertEquals(Bitmap.Config.ARGB_8888, drawable.bitmap.config)
	}

	@Test
	fun malformedImageThrows() {
		assertFailsWith<IllegalStateException> {
			decode(
				assetName = "malformed.jpg",
				size = PixelSize(100, 100)
			)
		}
	}

	private fun decodeBitmap(assetName: String, size: Size): Bitmap =
		decodeBitmap(assetName, Options(context = context, size = size, scale = Scale.FILL))

	private fun decodeBitmap(assetName: String, options: Options): Bitmap =
		(decode(assetName, options).drawable as BitmapDrawable).bitmap

	private fun decode(assetName: String, size: Size): DecodeResult =
		decode(assetName, Options(context = context, size = size, scale = Scale.FILL))

	private fun decode(assetName: String, options: Options): DecodeResult = runBlocking {
		val source = context.assets.open(assetName).source().buffer()
		val decoder = decoderFactory.create(
			result = SourceResultNew(
				source = ImageSource(source, context),
				mimeType = null,
				dataSource = DISK
			),
			options = options,
			imageLoader = ImageLoader(context)
		)
		val result = checkNotNull(decoder.decode())

		// Assert that the source has been closed.
		val exception = assertFailsWith<IllegalStateException> { source.exhausted() }
		assertEquals("closed", exception.message)

		return@runBlocking result
	}
}