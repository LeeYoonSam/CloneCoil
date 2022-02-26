package com.ys.coil.compose.utils

import androidx.compose.ui.test.IdlingResource
import com.ys.coil.EventListener
import com.ys.coil.request.ErrorResult
import com.ys.coil.request.ImageRequest
import com.ys.coil.request.SuccessResult

class ImageLoaderIdlingResource : EventListener, IdlingResource {

	private val ongoingRequest = mutableSetOf<ImageRequest>()

	var startedRequests = 0
		private set

	var finishedRequests = 0
		private set

	override val isIdleNow: Boolean
		get() = ongoingRequest.isEmpty()

	override fun onStart(request: ImageRequest) {
		ongoingRequest += request
		startedRequests++
	}

	override fun onCancel(request: ImageRequest) {
		ongoingRequest -= request
	}

	override fun onError(request: ImageRequest, result: ErrorResult) {
		ongoingRequest -= request
		finishedRequests++
	}

	override fun onSuccess(request: ImageRequest, result: SuccessResult) {
		ongoingRequest -= request
		finishedRequests++
	}
}
