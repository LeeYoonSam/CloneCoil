package com.ys.coil.memory

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.widget.ImageView
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import com.ys.coil.decode.Options
import com.ys.coil.lifecycle.GlobalLifecycle
import com.ys.coil.lifecycle.LifecycleCoroutineDispatcher
import com.ys.coil.request.CachePolicy
import com.ys.coil.request.GetRequest
import com.ys.coil.request.LoadRequest
import com.ys.coil.request.Request
import com.ys.coil.size.DisplaySizeResolver
import com.ys.coil.size.Scale
import com.ys.coil.size.SizeResolver
import com.ys.coil.size.ViewSizeResolver
import com.ys.coil.target.ViewTarget
import com.ys.coil.transform.Transformation
import com.ys.coil.util.getLifecycle
import com.ys.coil.util.scale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * [Request]에 대해 작동하는 작업을 처리합니다.
 */
internal class RequestService {

    @MainThread
    fun lifecycleInfo(request: Request): LifecycleInfo {
        return when (request) {
            is GetRequest -> LifecycleInfo.GLOBAL
            is LoadRequest -> {
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

    fun sizeResolver(request: Request, context: Context): SizeResolver {
        val sizeResolver = request.sizeResolver
        val target = request.target
        return when {
            sizeResolver != null -> sizeResolver
            target is ViewTarget<*> -> ViewSizeResolver(target.view)
            else -> DisplaySizeResolver(context)
        }
    }

    fun scale(request: Request, sizeResolver: SizeResolver): Scale {
        val scale = request.scale
        if (scale != null) {
            return scale
        }

        if (sizeResolver is ViewSizeResolver<*>) {
            val view = sizeResolver.view
            if (view is ImageView) {
                return view.scale
            }
        }

        val target = request.target
        if (target is ViewTarget<*>) {
            val view = target.view
            if (view is ImageView) {
                return view.scale
            }
        }

        return Scale.FILL
    }

    fun options(request: Request, scale: Scale, isOnline: Boolean): Options {
        val isValidBitmapConfig = request.isConfigValidForTransformations() && request.isConfigValidForAllowHardware()
        val bitmapConfig = if (isValidBitmapConfig) request.bitmapConfig else Bitmap.Config.ARGB_8888
        val allowRgb565 = isValidBitmapConfig && request.allowRgb565
        val networkCachePolicy = if (!isOnline && request.networkCachePolicy.readEnabled) {
            CachePolicy.DISABLED
        } else {
            request.networkCachePolicy
        }

        return Options(
            config = bitmapConfig,
            colorSpace = request.colorSpace,
            scale = scale,
            allowRgb565 = allowRgb565,
            networkCachePolicy = request.diskCachePolicy,
            diskCachePolicy = networkCachePolicy
        )
    }

    private fun LoadRequest.getLifecycle(): Lifecycle? {
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