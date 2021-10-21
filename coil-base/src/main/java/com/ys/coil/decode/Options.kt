package com.ys.coil.decode

import android.graphics.Bitmap
import android.graphics.ColorSpace
import com.ys.coil.fetch.Fetcher
import com.ys.coil.request.CachePolicy
import com.ys.coil.size.Scale
/**
 * 이미지 로드 및 디코딩을 위한 구성 옵션 집합입니다.
 *
 * [Fetcher] 및 [Decoder]는 이러한 옵션을 최대한 존중해야 합니다.
 *
 * @param config The requested config for any [Bitmap]s.
 * @param colorSpace 모든 [Bitmap]에 대한 기본 색상 공간입니다.
 * @param scale 대상의 크기에 맞게 이미지를 로드해야 하는지 아니면 채우도록 해야 하는지 결정합니다.
 * @param allowRgb565 [Fetcher]가 [Bitmap.Config.RGB_565]를 최적화로 사용할 수 있으면 True입니다.
 * @param networkCachePolicy 이 요청이 네트워크에서 읽을 수 있는지 여부를 결정하는 데 사용됩니다.
 * @param diskCachePolicy 이 요청이 디스크에서 디스크로 읽기/쓰기가 허용되는지 여부를 결정하는 데 사용됩니다.
 */
data class Options(
    val config: Bitmap.Config,
    val colorSpace: ColorSpace?,
    val scale: Scale,
    val allowRgb565: Boolean,
    val networkCachePolicy: CachePolicy,
    val diskCachePolicy: CachePolicy
)
