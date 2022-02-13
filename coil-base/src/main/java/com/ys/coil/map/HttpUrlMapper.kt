package com.ys.coil.map

import com.ys.coil.request.Options
import okhttp3.HttpUrl

class HttpUrlMapper : Mapper<HttpUrl, String> {
	override fun map(data: HttpUrl, options: Options) = data.toString()
}
