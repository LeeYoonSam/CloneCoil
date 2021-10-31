package com.ys.coil.memory

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.lifecycle.Lifecycle
import com.ys.coil.ImageLoader
import com.ys.coil.request.GetRequest
import com.ys.coil.request.LoadRequest
import com.ys.coil.request.Request
import com.ys.coil.target.PoolableViewTarget
import com.ys.coil.target.ViewTarget
import com.ys.coil.util.requestManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred

/**
 * [DelegateService]는 [Target]을 래핑하여 [Bitmap] 풀링을 지원하고 [Request]를 래핑하여 수명 주기를 관리합니다.
 */
internal class DelegateService(
    private val imageLoader: ImageLoader,
    private val referenceCounter: BitmapReferenceCounter
) {

    /**
     * [request]의 [Target]을 래핑하여 [Bitmap] 풀링을 지원합니다.
     */
    fun createTargetDelegate(request: Request): TargetDelegate {
        val target = request.target
        return when {
            request is GetRequest -> InvalidatableEmptyTargetDelegate(referenceCounter)
            target == null -> EmptyTargetDelegate
            target is PoolableViewTarget<*> -> PoolableTargetDelegate(target, referenceCounter)
            else -> InvalidatableTargetDelegate(target, referenceCounter)
        }
    }

    /**
     * [request]을 랩핑하여 수명 주기에 따라 [Request]을 자동으로 삭제하고 [ViewTarget]을 다시 시작합니다.
     */
    fun createRequestDelegate(
        request: Request,
        targetDelegate: TargetDelegate,
        lifecycle: Lifecycle,
        mainDispatcher: CoroutineDispatcher,
        deferred: Deferred<Drawable>
    ): RequestDelegate {
        val requestDelegate = when (request) {
            is GetRequest -> EmptyRequestDelegate
            is LoadRequest -> if (request.target is ViewTarget<*>) {
                ViewTargetRequestDelegate(imageLoader, request, targetDelegate, lifecycle, mainDispatcher, deferred)
            } else {
                BaseRequestDelegate(lifecycle, mainDispatcher, deferred)
            }
        }

        lifecycle.addObserver(requestDelegate)

        val target = request.target
        if (target is ViewTarget<*>) {
            target.requestManager.setRequest(requestDelegate)
        }

        return requestDelegate
    }
}