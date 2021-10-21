package com.ys.coil.decode

import android.graphics.drawable.Drawable
import com.ys.coil.fetch.DrawableResult
import com.ys.coil.fetch.SourceResult

/**
 * [Drawable]이 로드된 소스를 나타냅니다.
 *
 * @see SourceResult.dataSource
 * @see DrawableResult.dataSource
 */
enum class DataSource {
    NETWORK,
    DISK,
    MEMORY
}
