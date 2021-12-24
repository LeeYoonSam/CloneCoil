package com.ys.coil.size

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface ViewSizeResolver<T : View>: SizeResolver {

    companion object {
        /**
         * 기본 [View] 측정 구현을 사용하여 [ViewSizeResolver]를 만듭니다.
         *
         * @param view 측정할 뷰입니다.
         * @param subtractPadding true 이면 뷰의 패딩을 크기에서 뺍니다.
         */
        @JvmStatic
        @JvmOverloads
        @JvmName("create")
        operator fun <T : View> invoke(
            view: T,
            subtractPadding: Boolean = true
        ): ViewSizeResolver<T> = RealViewSizeResolver(view, subtractPadding)
    }

    /** [View]를 측정합니다. 이 필드는 변경할 수 없습니다. */
    val view: T

    /** 참이면 [View]의 패딩이 크기에서 뺍니다. */
    val subtractPadding: Boolean get() = true

    override suspend fun size(): Size {
        // Fast path: view 는 이미 측정되었습니다.
        getSize()?.let { return it }

        // 느린 경로: 뷰가 측정될 때까지 기다립니다.
        return suspendCancellableCoroutine { continuation ->
            val viewTreeObserver = view.viewTreeObserver

            val preDrawListener = object : ViewTreeObserver.OnPreDrawListener {

                private var isResumed = false

                override fun onPreDraw(): Boolean {
                    val size = getSize()

                    if (size != null) {
                        viewTreeObserver.removePreDrawListenerSafe(this)

                        if (!isResumed) {
                            isResumed = true
                            continuation.resume(size)
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

    private fun getSize(): PixelSize? {
        val width = getWidth().also { if (it <= 0) return null }
        val height = getHeight().also { if (it <= 0) return null }
        return PixelSize(width, height)
    }

    private fun getWidth(): Int {
        return getDimension(
            paramSize = view.layoutParams?.width ?: -1,
            viewSize = view.width,
            paddingSize = if (subtractPadding) view.paddingLeft + view.paddingRight else 0,
            isWidth = true,
        )
    }

    fun getHeight(): Int {
        return getDimension(
            paramSize = view.layoutParams?.height ?: -1,
            viewSize = view.height,
            paddingSize = if (subtractPadding) view.paddingTop + view.paddingBottom else 0,
            isWidth = false
        )
    }

    private fun getDimension(
        paramSize: Int,
        viewSize: Int,
        paddingSize: Int,
        isWidth: Boolean
    ): Int {
        // dimension 이 view의 레이아웃 매개변수 값과 일치한다고 가정합니다.
        val insetParamSize = paramSize - paddingSize
        if (insetParamSize > 0) {
            return insetParamSize
        }

        // view 의 현재 크기로 대체합니다.
        val insetViewSize = viewSize - paddingSize
        if (insetViewSize > 0) {
            return insetViewSize
        }

        // dimension WRAP_CONTENT로 설정된 경우 디스플레이 크기로 폴백합니다.
        if (paramSize == ViewGroup.LayoutParams.WRAP_CONTENT) {
            return view.context.resources.displayMetrics
                .run { if (isWidth) widthPixels else heightPixels }
        }

        // 크기를 확인할 수 없습니다.
        return -1
    }

    private fun ViewTreeObserver.removePreDrawListenerSafe(victim: ViewTreeObserver.OnPreDrawListener) {
        when {
            isAlive -> removeOnPreDrawListener(victim)
            else -> view.viewTreeObserver.removeOnPreDrawListener(victim)
        }
    }
}