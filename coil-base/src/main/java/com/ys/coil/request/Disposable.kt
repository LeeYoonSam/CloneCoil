package com.ys.coil.request

import android.view.View
import com.ys.coil.util.requestManager
import kotlinx.coroutines.Deferred

/**
 * [ImageLoader]에 의해 실행된 [ImageRequest]의 작업을 나타냅니다.
 */
interface Disposable {

	/**
	 * 가장 최근의 이미지 요청 작업입니다.
	 * 이 필드는 **불변**이 아니며 요청이 재생되면 변경될 수 있습니다.
	 */
	val job: Deferred<ImageResult>

	/**
	 * 이 일회용(disposable)의 작업이 완료되거나 취소되면 'true'를 반환합니다.
	 */
	val isDisposed: Boolean

	/**
	 * 이 일회용(disposable) 작업을 취소하고 보유된 리소스를 해제합니다.
	 */
	fun dispose()
}

/**
 * 원샷 이미지 요청에 대한 일회용입니다.
 */
internal class OneShotDisposable(
	override val job: Deferred<ImageResult>
) : Disposable {

	override val isDisposed: Boolean
		get() = !job.isActive

	override fun dispose() {
		if (isDisposed) return
		job.cancel()
	}
}

/**
 * [View]에 첨부된 요청에 대한 일회용입니다.
 *
 * [ViewTarget] 요청은 뷰가 분리되면 자동으로 취소되고 뷰가 연결되면 다시 시작됩니다.
 *
 * [isDisposed]는 이 일회용 요청이 지워지거나([DefaultLifecycleObserver.onDestroy]로 인해) 뷰에 첨부된 새 요청으로 대체될 때만 'true'를 반환합니다.
 */
internal class ViewTargetDisposable(
	private val view: View,
	@Volatile override var job: Deferred<ImageResult>
) : Disposable {
	override val isDisposed: Boolean
		get() = view.requestManager.isDisposed(this)

	override fun dispose() {
		if (isDisposed) return
		view.requestManager.dispose()
	}
}

