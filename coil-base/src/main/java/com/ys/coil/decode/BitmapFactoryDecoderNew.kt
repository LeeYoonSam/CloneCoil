package com.ys.coil.decode

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build.VERSION
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import com.ys.coil.ImageLoader
import com.ys.coil.fetch.SourceResult
import com.ys.coil.request.Options
import com.ys.coil.size.PixelSize
import com.ys.coil.util.toDrawable
import com.ys.coil.util.toSoftware
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.InputStream
import kotlin.math.roundToInt

class BitmapFactoryDecoderNew @JvmOverloads constructor(
    private val source: ImageSource,
    private val options: Options,
    private val parallelismLock: Semaphore = Semaphore(Int.MAX_VALUE)
) : DecoderNew {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    override suspend fun decode() = parallelismLock.withPermit {
        runInterruptible { BitmapFactory.Options().decode() }
    }

    private fun BitmapFactory.Options.decode(): DecodeResult {
        val safeSource = ExceptionCatchingSource(source.source())
        val safeBufferedSource = safeSource.buffer()

        // 이미지의 치수를 읽습니다.
        inJustDecodeBounds = true
        BitmapFactory.decodeStream(safeBufferedSource.peek().inputStream(), null, this)
        safeSource.exception?.let { throw it }
        inJustDecodeBounds = false

        // 이미지의 EXIF 데이터를 읽습니다.
        val isFlipped: Boolean
        val rotationDegrees: Int

        if (shouldReadExifData(outMimeType)) {
            val inputStream = safeBufferedSource.peek().inputStream()
            val exifInterface = ExifInterface(ExifInterfaceInputStream(inputStream))
            safeSource.exception?.let { throw it }
            isFlipped = exifInterface.isFlipped
            rotationDegrees = exifInterface.rotationDegrees
        } else {
            isFlipped = false
            rotationDegrees = 0
        }

        // srcWidth 및 srcHeight는 EXIF 변환 후(그러나 샘플링 전) 이미지의 크기입니다.
        val isSwapped = rotationDegrees == 90 || rotationDegrees == 270
        val srcWidth = if (isSwapped) outHeight else outWidth
        val srcHeight = if (isSwapped) outWidth else outHeight

        inPreferredConfig = computeConfig(options, isFlipped, rotationDegrees)
        inPremultiplied = options.premultipliedAlpha

        if (VERSION.SDK_INT >= 26 && options.colorSpace != null) {
            inPreferredColorSpace = options.colorSpace
        }

        // 성능상의 이점이 있으므로 항상 변경할 수 없는 비트맵을 만듭니다.
        inMutable = false
        inScaled = false

        when {
            outWidth <= 0 || outHeight <= 0 -> {
                // 이것은 이미지의 크기를 디코딩하는 동안 오류가 발생한 경우에 발생합니다.
                inSampleSize = 1
                inScaled = false
                inBitmap = null
            }
            options.size !is PixelSize -> {
                // 크기가 OriginalSize인 경우에 발생합니다.
                inSampleSize = 1
                inScaled = false
            }
            else -> {
                val (width, height) = options.size
                inSampleSize = DecodeUtils
                    .calculateInSampleSize(srcWidth, srcHeight, width, height, options.scale)

                // 이미지의 밀도 배율을 계산합니다.
                val rawScale = DecodeUtils.computeSizeMultiplier(
                    srcWidth = srcWidth / inSampleSize.toDouble(),
                    srcHeight = srcHeight / inSampleSize.toDouble(),
                    dstWidth = width.toDouble(),
                    dstHeight = height.toDouble(),
                    scale = options.scale
                )

                // 허용되는 경우 원래 크기보다 큰 이미지를 로드하지 마십시오.
                val scale = if (options.allowInexactSize) {
                    rawScale.coerceAtMost(1.0)
                } else {
                    rawScale
                }

                inScaled = scale != 1.0
                if (inScaled) {
                    if (scale > 1) {
                        // 확대
                        inDensity = (Int.MAX_VALUE / scale).roundToInt()
                        inTargetDensity = Int.MAX_VALUE
                    } else {
                        // 축소
                        inDensity = Int.MAX_VALUE
                        inTargetDensity = (Int.MAX_VALUE * scale).roundToInt()
                    }
                }
            }
        }

        // 비트맵을 디코딩합니다.
        val outBitmap: Bitmap? = safeBufferedSource.use {
            BitmapFactory.decodeStream(it.inputStream(), null, this)
        }
        safeSource.exception?.let { throw it }
        checkNotNull(outBitmap) {
            "BitmapFactory returned a null bitmap. Often this means BitmapFactory could not " +
                "decode the image data read from the input source (e.g. network, disk, or " +
                "memory) as it's not encoded as a valid image format."
        }

        // inDensity/inTargetDensity를 오버로드하여 잘못 생성된 밀도를 수정합니다.
        outBitmap.density = options.context.resources.displayMetrics.densityDpi

        // Apply any EXIF transformations.
        val bitmap = applyExifTransformations(outBitmap, inPreferredConfig, isFlipped, rotationDegrees)

        return DecodeResult(
            drawable = bitmap.toDrawable(options.context),
            isSampled = inSampleSize > 1 || inScaled
        )
    }

    /** 이미지의 EXIF 데이터를 읽어야 하는 경우 'true'를 반환합니다. */
    private fun shouldReadExifData(mimeType: String?): Boolean {
        return mimeType != null && mimeType in SUPPORTED_EXIF_MIME_TYPES
    }

    /** [BitmapFactory.Options.inPreferredConfig]를 계산하고 반환합니다. */
    private fun BitmapFactory.Options.computeConfig(
        options: Options,
        isFlipped: Boolean,
        rotationDegrees: Int
    ): Bitmap.Config {
        var config = options.config

        // EXIF 변환을 수행해야 하는 경우 하드웨어 비트맵을 비활성화합니다.
        if (isFlipped || rotationDegrees > 0) {
            config = config.toSoftware()
        }

        // 허용되는 경우 최적화로 이미지를 RGB_565로 디코딩합니다.
        if (options.allowRgb565 && config == Bitmap.Config.ARGB_8888 && outMimeType == MIME_TYPE_JPEG) {
            config = Bitmap.Config.RGB_565
        }

        // 높은 색상 깊이 이미지는 RGBA_F16 또는 HARDWARE로 디코딩되어야 합니다.
        if (VERSION.SDK_INT >= 26 && outConfig == Bitmap.Config.RGBA_F16 && config != Bitmap.Config.HARDWARE) {
            config = Bitmap.Config.RGBA_F16
        }

        return config
    }

    /** 참고: 이 방법은 이미지를 변환해야 하는 경우 [config]가 [Bitmap.Config.HARDWARE]가 아니라고 가정합니다. */
    private fun applyExifTransformations(
        inBitmap: Bitmap,
        config: Bitmap.Config,
        isFlipped: Boolean,
        rotationDegrees: Int
    ): Bitmap {
        // 적용할 변환이 없는 경우 단락.
        val isRotated = rotationDegrees > 0
        if (!isFlipped && !isRotated) {
            return inBitmap
        }

        val matrix = Matrix()
        val centerX = inBitmap.width / 2f
        val centerY = inBitmap.height / 2f
        if (isFlipped) {
            matrix.postScale(-1f, 1f, centerX, centerY)
        }
        if (isRotated) {
            matrix.postRotate(rotationDegrees.toFloat(), centerX, centerY)
        }

        val rect = RectF(0f, 0f, inBitmap.width.toFloat(), inBitmap.height.toFloat())
        matrix.mapRect(rect)
        if (rect.left != 0f || rect.top != 0f) {
            matrix.postTranslate(-rect.left, -rect.top)
        }

        val outBitmap = if (rotationDegrees == 90 || rotationDegrees == 270) {
            createBitmap(inBitmap.height, inBitmap.width, config)
        } else {
            createBitmap(inBitmap.width, inBitmap.height, config)
        }

        outBitmap.applyCanvas {
            drawBitmap(inBitmap, matrix, paint)
        }
        inBitmap.recycle()
        return outBitmap
    }

    class Factory @JvmOverloads constructor(
        maxParallelism: Int = DEFAULT_MAX_PARALLELISM
    ) : DecoderNew.Factory {

        private val parallelismLock = Semaphore(maxParallelism)

        override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): DecoderNew {
            return BitmapFactoryDecoderNew(result.source, options, parallelismLock)
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()
    }

    /** [BitmapFactory.decodeStream]이 [Exception]를 삼키는 것을 방지합니다. */
    private class ExceptionCatchingSource(delegate: Source) : ForwardingSource(delegate) {

        var exception: Exception? = null
            private set

        override fun read(sink: Buffer, byteCount: Long): Long {
            try {
                return super.read(sink, byteCount)
            } catch (e: Exception) {
                exception = e
                throw e
            }
        }
    }

    /** [delegate]를 [ExifInterface]와 함께 작동하도록 래핑합니다. */
    private class ExifInterfaceInputStream(private val delegate: InputStream) : InputStream() {

        // Ensure that this value is always larger than the size of the image
        // so ExifInterface won't stop reading the stream prematurely.
        private var availableBytes = GIGABYTE_IN_BYTES

        override fun read() = interceptBytesRead(delegate.read())

        override fun read(b: ByteArray) = interceptBytesRead(delegate.read(b))

        override fun read(b: ByteArray, off: Int, len: Int) =
            interceptBytesRead(delegate.read(b, off, len))

        override fun skip(n: Long) = delegate.skip(n)

        override fun available() = availableBytes

        override fun close() = delegate.close()

        private fun interceptBytesRead(bytesRead: Int): Int {
            if (bytesRead == -1) availableBytes = 0
            return bytesRead
        }
    }

    internal companion object {
        private const val MIME_TYPE_JPEG = "image/jpeg"
        private const val MIME_TYPE_WEBP = "image/webp"
        private const val MIME_TYPE_HEIC = "image/heic"
        private const val MIME_TYPE_HEIF = "image/heif"
        private const val GIGABYTE_IN_BYTES = 1024 * 1024 * 1024
        internal const val DEFAULT_MAX_PARALLELISM = 4

        // NOTE: We don't support PNG EXIF data as it's very rarely used and requires buffering
        // the entire file into memory. All of the supported formats short circuit when the EXIF
        // chunk is found (often near the top of the file).
        private val SUPPORTED_EXIF_MIME_TYPES =
            arrayOf(MIME_TYPE_JPEG, MIME_TYPE_WEBP, MIME_TYPE_HEIC, MIME_TYPE_HEIF)
    }
}