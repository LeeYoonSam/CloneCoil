package com.ys.coil.fetch

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts
import androidx.annotation.VisibleForTesting
import com.ys.coil.ImageLoader
import com.ys.coil.decode.DataSource
import com.ys.coil.decode.ImageSource
import com.ys.coil.request.Options
import okio.buffer
import okio.source
import java.io.InputStream

internal class ContentUriFetcher(
	private val data: Uri,
	private val options: Options
) : Fetcher {
	override suspend fun fetch(): FetchResult {
		val context = options.context
		val inputStream = if (isContactPhotoUri(data)) {

			// ContactsContract.Contacts.openContactPhotoInputStream에서 수정되었습니다.
			val stream: InputStream? = context.contentResolver
				.openAssetFileDescriptor(data, "r")?.createInputStream()

			checkNotNull(stream) { "Unable to find a contact photo associated with '$data'." }
		} else {
			val stream: InputStream? = context.contentResolver.openInputStream(data)
			checkNotNull(stream) { "Unable to open '$data'." }
		}

		return SourceResult(
			source = ImageSource(inputStream.source().buffer(), options.context),
			mimeType = context.contentResolver.getType(data),
			dataSource = DataSource.DISK
		)
	}

	/**
	 * 연락처 사진은 [ContentResolver.openAssetFileDescriptor]를 사용하여 로드해야 하는 콘텐츠 URI의 특수한 경우입니다.
	 */
	@VisibleForTesting
	internal fun isContactPhotoUri(data: Uri): Boolean {
		return data.authority == ContactsContract.AUTHORITY &&
			data.lastPathSegment == Contacts.Photo.DISPLAY_NAME
	}

	class Factory : Fetcher.Factory<Uri> {
		override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
			if (!isApplicable(data)) return null
			return ContentUriFetcher(data, options)
		}

		private fun isApplicable(data: Uri): Boolean {
			return data.scheme == ContentResolver.SCHEME_CONTENT
		}
	}
}
