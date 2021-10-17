package com.ys.coil.size

import android.content.Context

/**
 * [Target]을 디스플레이 크기로 제한하는 [SizeResolver]입니다.
 *
 * 이것은 [Request]에 대한 대체 [SizeResolver]로 사용됩니다.
 */
class DisplaySizeResolver(private val context: Context) :SizeResolver {
    override suspend fun size(): Size {
        return context.resources.displayMetrics.run { PixelSize(widthPixels, heightPixels) }
    }
}