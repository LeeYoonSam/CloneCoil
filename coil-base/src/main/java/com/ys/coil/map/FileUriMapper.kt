package com.ys.coil.map

import android.content.ContentResolver
import android.net.Uri
import com.ys.coil.fetch.AssetUriFetcher
import com.ys.coil.request.Options
import com.ys.coil.util.firstPathSegment
import java.io.File

class FileUriMapper : Mapper<Uri, File> {
	override fun map(data: Uri, options: Options): File? {
		if (!isApplicable(data)) return null
		return File(checkNotNull(data.path))
	}

	private fun isApplicable(data: Uri): Boolean {
		if (isAssetUri(data)) return false

		return data.scheme.let { it == null || it == ContentResolver.SCHEME_FILE } &&
			data.path.orEmpty().startsWith('/') && data.firstPathSegment!= null
	}

	private fun isAssetUri(data: Uri): Boolean {
		return data.scheme == ContentResolver.SCHEME_FILE &&
			data.firstPathSegment == AssetUriFetcher.ASSET_FILE_PATH_ROOT
	}
}