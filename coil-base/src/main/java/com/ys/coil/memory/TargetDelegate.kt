package com.ys.coil.memory

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.MainThread
import com.ys.coil.ImageLoader
import com.ys.coil.R
import com.ys.coil.drawable.CrossfadeDrawable
import com.ys.coil.request.Request
import com.ys.coil.target.Target
import com.ys.coil.target.ImageViewTarget
import com.ys.coil.target.PoolableViewTarget

/**
 * [Target]을 래핑하여 [Bitmap] 풀링을 지원합니다.
 *
 * @see DelegateService
 */
internal sealed class TargetDelegate {

    @MainThread
    open fun start(cached: BitmapDrawable?, placeholder: Drawable?) {}

    @MainThread
    open suspend fun success(result: Drawable, crossfadeMillis: Int) {}

    @MainThread
    open fun error(error: Drawable?, crossfadeMillis: Int) {}

    @MainThread
    open fun clear() {}
}

/**
 * 비어있는 대상의 Delegate. 요청에 대상이 없고 비트맵을 무효화할 필요가 없는 경우에 사용됩니다.
 */
internal object EmptyTargetDelegate : TargetDelegate()

/**
 * 성공한 Bitmap만 무효화합니다.
 *
 * [Request.target]이 null이고 성공 [Drawable]이 누출된 경우에 사용됩니다.
 *
 * @see ImageLoader.get
 */
internal class InvalidatableEmptyTargetDelegate(
    override val referenceCounter: BitmapReferenceCounter
) : TargetDelegate(), Invalidable {

    override suspend fun success(result: Drawable, crossfadeMillis: Int) {
        invalidate(result.bitmap)
    }
}

/**
 * 캐시된 비트맵과 성공 비트맵을 무효화합니다.
 */
internal class InvalidatableTargetDelegate(
    val target: Target,
    override val referenceCounter: BitmapReferenceCounter
) : TargetDelegate(), Invalidable {

    override fun start(cached: BitmapDrawable?, placeholder: Drawable?) {
        invalidate(cached?.bitmap)
        target.onStart(placeholder)
    }

    override suspend fun success(result: Drawable, crossfadeMillis: Int) {
        invalidate(result.bitmap)
        target.onSuccess(result)
    }

    override fun error(error: Drawable?, crossfadeMillis: Int) {
        target.onError(error)
    }
}

/**
 * 캐시된 비트맵 및 성공 비트맵에 대한 참조 카운트를 처리합니다.
 */
internal class PoolableTargetDelegate(
    override val target: PoolableViewTarget<*>,
    override val referenceCounter: BitmapReferenceCounter
) : TargetDelegate(), Poolable {

    override fun start(cached: BitmapDrawable?, placeholder: Drawable?) {
        instrument(cached?.bitmap) { onStart(placeholder) }
    }

    override suspend fun success(result: Drawable, crossfadeMillis: Int) {
        instrument(result.bitmap) { onSuccess(result, crossfadeMillis) }
    }

    override fun error(error: Drawable?, crossfadeMillis: Int) {
        instrument(null) { onError(error, crossfadeMillis) }
    }

    override fun clear() {
        instrument(null) { onClear() }
    }
}

private interface Invalidable {

    val referenceCounter: BitmapReferenceCounter

    fun invalidate(bitmap: Bitmap?) {
        bitmap?.let(referenceCounter::invalidate)
    }
}

private interface Poolable {

    private inline var PoolableViewTarget<*>.bitmap: Bitmap?
        get() = view.getTag(R.id.coil_bitmap) as? Bitmap
        set(value) = view.setTag(R.id.coil_bitmap, value)

    val target: PoolableViewTarget<*>
    val referenceCounter: BitmapReferenceCounter

    /**
     * 현재 Bitmap에 대한 참조 카운터를 증가시킵니다.
     */
    fun increment(bitmap: Bitmap?) {
        bitmap?.let(referenceCounter::increment)
    }

    /**
     * 현재 캐시된 비트맵에 대한 참조를 바꿉니다.
     */
    fun decrement(bitmap: Bitmap?) {
        target.bitmap?.let(referenceCounter::decrement)
        target.bitmap = bitmap
    }
}

private inline val Drawable.bitmap: Bitmap?
    get() = (this as? BitmapDrawable)?.bitmap

private inline fun Poolable.instrument(bitmap: Bitmap?, update: PoolableViewTarget<*>.() -> Unit) {
    increment(bitmap)
    target.update()
    decrement(bitmap)
}

private suspend inline fun Poolable.onSuccess(result: Drawable, crossfadeMillis: Int) {
    val target = target
    if (crossfadeMillis > 0 && target is ImageViewTarget) {
        target.onSuccessCrossfade(result, crossfadeMillis)
    } else {
        target.onSuccess(result)
    }
}

private fun Poolable.onError(error: Drawable?, crossfadeMillis: Int) {
    val target = target
    if (crossfadeMillis > 0 && error != null && target is ImageViewTarget) {
        target.onError(CrossfadeDrawable(target.view.drawable, error))
    } else {
        target.onError(error)
    }
}
