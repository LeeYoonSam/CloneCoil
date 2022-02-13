package com.ys.coil.target

import android.graphics.drawable.Drawable
import android.widget.ImageView

/**
 * [ImageView]에서 이미지 설정을 처리하는 [Target].
 */
open class ImageViewTarget(override val view: ImageView) : GenericViewTarget<ImageView>() {

    override var drawable: Drawable?
        get() = view.drawable
        set(value) = view.setImageDrawable(value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ImageViewTarget && view == other.view
    }

    override fun hashCode() = view.hashCode()
}
