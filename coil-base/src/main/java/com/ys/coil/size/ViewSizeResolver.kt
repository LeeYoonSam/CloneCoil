package com.ys.coil.size

import android.view.View
import android.view.ViewTreeObserver
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface ViewSizeResolver<T : View>: SizeResolver {

    companion object {
        /**
         * 기본 [View] 측정 구현을 사용하여 [ViewSizeResolver] 인스턴스를 구성합니다.
         */
        operator fun <T : View> invoke(view: T): ViewSizeResolver<T> {
            return object : ViewSizeResolver<T> {
                override val view = view
            }
        }
    }

    val view: T

    override suspend fun size(): Size {
        // fast path: View의 높이가 레이아웃 매개변수의 데이터와 일치한다고 가정합니다.
        view.layoutParams?.let { layoutParams ->
            val width = layoutParams.width - view.paddingLeft - view.paddingRight
            val height = layoutParams.height - view.paddingTop - view.paddingBottom
            if (width > 0 && height > 0) {
                return PixelSize(width, height)
            }
        }

        // 뷰가 측정될 때까지 기다린다.
        return suspendCancellableCoroutine { continuation ->
            val viewTreeObserver = view.viewTreeObserver

            val preDrawListener = object : ViewTreeObserver.OnPreDrawListener {

                private var isResumed = false

                override fun onPreDraw(): Boolean {
                    if (!isResumed) {
                        val width = view.width - view.paddingLeft - view.paddingRight
                        val height = view.height - view.paddingTop - view.paddingBottom

                        if (width > 0 && height > 0) {
                            isResumed = true
                            viewTreeObserver.removePreDrawListenerSafe(this)
                            continuation.resume(PixelSize(width, height))
                        }
                    }

                    return true
                }
            }

            viewTreeObserver.addOnPreDrawListener(preDrawListener)

            continuation.invokeOnCancellation {
                viewTreeObserver.removePreDrawListenerSafe(preDrawListener)
            }
        }
    }

    private fun ViewTreeObserver.removePreDrawListenerSafe(victim: ViewTreeObserver.OnPreDrawListener) {
        when {
            isAlive -> removeOnPreDrawListener(victim)
            else -> view.viewTreeObserver.removeOnPreDrawListener(victim)
        }
    }
}