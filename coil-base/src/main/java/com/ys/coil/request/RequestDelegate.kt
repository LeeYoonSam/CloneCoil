package com.ys.coil.request

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ys.coil.ImageLoader
import com.ys.coil.target.ViewTarget
import com.ys.coil.util.removeAndAddObserver
import com.ys.coil.util.requestManager
import kotlinx.coroutines.Job
import kotlin.coroutines.cancellation.CancellationException

internal sealed class RequestDelegate : DefaultLifecycleObserver {

	/** 시작하기 전에 이 요청을 취소해야 하는 경우 [CancellationException]을 throw합니다. */
	@MainThread
	open fun assertActive() {}

	/** 모든 수명 주기 관찰자를 등록합니다. */
	@MainThread
	open fun start() {}

	/** 이 요청의 작업이 취소되거나 성공/실패할 때 호출됩니다. */
	@MainThread
	open fun complete() {}

	/** 이 요청의 작업을 취소하고 모든 수명 주기 관찰자를 지웁니다. */
	@MainThread
	open fun dispose() {}
}

/** 대상이 없거나 [ViewTarget]이 아닌 일회성 요청에 대한 요청 대리자. */
internal class BaseRequestDelegate(
	private val lifecycle: Lifecycle,
	private val job: Job
) : RequestDelegate() {
	override fun start() {
		lifecycle.addObserver(this)
	}

	override fun complete() {
		lifecycle.removeObserver(this)
	}

	override fun dispose() {
		job.cancel()
	}

	override fun onDestroy(owner: LifecycleOwner) = dispose()
}

/** [ViewTarget]이 있는 재시작 가능한 요청에 대한 요청 대리자. */
internal class ViewTargetRequestDelegate(
	private val imageLoader: ImageLoader,
	private val initialRequest: ImageRequest,
	private val target: ViewTarget<*>,
	private val lifecycle: Lifecycle,
	private val job: Job
) : RequestDelegate() {

	/** 동일한 [ImageRequest]로 이 요청을 반복합니다. */
	@MainThread
	fun restart() {
		imageLoader.enqueue(initialRequest)
	}

	override fun assertActive() {
		if (!target.view.isAttachedToWindow) {
			target.view.requestManager.setRequest(this)
			throw CancellationException("'ViewTarget.view' must be attached to a window.")
		}
	}

	override fun start() {
		lifecycle.addObserver(this)
		if (target is LifecycleObserver) lifecycle.removeAndAddObserver(target)
		target.view.requestManager.setRequest(this)
	}

	override fun dispose() {
		job.cancel()
		if (target is LifecycleObserver) lifecycle.removeObserver(target)
		lifecycle.removeObserver(this)
	}

	override fun onDestroy(owner: LifecycleOwner) {
		target.view.requestManager.dispose()
	}
}