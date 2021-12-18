package com.ys.coil.decode

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import com.ys.coil.fetch.DrawableResult
import com.ys.coil.fetch.SourceResult
import com.ys.coil.ImageLoader
import okhttp3.HttpUrl
import java.io.File
import java.nio.ByteBuffer

/**
 * [Drawable]이 로드된 소스를 나타냅니다.
 *
 * @see SourceResult.dataSource
 * @see DrawableResult.dataSource
 */
enum class DataSource {
    /**
     * [ImageLoader]의 메모리 캐시를 나타냅니다.
     *
     * 요청이 합선되어 전체 이미지 파이프라인을 건너뛰었음을 의미하는 특수 데이터 소스입니다.
     */
    MEMORY_CACHE,

    /**
     * 메모리 내 데이터 소스를 나타냅니다(예: [Bitmap], [ByteBuffer]).
     */
    MEMORY,

    /**
     * 디스크 기반 데이터 소스(예: [DrawableRes], [File])를 나타냅니다.
     */
    DISK,

    /**
     * 네트워크 기반 데이터 소스(예: [HttpUrl])를 나타냅니다.
     */
    NETWORK
}
