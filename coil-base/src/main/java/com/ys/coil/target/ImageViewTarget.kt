package com.ys.coil.target

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ys.coil.drawable.CrossfadeDrawable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * [ImageView]에서 이미지 설정을 처리하는 [Target].
 */
class ImageViewTarget(override val view: ImageView) : PoolableViewTarget<ImageView>, DefaultLifecycleObserver {
    private var isStarted = false

    override fun onStart(placeHolder: Drawable?) = setDrawable(placeHolder)

    override fun onSuccess(result: Drawable) = setDrawable(result)

    override fun onError(error: Drawable?) = setDrawable(error)

    override fun onClear() = setDrawable(null)

    override fun onStart(owner: LifecycleOwner) {
        isStarted = true
        updateAnimation()
    }

    override fun onStop(owner: LifecycleOwner) {
        isStarted = false
        updateAnimation()
    }

    internal suspend inline fun onSuccessCrossfade(
        result: Drawable,
        duration: Int
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        val drawable = CrossfadeDrawable(
            start = view.drawable,
            end = result,
            duration = duration,
            onEnd = { continuation.resume(Unit) }
        )
        continuation.invokeOnCancellation { drawable.stop() }
        onSuccess(drawable)
    }

    private fun setDrawable(drawable: Drawable?) {
        (view.drawable as? Animatable)?.stop()
        view.setImageDrawable(drawable)
        updateAnimation()
    }

    private fun updateAnimation() {
        val animatable = view.drawable as? Animatable ?: return
        if (isStarted) animatable.start()else animatable.stop()

    }
}