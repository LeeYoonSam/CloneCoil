package com.ys.coil.key

import android.content.ContentResolver
import android.net.Uri
import com.ys.coil.request.Options
import com.ys.coil.util.nightMode

internal class UriKeyer : Keyer<Uri> {
	override fun key(data: Uri, options: Options): String? {
		// 'android.resource' uris는 야간 모드가 활성화/비활성화된 경우 변경될 수 있습니다.
		return if (data.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE) {
			"$data-${options.context.resources.configuration.nightMode}"
		} else  {
			data.toString()
		}
	}
}