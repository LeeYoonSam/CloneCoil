package com.ys.coil.gif.drawable

import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.RequiresApi
import androidx.core.graphics.withSave
import com.ys.coil.decode.DecodeUtils
import com.ys.coil.size.Scale
import kotlin.math.roundToInt

/**
 * [child]을 중앙에 배치하고 크기를 조정하여 경계를 채우는 [Drawable]입니다.
 *
 * 이를 통해 고유 치수 내에서만 그리는 드로어블(예: [AnimatedImageDrawable])이 전체 경계를 채울 수 있습니다.
 */
class ScaleDrawable @JvmOverloads constructor(
    val child: Drawable,
    val scale: Scale = Scale.FIT
) : Drawable(), Drawable.Callback, Animatable {

    private var childDx = 0f
    private var childDy = 0f
    private var childScale = 1f

    init {
        child.callback = this
    }

    override fun draw(canvas: Canvas) {
        canvas.withSave {
            translate(childDx, childDy)
            scale(childScale, childScale)
            child.draw(this)
        }
    }

    override fun getAlpha() = child.alpha

    override fun setAlpha(alpha: Int) {
        child.alpha = alpha
    }

    @Suppress("DEPRECATION")
    override fun getOpacity() = child.opacity

    override fun getColorFilter() = child.colorFilter

    override fun setColorFilter(colorFilter: ColorFilter?) {
        child.colorFilter = colorFilter
    }

    override fun onBoundsChange(bounds: Rect) {
        val width = child.intrinsicWidth
        val height = child.intrinsicHeight
        if (width <= 0 || height <= 0) {
            child.bounds = bounds
            childDx = 0f
            childDy = 0f
            childScale = 1f
            return
        }

        val targetWidth = bounds.width()
        val targetHeight = bounds.height()
        val multiplier = DecodeUtils.computeSizeMultiplier(width, height, targetWidth, targetHeight, scale)

        val left = ((targetWidth - multiplier * width) / 2).roundToInt()
        val top = ((targetHeight - multiplier * height) / 2).roundToInt()
        val right = left + width
        val bottom = top + height
        child.setBounds(left, top, right, bottom)

        childDx = bounds.left.toFloat()
        childDy = bounds.top.toFloat()
        childScale = multiplier.toFloat()
    }

    override fun onLevelChange(level: Int) = child.setLevel(level)

    override fun onStateChange(state: IntArray) = child.setState(state)

    override fun getIntrinsicWidth() = child.intrinsicWidth

    override fun getIntrinsicHeight() = child.intrinsicHeight

    override fun unscheduleDrawable(who: Drawable, what: Runnable) = unscheduleSelf(what)

    override fun invalidateDrawable(who: Drawable) = invalidateSelf()

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) = scheduleSelf(what, `when`)

    override fun setTint(tintColor: Int) = child.setTint(tintColor)

    override fun setTintList(tint: ColorStateList?) = child.setTintList(tint)

    override fun setTintMode(tintMode: PorterDuff.Mode?) = child.setTintMode(tintMode)

    @RequiresApi(29)
    override fun setTintBlendMode(blendMode: BlendMode?) = child.setTintBlendMode(blendMode)

    override fun isRunning() = child is Animatable && child.isRunning

    override fun start() {
        if (child is Animatable) child.start()
    }

    override fun stop() {
        if (child is Animatable) child.stop()
    }
}
