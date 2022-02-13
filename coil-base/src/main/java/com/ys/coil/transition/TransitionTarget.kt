package com.ys.coil.transition

import android.graphics.drawable.Drawable
import android.view.View
import com.ys.coil.target.Target

/**
 * [Transition] 적용을 지원하는 [Target]입니다.
 */
interface TransitionTarget : Target {
	/**
	 * 이 [Target]에서 사용하는 [View]입니다.
 	 */
	val view: View

	/**
	 * [View]의 현재 [Drawable]입니다.
	 */
	val drawable: Drawable?
}
