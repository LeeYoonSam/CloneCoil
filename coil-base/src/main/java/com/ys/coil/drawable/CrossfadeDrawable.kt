package com.ys.coil.drawable

import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.withSave
import androidx.core.graphics.withScale
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.Animatable2Compat.AnimationCallback
import com.ys.coil.decode.DecodeUtils
import com.ys.coil.size.Scale
import com.ys.coil.util.forEachIndices
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * [start]에서 [end]으로 크로스페이드되는 [Drawable]입니다.
 *
 * 참고: 애니메이션은 [start]으로 한 번만 실행할 수 있습니다.
 * 드로어블은 전환이 끝나면 역참조됩니다.
 *
 * @param start 크로스페이드할 [Drawable]입니다.
 * @param end 크로스페이드할 [Drawable]입니다.
 * @param scale [start]와 [end]에 대한 스케일링 알고리즘.
 * @param durationMillis 크로스페이드 애니메이션의 지속 시간.
 * @param fadeStart false인 경우 start 드로어블이 페이드 아웃되는 동안 end 드로어블이 페이드 인됩니다.
 * @param preferredExactIntrinsicSize true인 경우 이 드로어블의 고유 너비/높이는 -1만 됩니다.
 *  [start] 및 [end]가 해당 치수에 대해 -1을 반환하는 경우.
 *  false인 경우 [start] 또는 [end]가 해당 치수에 대해 -1을 반환하면 고유 너비/높이가 -1이 됩니다.
 *  이것은 드로어블의 크기를 조정하기 위해 정확한 고유 크기가 필요한 뷰에 유용합니다.
 */
