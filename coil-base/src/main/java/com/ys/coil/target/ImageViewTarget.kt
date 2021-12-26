package com.ys.coil.target

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.ys.coil.drawable.CrossfadeDrawable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * [ImageView]에서 이미지 설정을 처리하는 [Target].
 */
open class ImageViewTarget(override val view: ImageView) : GenericViewTarget<ImageView>() {

    override var drawable: Drawable?
        get() = view.drawable
        set(value) = view.setImageDrawable(value)

    /**
     * 요청 성공한 [Drawable]을 현재 드로어블과 크로스페이드하는 내부 메서드입니다.
     *
     * [Request.crossfadeMillis] > 0인 경우 [onSuccess] 대신 호출됩니다.
     *
     * 애니메이션에서 아직 사용 중인 현재 드로어블을 풀링하지 않도록 애니메이션이 완료될 때까지 요청이 일시 중단됩니다.
     */
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ImageViewTarget && view == other.view
    }

    override fun hashCode() = view.hashCode()
}