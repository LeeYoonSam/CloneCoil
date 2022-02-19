@file:JvmName("-VideoExtensions")

package com.ys.coil.util

import android.media.MediaMetadataRetriever
import android.os.Build.VERSION.SDK_INT

/** [MediaMetadataRetriever]는 API 29까지 [AutoCloseable]을 구현하지 않습니다. */
internal inline fun <T> MediaMetadataRetriever.use(block: (MediaMetadataRetriever) -> T): T {
	try {
		return block(this)
	} finally {
		// 엄격 모드 경고를 피하기 위해 API 29+에서 'close'를 호출해야 합니다.
		if (SDK_INT >= 29) {
			close()
		} else {
			release()
		}
	}
}
