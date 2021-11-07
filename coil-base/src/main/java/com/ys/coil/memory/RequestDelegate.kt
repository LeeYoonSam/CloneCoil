package com.ys.coil.memory

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ys.coil.ImageLoader
import com.ys.coil.request.LoadRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job

internal sealed class RequestDelegate : DefaultLifecycleObserver {

    /**
     * 동일한 매개변수로 이 요청을 반복하십시오.
     */
    open fun restart() {}

    /**
     * 이 delegate 와 관련된 자원 해지
     */
    open fun dispose() {}

    /**
     * 로드가 완료되면 호출됩니다.
     */
    open fun onComplete() {}
}

/**
 * 빈 요청 delegate
 */
internal object EmptyRequestDelegate : RequestDelegate()

/**
 * 재시작을 지원하지 않는 단순 요청 delegate.
 */
internal class BaseRequestDelegate(
    private val lifecycle: Lifecycle,
    private val dispatcher: CoroutineDispatcher,
    private val job: Job
) : RequestDelegate() {

    override fun dispose() = job.cancel()

    override fun onComplete() {
        if (dispatcher is LifecycleObserver) {
            lifecycle.removeObserver(dispatcher)
        }
        lifecycle.removeObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) = dispose()
}

/**
 * 연결된 뷰가 있고 재시작을 지원하는 요청 delegate.
 *
 * @see ViewTargetRequestManager
 */
internal class ViewTargetRequestDelegate(
    private val loader: ImageLoader,
    internal val request: LoadRequest,
    private val target: TargetDelegate,
    private val lifecycle: Lifecycle,
    private val dispatcher: CoroutineDispatcher,
    private val job: Job
) : RequestDelegate() {

    override fun restart() {
        loader.load(request)
    }

    override fun dispose() {
        job.cancel()
        target.clear()

        if (request.target is LifecycleObserver) {
            lifecycle.removeObserver(request.target)
        }
        lifecycle.removeObserver(this)
    }

    override fun onComplete() {
        if (dispatcher is LifecycleObserver) {
            lifecycle.removeObserver(dispatcher)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) = dispose()
}