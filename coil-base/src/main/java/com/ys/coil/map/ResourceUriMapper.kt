package com.ys.coil.map

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toUri
import com.ys.coil.request.Options

class ResourceUriMapper : Mapper<Uri, Uri> {
	override fun map(data: Uri, options: Options): Uri? {
		if (!isApplicable(data)) return null

		val packageName = data.authority.orEmpty()
		val resource = options.context.packageManager.getResourcesForApplication(packageName)
		val (type, name) = data.pathSegments
		val id = resource.getIdentifier(name, type, packageName)
		check(id != 0) { "Invalid ${ContentResolver.SCHEME_ANDROID_RESOURCE} URI: $data" }

		return "${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/$id".toUri()
	}

	private fun isApplicable(data: Uri): Boolean {
		return data.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE &&
			!data.authority.isNullOrBlank() &&
			data.pathSegments.count() == 2
	}
}