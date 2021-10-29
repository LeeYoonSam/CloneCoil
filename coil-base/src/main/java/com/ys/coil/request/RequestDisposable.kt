package com.ys.coil.request

import com.ys.coil.memory.ViewTargetRequestDelegate
import com.ys.coil.target.ViewTarget
import com.ys.coil.util.cancel
import com.ys.coil.util.requestManager
import kotlinx.coroutines.Job

/**
 * 이미지 요청 작업을 나타냅니다.
 */
interface RequestDisposable {
    /**
     * 요청이 활성 상태가 아니거나 완료되었거나 취소되면 true를 반환합니다.
     */
    fun isDisposed(): Boolean

    /**
     * 진행 중인 작업을 모두 취소하고 이 요청과 관련된 모든 리소스를 해제합니다. 이 메서드는 멱등원입니다.
     */
    fun dispose()
}

internal object EmptyRequestDisposable : RequestDisposable {

    override fun isDisposed() = true

    override fun dispose() {}
}

internal class BaseTargetRequestDisposable(private val job: Job) : RequestDisposable {

    override fun isDisposed(): Boolean {
        return !job.isActive || job.isCompleted
    }

    override fun dispose() {
        if (!isDisposed()) {
            job.cancel()
        }
    }
}

internal class ViewTargetRequestDisposable(
    private val target: ViewTarget<*>,
    private val request: Request
) : RequestDisposable {

    /**
     * Check if the current request attached to this view is the same as this disposable's request.
     */
    override fun isDisposed(): Boolean {
        return (target.requestManager.getRequest() as? ViewTargetRequestDelegate)?.request != request
    }

    override fun dispose() {
        if (!isDisposed()) {
            target.cancel()
        }
    }
}