package com.ys.coil.interceptor

import android.graphics.drawable.ColorDrawable
import com.ys.coil.decode.DataSource
import com.ys.coil.interceptor.Interceptor.Chain
import com.ys.coil.request.ImageResult
import com.ys.coil.request.SuccessResult

class FakeInterceptor : Interceptor {

	override suspend fun interceptor(chain: Chain): ImageResult {
		return SuccessResult(
			drawable = ColorDrawable(),
			request = chain.request,
			dataSource = DataSource.MEMORY,
			memoryCacheKey = null,
			diskCacheKey = null,
			isSampled = false,
			isPlaceholderCached = false
		)
	}
}