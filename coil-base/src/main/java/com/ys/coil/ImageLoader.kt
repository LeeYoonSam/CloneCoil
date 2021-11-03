package com.ys.coil

import android.graphics.drawable.Drawable
import com.ys.coil.request.GetRequest
import com.ys.coil.request.LoadRequest
import com.ys.coil.request.Request
import com.ys.coil.request.RequestDisposable

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
//        inline operator fun invoke(
//            context: Context,
//            builder: ImageLoaderBuilder.() -> Unit = {}
//        ): ImageLoader = ImageLoaderBuilder(context).apply(builder).build()
    }

    /**
     * 이 이미지 로더에 의해 생성된 모든 [Request]에 대한 기본 옵션입니다.
     */
    val defaults: DefaultRequestOptions

    /**
     * 비동기 작업을 시작하여 [request]의 데이터를 [Target]으로 로드합니다.
     *
     * 요청의 대상이 null인 경우 이 메서드는 이미지를 미리 로드합니다.
     *
     * @param request 실행할 요청입니다.
     * @return A [RequestDisposable] 요청 상태를 취소하거나 확인하는 데 사용할 수 있습니다.
     */
    fun load(request: LoadRequest): RequestDisposable

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