package com.ys.coil

import android.graphics.drawable.Drawable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * [RequestBuilder]를 초기화하는 데 사용되는 기본 요청 옵션 집합입니다.
 *
 * @see ImageLoader.defaults
 */
data class DefaultRequestOptions(
    val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    val allowRgb565: Boolean = false,
    val crossfadeMillis: Int = 0,
    val placeholder: Drawable? = null,
    val error: Drawable? = null
)