package com.ys.coil.request

import android.view.View
import androidx.annotation.MainThread
import com.ys.coil.memory.ViewTargetRequestDelegate
import com.ys.coil.util.getCompletedOrNull
import com.ys.coil.util.isMainThread
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 한 번에 최대 하나의 실행된 [ImageRequest]를 지정된 [View]에 첨부할 수 있는지 확인합니다.
 *
 * @see requestManager
 */
internal class ViewTargetRequestManager(private val view: View) : View.OnAttachStateChangeListener {

    // 이 뷰에 연결된 현재 요청에 대한 일회용입니다.
    private var currentDisposable: ViewTargetDisposable? = null

    // 현재 요청을 지우기 위해 메인 스레드에 게시하는 보류 중인 작업입니다.
    private var pendingClear: Job? = null

    // 메인 스레드에서만 액세스합니다.
    private var currentRequest: ViewTargetRequestDelegate? = null
    private var isRestart = false

    /** [disposable]이 이 보기에 첨부되지 않은 경우 'true'를 반환합니다. */
    @Synchronized
    fun isDisposed(disposable: ViewTargetDisposable): Boolean {
        return disposable !== currentDisposable
    }

    /** 다시 시작된 요청이 아닌 새 일회용을 만들고 반환합니다. */
    @Synchronized
    fun getDisposable(job: Deferred<ImageResult>): ViewTargetDisposable {
        val disposable = currentDisposable
        if (disposable != null  && isMainThread() && isRestart) {
            isRestart = false
            disposable.job = job
            return disposable
        }

        pendingClear?.cancel()
        pendingClear = null

        return ViewTargetDisposable(view, job).also { currentDisposable = it }
    }

    /** 진행 중인 작업을 취소하고 이 보기에서 [currentRequest]를 분리합니다. */
    @Synchronized
    @OptIn(DelicateCoroutinesApi::class)
    fun dispose() {
        pendingClear?.cancel()
        pendingClear = GlobalScope.launch(Dispatchers.Main.immediate) { setRequest(null) }
        currentDisposable = null
    }

    /**
     * 완료된 경우 최신 작업의 완료된 값을 반환합니다. 그렇지 않으면 'null'을 반환합니다.
     */
    @Synchronized
    fun getResult(): ImageResult? {
        return currentDisposable?.job?.getCompletedOrNull()
    }

    /**
     * 이 뷰에 [request]을 첨부하고 이전 요청을 취소합니다.
     */
    @MainThread
    fun setRequest(request: ViewTargetRequestDelegate?) {
        currentRequest?.dispose()
        currentRequest = request
    }

    @MainThread
    override fun onViewAttachedToWindow(v: View) {
        val request = currentRequest ?: return

        // 메인 스레드에서 호출되기 때문에 isRestart는 request.restart()의 일부로 동기적으로 지워집니다.
        isRestart = true
        request.restart()
    }

    @MainThread
    override fun onViewDetachedFromWindow(v: View) {
        currentRequest?.dispose()
    }
}
