package com.ys.coil.target

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ys.coil.transition.TransitionTarget

/**
 * [View]에 첨부된 [Drawable] 업데이트를 단순화하고
 * 애니메이션 [Drawable]의 자동 시작 및 중지를 지원하는 독창적인 [ViewTarget]입니다.
 *
 * 이 클래스가 지원하지 않는 사용자 정의 동작이 필요한 경우 [ViewTarget]을 직접 구현하는 것이 좋습니다.
 */
abstract class GenericViewTarget<T: View>: ViewTarget<T>, TransitionTarget, DefaultLifecycleObserver {

	private var isStarted = false

	// [view]에 연결된 현재 [Drawable]입니다.
	abstract override var drawable: Drawable?

	override fun onStart(placeHolder: Drawable?) = updateDrawable(placeHolder)

	override fun onError(error: Drawable?) = updateDrawable(error)

	override fun onSuccess(result: Drawable) = updateDrawable(result)

	override fun onStart(owner: LifecycleOwner) {
		isStarted = true
		updateAnimation()
	}

	override fun onStop(owner: LifecycleOwner) {
		isStarted = false
		updateAnimation()
	}

	/** [ImageView]의 현재 드로어블을 [Drawable]로 바꿉니다. */
	private fun updateDrawable(drawable: Drawable?) {
		(this.drawable as? Animatable)?.stop()
		this.drawable = drawable
		updateAnimation()
	}

	/** 현재 수명 주기 상태를 기반으로 현재 [Drawable]의 애니메이션을 시작/중지합니다. */
	private fun updateAnimation() {
		val animatable = drawable as? Animatable ?: return
		if (isStarted) animatable.start() else animatable.stop()
	}
}