package com.ys.coil.decode

import android.content.Context
import com.ys.coil.util.closeQuietly
import com.ys.coil.util.safeCacheDir
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.source
import java.io.Closeable
import java.io.File

/**
 * [File]이 지원하는 새 [ImageSource]를 만듭니다.
 *
 * @param file 읽을 파일.
 * @param diskCacheKey 디스크 캐시의 [File]에 대한 선택적 캐시 키입니다.
 * @param closeable 이미지 소스가 닫힐 때 닫힐 선택적 닫기 가능 참조입니다.
 */
@JvmOverloads
@JvmName("create")
fun ImageSource(
	file: File,
	diskCacheKey: String? = null,
	closeable: Closeable? = null
): ImageSource = FileImageSource(file, diskCacheKey, closeable)

/**
 * [BufferedSource]가 지원하는 새 [ImageSource]를 만듭니다.
 *
 * @param source 읽을 버퍼링된 소스입니다.
 * @param context 안전한 캐시 디렉토리를 확인하는 데 사용되는 컨텍스트입니다.
 */
@JvmName("create")
fun ImageSource(
	source: BufferedSource,
	context: Context
): ImageSource = SourceImageSource(source, context.safeCacheDir)

/**
 * [BufferedSource]가 지원하는 새 [ImageSource]를 만듭니다.
 *
 * @param source 읽을 버퍼링된 소스입니다.
 * @param cacheDirectory 임시 파일을 생성할 디렉토리
 * [ImageSource.file]이 호출되는 경우.
 */
@JvmName("create")
fun ImageSource(
	source: BufferedSource,
	cacheDirectory: File
): ImageSource = SourceImageSource(source, cacheDirectory)

/**
 * 디코딩할 이미지 데이터에 대한 액세스를 제공합니다.
 */
sealed class ImageSource : Closeable {
	/**
	 * 이 [ImageSource]를 읽기 위해 [BufferedSource]를 반환합니다.
	 */
	abstract fun source(): BufferedSource

	/**
	 * 이미 생성된 경우 이 [ImageSource]를 읽기 위해 [BufferedSource]를 반환합니다.
	 * 그렇지 않으면 'null'을 반환합니다.
	 */
	abstract fun sourceOrNull(): BufferedSource?

	/**
	 * 이 [ImageSource]의 데이터를 포함하는 [File]을 반환합니다.
	 * 이 이미지 소스가 [BufferedSource]에 의해 지원되는 경우 이 [ImageSource]의 데이터를 포함하는 임시 파일이 생성됩니다.
	 */
	abstract fun file(): File

	/**
	 * 이미 생성된 경우 이 [ImageSource]의 데이터를 포함하는 [File]을 반환합니다.
	 * 그렇지 않으면 'null'을 반환합니다.
	 */
	abstract fun fileOrNull(): File?
}

internal class FileImageSource(
	internal val file: File,
	internal val diskCacheKey: String?,
	private val closeable: Closeable?
) : ImageSource() {

	private var isClosed = false
	private var source: BufferedSource? = null

	@Synchronized
	override fun source(): BufferedSource {
		assertNotClosed()
		source?.let { return it }
		return file.source().buffer().also {
			source = it
		}
	}

	@Synchronized
	override fun sourceOrNull(): BufferedSource? {
		assertNotClosed()
		return source
	}

	@Synchronized
	override fun file(): File {
		assertNotClosed()
		return file
	}

	@Synchronized
	override fun fileOrNull() = file()

	@Synchronized
	override fun close() {
		isClosed = true
		source?.closeQuietly()
		closeable?.closeQuietly()
	}

	private fun assertNotClosed() {
		check(!isClosed) { "closed" }
	}
}

internal class SourceImageSource(
	source: BufferedSource,
	private val cacheDirectory: File
) : ImageSource() {

	private var isClosed = false
	private var source: BufferedSource? = source
	private var file: File? = null

	init {
		require(cacheDirectory.isDirectory) { "cacheDirectory must be a directory" }
	}

	@Synchronized
	override fun source(): BufferedSource {
		assertNotClosed()
		source?.let { return it }
		return file!!.source().buffer().also { source = it }
	}

	override fun sourceOrNull() = source()

	@Synchronized
	override fun file(): File {
		assertNotClosed()
		file?.let { return  it }

		// 소스를 임시 파일에 복사합니다.
		val tempFile = File.createTempFile("tmp", null, cacheDirectory)
		source!!.use { tempFile.sink().use(it::readAll) }
		source = null
		return tempFile.also { file = it }
	}

	@Synchronized
	override fun fileOrNull(): File? {
		assertNotClosed()
		return file
	}

	@Synchronized
	override fun close() {
		isClosed = true
		source?.closeQuietly()
		file?.delete()
	}

	private fun assertNotClosed() {
		check(!isClosed) { "closed" }
	}
}
