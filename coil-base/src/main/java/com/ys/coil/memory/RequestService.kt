package com.ys.coil.memory

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import com.ys.coil.lifecycle.GlobalLifecycle
import com.ys.coil.lifecycle.LifecycleCoroutineDispatcher
import com.ys.coil.request.Request
import com.ys.coil.target.ViewTarget
import com.ys.coil.transform.Transformation
import com.ys.coil.util.getLifecycle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * [Request]에 대해 작동하는 작업을 처리합니다.
 */
internal class RequestService {

    @MainThread
    fun lifecycleInfo(request: Request): LifecycleInfo {
        return when (request) {
            is Request.GetRequest -> LifecycleInfo.GLOBAL
            is Request.LoadRequest -> {
                // 이 요청에 대한 수명 주기를 찾습니다.
                val lifecycle = request.getLifecycle()
                return if (lifecycle != null) {
                    LifecycleInfo(
                        lifecycle = lifecycle,
                        mainDispatcher = LifecycleCoroutineDispatcher.create(Dispatchers.Main.immediate, lifecycle)
                    )
                } else {
                    LifecycleInfo.GLOBAL
                }
            }
        }
    }

    private fun Request.LoadRequest.getLifecycle(): Lifecycle? {
        return when {
            lifecycle != null -> lifecycle
            target is ViewTarget<*> -> target.view.context.getLifecycle()
            else -> context.getLifecycle()
        }
    }

    private fun Request.isConfigValidForTransformations(): Boolean {
        return transformations.isEmpty() || Transformation.VALID_CONFIGS.contains(bitmapConfig)
    }

    private fun Request.isConfigValidForAllowHardware(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || allowHardware || bitmapConfig != Bitmap.Config.HARDWARE
    }

    data class LifecycleInfo(
        val lifecycle: Lifecycle,
        val mainDispatcher: CoroutineDispatcher
    ) {

        companion object {
            val GLOBAL = LifecycleInfo(
                lifecycle = GlobalLifecycle,
                mainDispatcher = Dispatchers.Main.immediate
            )
        }
    }
}