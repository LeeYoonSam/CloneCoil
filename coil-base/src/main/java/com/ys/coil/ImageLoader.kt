package com.ys.coil

import android.content.Context
import android.graphics.drawable.Drawable
import com.ys.coil.request.DefaultRequestOptions
import com.ys.coil.request.Disposable
import com.ys.coil.request.GetRequest
import com.ys.coil.request.ImageRequest
import com.ys.coil.request.Request

/**
 * [load] 및 [get]을 사용하여 이미지를 로드합니다.
 */
interface ImageLoader {
    companion object {
        /**
         * 새 [ImageLoader] 인스턴스를 만듭니다.
         *
         * Example:
         * ```
         * val loader = ImageLoader(context) {
         *     availableMemoryPercentage(0.5)
         *     crossfade(true)
         * }
         * ```
         */
        inline operator fun invoke(
            context: Context,
            builder: ImageLoaderBuilder.() -> Unit = {}
        ): ImageLoader = ImageLoaderBuilder(context).apply(builder).build()
    }

    /**
     * 이 이미지 로더에 의해 생성된 모든 [Request]에 대한 기본 옵션입니다.
     */
    val defaults: DefaultRequestOptions

    /**
     * 비동기적으로 실행할 [request]을 대기열에 넣습니다.
     *
     * 참고: 요청은 실행되기 전에 [ImageRequest.lifecycle]이 [Lifecycle.State.STARTED] 이상이 될 때까지 대기합니다.
     *
     * @param request 실행할 요청입니다.
     * @return A [일회용] 요청의 상태를 확인하거나 취소하는 데 사용할 수 있습니다.
     */
    fun enqueue(request: ImageRequest): Disposable

    /**
     * [request]의 데이터를 로드하고 작업이 완료될 때까지 일시 중단합니다. 로드된 [Drawable]을 반환합니다.
     *
     * @param request 실행할 요청입니다.
     * @return The [Drawable] 결과
     */
    suspend fun get(request: GetRequest): Drawable

    /**
     * 이 이미지 로더의 메모리 캐시와 비트맵 풀을 완전히 지웁니다.
     */
    fun clearMemory()

    /**
     * 이 이미지 로더를 종료하십시오.
     *
     * 연결된 모든 리소스가 해제되고 모든 새 요청은 시작하기 전에 실패합니다.
     *
     * 기내 [load] 요청은 취소됩니다. 기내 [get] 요청은 완료될 때까지 계속됩니다.
     */
    fun shutdown()
}