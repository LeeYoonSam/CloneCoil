package com.ys.coil.disk

import android.content.Context
import android.os.StatFs
import androidx.annotation.FloatRange
import java.io.Closeable
import java.io.File

/**
 * 이전에 로드된 이미지의 온디스크 캐시입니다.
 */
interface DiskCache {
	/** 캐시의 현재 크기(바이트)입니다. */
	val size: Long

	/** 캐시의 최대 크기(바이트)입니다. */
	val maxSize: Long

	/** 캐시가 데이터를 저장하는 디렉토리입니다. */
	val directory: File

	/**
	 * [key]와 관련된 항목을 가져옵니다.
	 *
	 * 중요: **완료되면 **[Snapshot.close] 또는 [Snapshot.closeAndEdit]를 호출해야 합니다.
	 * 스냅샷을 읽고 있습니다. 열린 스냅샷은 항목을 편집하거나 디스크에서 삭제할 수 없습니다.
	 */
	operator fun get(key: String): Snapshot?

	/**
	 * [key]와 관련된 항목을 편집합니다.
	 *
	 * 중요: **편집을 완료하려면 **[Editor.commit], [Editor.commitAndGet] 또는 [Editor.abort] 중 하나를 호출해야 합니다.
	 * 열려 있는 편집기는 새 [Snapshot]을 열거나 새 [Editor]를 열 수 없습니다.
	 */
	fun edit(key: String): Editor?

	/**
	 * [key]에서 참조하는 항목을 삭제합니다.
	 *
	 * @return 'true'는 [key]가 성공적으로 제거된 경우입니다. 그렇지 않으면 'false'를 반환합니다.
	 */
	fun remove(key: String): Boolean

	/** 디스크 캐시의 모든 항목을 삭제합니다. */
	fun clear()

	/**
	 * 항목 값의 스냅샷입니다.
	 *
	 * 중요: [metadata] 또는 [data]를 **만 읽어야 합니다**. 두 파일 중 하나를 변경하면 파일이 손상될 수 있습니다.
	 * 디스크 캐시. 해당 파일의 내용을 수정하려면 [edit]을 사용하십시오.
	 */
	interface Snapshot : Closeable {

		/** 이미지의 메타데이터를 가져옵니다. */
		val metadata: File

		/** 원시 이미지 데이터를 가져옵니다. */
		val data: File

		/** 편집을 허용하려면 스냅샷을 닫습니다. */
		override fun close()

		/** 스냅샷을 닫고 이 항목에 대해 원자적으로 [edit]을 호출합니다. */
		fun closeAndEdit(): Editor?
	}

	/**
	 * 항목의 값을 편집합니다.
	 *
	 * [metadata] 또는 [data]를 호출하면 해당 파일을 더티로 표시하여 디스크에 유지됩니다.
	 * 이 편집기가 커밋된 경우.
	 *
	 * 중요: [metadata] 또는 [data]의 **내용만 읽거나 수정**해야 합니다.
	 * 이름 바꾸기, 잠금 또는 기타 파일 변경 작업은 디스크 캐시를 손상시킬 수 있습니다.
	 */
	interface Editor {

		/** 이미지의 메타데이터를 가져옵니다. */
		val metadata: File

		/** 원시 이미지 데이터를 가져옵니다. */
		val data: File

		/** 변경 사항이 독자에게 표시되도록 편집을 커밋합니다. */
		fun commit()

		/** 편집을 커밋하고 새 [Snapshot]을 원자적으로 엽니다. */
		fun commitAndGet(): Snapshot?

		/** 편집을 중단합니다. 작성된 모든 데이터는 폐기됩니다. */
		fun abort()
	}

	class Builder(private val context: Context) {

		private var directory: File? = null
		private var maxSizePercent = 0.02 // 2%
		private var minimumMaxSizeBytes = 10L * 1024 * 1024 // 10MB
		private var maximumMaxSizeBytes = 250L * 1024 * 1024 // 250MB
		private var maxSizeBytes = 0L

		/**
		 * 캐시가 데이터를 저장하는 [directory]를 설정합니다.
		 *
		 * 중요: 동일한 디렉토리에서 동시에 두 개의 [DiskCache] 인스턴스를 활성화하면 디스크 캐시가 손상될 수 있으므로 오류입니다.
		 */
		fun directory(directory: File) = apply {
			this.directory = directory
		}

		/**
		 * 디스크 캐시의 최대 크기를 장치의 여유 디스크 공간에 대한 백분율로 설정합니다.
		 */
		fun maxSizePercent(@FloatRange(from = 0.0, to = 1.0) percent: Double) = apply {
			require(percent in 0.0..1.0) { "size must be in the range [0.0, 1.0]." }
			this.maxSizeBytes = 0
			this.maxSizePercent = percent
		}

		/**
		 * 디스크 캐시의 최소 크기를 바이트 단위로 설정합니다.
		 * [maxSizeBytes]가 설정되면 무시됩니다.
		 */
		fun minimumMaxSizeBytes(size: Long) = apply {
			require(size > 0) { "size must be > 0." }
			this.minimumMaxSizeBytes = size
		}

		/**
		 * 디스크 캐시의 최대 크기를 바이트 단위로 설정합니다.
		 * [maxSizeBytes]가 설정되면 무시됩니다.
		 */
		fun maximumMaxSizeBytes(size: Long) = apply {
			require(size > 0) { "size must be > 0." }
			this.maximumMaxSizeBytes = size
		}

		/**
		 * 디스크 캐시의 최대 크기를 바이트 단위로 설정합니다.
		 */
		fun maxSizeBytes(size: Long) = apply {
			require(size > 0) { "size must be > 0." }
			this.maxSizePercent = 0.0
			this.maxSizeBytes = size
		}

		/**
		 * 새 [DiskCache] 인스턴스를 만듭니다.
		 */
		fun build(): DiskCache {
			val directory = checkNotNull(directory) { "directory == null" }
			val maxSize = if (maxSizePercent > 0) {
				try {
					val stats = StatFs(directory.absolutePath)
					val size = maxSizePercent * stats.blockCountLong * stats.blockSizeLong
					size.toLong().coerceIn(minimumMaxSizeBytes, maximumMaxSizeBytes)
				} catch (_: Exception) {
					minimumMaxSizeBytes
				}
			} else {
				maxSizeBytes
			}

			return RealDiskCache(
				maxSize = maxSize,
				directory = directory
			)
		}
	}
}