class CrossfadeDrawable @JvmOverloads constructor(
    start: Drawable?,
    end: Drawable?,
    val scale: Scale = Scale.FIT,
    val durationMillis: Int = DEFAULT_DURATION,
    val fadeStart: Boolean = true,
    val preferExactIntrinsicSize: Boolean = false,
) : Drawable(), Drawable.Callback, Animatable2Compat {

    private val callbacks = mutableListOf<AnimationCallback>()

    private val intrinsicWidth = computeIntrinsicDimension(start?.intrinsicWidth, end?.intrinsicWidth)
    private val intrinsicHeight = computeIntrinsicDimension(start?.intrinsicHeight, end?.intrinsicHeight)

    private var start = start?.mutate()
    private var end = end?.mutate()
    private var startTimeMillis = 0L
    private var maxAlpha = 255
    private var state = STATE_START

    init {
        require(durationMillis > 0) { "durationMillis must be > 0." }

        this.start?.callback = this
        this.end?.callback = this
    }

    override fun draw(canvas: Canvas) {
        if (state == STATE_START) {
            start?.apply {
                alpha = maxAlpha
                canvas.withScale { draw(canvas) }
            }
            return
        }

        if (state == STATE_DONE) {
            end?.apply {
                alpha = maxAlpha
                canvas.withScale { draw(canvas) }
            }
            return
        }

        val percent = (SystemClock.uptimeMillis() - startTimeMillis) / durationMillis.toDouble()
        val endAlpha = (percent.coerceIn(0.0, 1.0) * maxAlpha).toInt()
        val startAlpha = if (fadeStart) maxAlpha - endAlpha else maxAlpha
        val isDone = percent >= 1.0

        // start 드로어블을 그립니다.
        if (!isDone) {
            start?.apply {
                alpha = startAlpha
                canvas.withSave { draw(canvas) }
            }
        }

        // end 드로어블을 그립니다.
        end?.apply {
            alpha = endAlpha
            canvas.withSave { draw(canvas) }
        }

        if (isDone) {
            markDone()
        } else {
            invalidateSelf()
        }
    }

    override fun getAlpha() = maxAlpha

    override fun setAlpha(alpha: Int) {
        require(alpha in 0..255) { "Invalid alpha: $alpha" }
        maxAlpha = alpha
    }

    @Suppress("DEPRECATION")
    override fun getOpacity(): Int {
        val start = start
        val end = end

        if (state == STATE_START) {
            return start?.opacity ?: PixelFormat.TRANSPARENT
        }

        if (state == STATE_DONE) {
            return end?.opacity ?: PixelFormat.TRANSPARENT
        }

        return when {
            start != null && end != null -> resolveOpacity(start.opacity, end.opacity)
            start != null -> start.opacity
            end != null -> end.opacity
            else -> PixelFormat.TRANSPARENT
        }
    }

    override fun getColorFilter(): ColorFilter? = when(state) {
        STATE_START -> start?.colorFilter
        STATE_RUNNING -> end?.colorFilter ?: start?.colorFilter
        STATE_DONE -> end?.colorFilter
        else -> null
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        start?.colorFilter = colorFilter
        end?.colorFilter = colorFilter
    }

    override fun onBoundsChange(bounds: Rect) {
        start?.let { updateBounds(it, bounds) }
        end?.let { updateBounds(it, bounds) }
    }

    override fun onLevelChange(level: Int): Boolean {
        val startChanged = start?.setLevel(level) ?: false
        val endChanged = end?.setLevel(level) ?: false
        return startChanged || endChanged
    }

    override fun onStateChange(state: IntArray): Boolean {
        val startChanged = start?.setState(state) ?: false
        val endChanged = end?.setState(state) ?: false
        return startChanged || endChanged
    }

    override fun getIntrinsicWidth() = intrinsicWidth

    override fun getIntrinsicHeight() = intrinsicHeight

    override fun unscheduleDrawable(who: Drawable, what: Runnable) = unscheduleSelf(what)

    override fun invalidateDrawable(who: Drawable) = invalidateSelf()

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) = scheduleSelf(what, `when`)

    override fun setTint(tintColor: Int) {
        start?.setTint(tintColor)
        end?.setTint(tintColor)
    }

    override fun setTintList(tint: ColorStateList?) {
        start?.setTintList(tint)
        end?.setTintList(tint)
    }

    override fun setTintMode(tintMode: PorterDuff.Mode?) {
        start?.setTintMode(tintMode)
        end?.setTintMode(tintMode)
    }

    @RequiresApi(29)
    override fun setTintBlendMode(blendMode: BlendMode?) {
        start?.setTintBlendMode(blendMode)
        end?.setTintBlendMode(blendMode)
    }

    override fun isRunning() = state == STATE_RUNNING

    override fun start() {
        (start as? Animatable)?.start()
        (end as? Animatable)?.start()

        if (state != STATE_START) {
            return
        }

        state = STATE_RUNNING
        startTimeMillis = SystemClock.uptimeMillis()
        callbacks.forEachIndices { it.onAnimationStart(this) }

        invalidateSelf()
    }

    override fun stop() {
        (start as? Animatable)?.stop()
        (end as? Animatable)?.stop()

        if (state != STATE_DONE) {
            markDone()
        }
    }

    override fun registerAnimationCallback(callback: AnimationCallback) {
        callbacks.add(callback)
    }

    override fun unregisterAnimationCallback(callback: AnimationCallback): Boolean {
        return callbacks.remove(callback)
    }

    override fun clearAnimationCallbacks() = callbacks.clear()

    /** 종횡비를 유지하면서 [targetBounds] 내에서 [Drawable]의 경계를 업데이트합니다. */
    @VisibleForTesting
    internal fun updateBounds(drawable: Drawable, targetBounds: Rect) {
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        if (width <= 0 || height <= 0) {
            drawable.bounds = targetBounds
            return
        }

        val targetWidth = targetBounds.width()
        val targetHeight = targetBounds.height()
        val multiplier = DecodeUtils.computeSizeMultiplier(width, height, targetWidth, targetHeight, scale)
        val dx = ((targetWidth - multiplier * width) / 2).roundToInt()
        val dy = ((targetHeight - multiplier * height) / 2).roundToInt()

        val left = targetBounds.left + dx
        val top = targetBounds.top + dy
        val right = targetBounds.right - dx
        val bottom = targetBounds.bottom - dy
        drawable.setBounds(left, top, right, bottom)
    }

    private fun computeIntrinsicDimension(startSize: Int?, endSize: Int?): Int {
        if (!preferExactIntrinsicSize && (startSize == -1 || endSize == -1)) return -1
        return max(startSize ?: -1, endSize ?: -1)
    }

    private fun markDone() {
        state = STATE_DONE
        start = null
        callbacks.forEachIndices { it.onAnimationEnd(this) }
    }

    companion object {
        private const val STATE_START = 0
        private const val STATE_RUNNING = 1
        private const val STATE_DONE = 2

        const val DEFAULT_DURATION = 100
    }
}