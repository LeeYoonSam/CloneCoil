package com.ys.coil.fetch

import android.webkit.MimeTypeMap
import com.ys.coil.ImageLoader
import com.ys.coil.decode.DataSource
import com.ys.coil.decode.ImageSource
import com.ys.coil.request.Options
import java.io.File

internal class FileFetcher(private val data: File) : Fetcher {

	override suspend fun fetch(): FetchResult {
		return SourceResult(
			source = ImageSource(file = data),
			mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(data.extension),
			dataSource = DataSource.DISK
		)
	}

	class Factory : Fetcher.Factory<File> {
		override fun create(data: File, options: Options, imageLoader: ImageLoader): Fetcher? {
			return FileFetcher(data)
		}
	}
}