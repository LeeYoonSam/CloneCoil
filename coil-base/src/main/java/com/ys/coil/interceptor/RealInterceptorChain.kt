package com.ys.coil.interceptor

import com.ys.coil.EventListener
import com.ys.coil.request.ImageRequest
import com.ys.coil.request.ImageResult
import com.ys.coil.request.NullRequestData
import com.ys.coil.size.Size

internal class RealInterceptorChain(
	val initialRequest: ImageRequest,
	val interceptors: List<Interceptor>,
	val index: Int,
	override val request: ImageRequest,
	override val size: Size,
	val eventListener: EventListener,
	val isPlaceholderCached: Boolean
) : Interceptor.Chain {

	override fun withSize(size: Size) = copy(size = size)

	override suspend fun proceed(request: ImageRequest): ImageResult {
		if (index > 0) checkRequest(request, interceptors[index - 1])
		val interceptor = interceptors[index]
		val next = copy(index = index + 1, request = request)
		val result = interceptor.interceptor(next)
		checkRequest(result.request, interceptor)
		return result
	}

	private fun checkRequest(request: ImageRequest, interceptor: Interceptor) {
		check(request.context === initialRequest.context) {
			"Interceptor '$interceptor' cannot modify the request's context."
		}
		check(request.data !== NullRequestData) {
			"Interceptor '$interceptor' cannot set the request's data to null."
		}
		check(request.target === initialRequest.target) {
			"Interceptor '$interceptor' cannot modify the request's target."
		}
		check(request.lifecycle === initialRequest.lifecycle) {
			"Interceptor '$interceptor' cannot modify the request's lifecycle."
		}
		check(request.sizeResolver === initialRequest.sizeResolver) {
			"Interceptor '$interceptor' cannot modify the request's size resolver. " +
				"Use `Interceptor.Chain.withSize` instead."
		}
	}

	private fun copy(
		index: Int = this.index,
		request: ImageRequest = this.request,
		size: Size = this.size,
	) = RealInterceptorChain(
		initialRequest = initialRequest,
		interceptors = interceptors,
		index = index,
		request = request,
		size = size,
		eventListener = eventListener,
		isPlaceholderCached = isPlaceholderCached
	)
}
