package com.ys.coil.request

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.os.Build.VERSION.SDK_INT
import com.ys.coil.fetch.Fetcher
import com.ys.coil.size.OriginalSize
import com.ys.coil.size.Scale
import com.ys.coil.size.Size
import com.ys.coil.util.EMPTY_HEADERS
import com.ys.coil.util.NULL_COLOR_SPACE
import okhttp3.Headers

/**
 * 이미지를 가져오고 디코딩하기 위한 구성 옵션 집합입니다.
 * [Fetcher] 및 [Decoder]는 이러한 옵션을 최대한 존중해야 합니다.
 */
class Options(
    /**
     * 이 요청을 실행하는 데 사용되는 [Context]입니다.
     */
    val context: Context,

    /**
     * 모든 [Bitmap]에 대해 요청된 구성입니다.
     */
    val config: Bitmap.Config = Bitmap.Config.ARGB_8888,

    /**
     * 모든 [Bitmap]에 대한 기본 색상 공간입니다.
     * 'null'인 경우 구성 요소는 일반적으로 [ColorSpace.Rgb]로 기본 설정되어야 합니다.
     */
    val colorSpace: ColorSpace? = NULL_COLOR_SPACE,

    /**
     * 이미지 요청에 대해 요청된 출력 크기입니다.
     */
    val size: Size = OriginalSize,

    /**
     * 소스 이미지의 치수를 대상의 치수에 맞추는 방법에 대한 크기 조정 알고리즘입니다.
     */
    val scale: Scale = Scale.FIT,

    /**
     * 출력 이미지가 대상의 치수에 정확히 맞거나 채울 필요가 없으면 'true'입니다.
     * 예를 들어, 'true'인 경우 [BitmapFactoryDecoder]는 최적화로 소스 크기보다 큰 크기의 이미지를 디코딩하지 않습니다.
     */
    val allowInexactSize: Boolean = false,

    /**
     * 구성 요소가 [Bitmap.Config.RGB_565]를 최적화로 사용할 수 있는 경우 'true'입니다.
     * RGB_565에는 알파 채널이 없으므로 구성 요소는 이미지가 알파를 사용하지 않는 것이 보장되는 경우에만 RGB_565를 사용해야 합니다.
     */
    val allowRgb565: Boolean = false,

    /**
     * 디코딩된 이미지의 색상(RGB) 채널에 알파 채널을 미리 곱해야 하는 경우 'true'입니다.
     * 기본 동작은 사전 곱셈을 활성화하는 것이지만 일부 환경에서는 소스 픽셀을 수정하지 않은 상태로 두려면 이 기능을 비활성화해야 할 수 있습니다.
     */
    val premultipliedAlpha: Boolean = true,

    /**
     * 이미지를 디스크 캐시에 유지할 때 사용할 캐시 키 또는 구성 요소가 자체적으로 계산할 수 있는 경우 'null'입니다.
     */
    val diskCacheKey: String? = null,

    /**
     * 모든 네트워크 요청에 사용할 헤더 필드입니다.
     */
    val headers: Headers = EMPTY_HEADERS,

    /**
     * 맞춤 매개변수의 맵입니다. 이들은 사용자 정의 데이터를 구성 요소에 전달하는 데 사용됩니다.
     */
    val parameters: Parameters = Parameters.EMPTY,

    /**
     * 이 요청이 메모리에서/메모리로 읽기/쓰기가 허용되는지 여부를 결정합니다.
     */
    val memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,

    /**
     * 이 요청이 디스크에서/디스크로 읽기/쓰기가 허용되는지 여부를 결정합니다.
     */
    val diskCachePolicy: CachePolicy = CachePolicy.ENABLED,

    /**
     * 이 요청이 네트워크에서 읽을 수 있는지 여부를 결정합니다.
     */
    val networkCachePolicy: CachePolicy = CachePolicy.ENABLED,
) {

    fun copy(
        context: Context = this.context,
        config: Bitmap.Config = this.config,
        colorSpace: ColorSpace? = this.colorSpace,
        size: Size = this.size,
        scale: Scale = this.scale,
        allowInexactSize: Boolean = this.allowInexactSize,
        allowRgb565: Boolean = this.allowRgb565,
        premultipliedAlpha: Boolean = this.premultipliedAlpha,
        diskCacheKey: String? = this.diskCacheKey,
        headers: Headers = this.headers,
        parameters: Parameters = this.parameters,
        memoryCachePolicy: CachePolicy = this.memoryCachePolicy,
        diskCachePolicy: CachePolicy = this.diskCachePolicy,
        networkCachePolicy: CachePolicy = this.networkCachePolicy,
    ) = Options(
        context = context,
        config = config,
        colorSpace = colorSpace,
        size = size,
        scale = scale,
        allowInexactSize = allowInexactSize,
        allowRgb565 = allowRgb565,
        premultipliedAlpha = premultipliedAlpha,
        diskCacheKey = diskCacheKey,
        headers = headers,
        parameters = parameters,
        memoryCachePolicy = memoryCachePolicy,
        diskCachePolicy = diskCachePolicy,
        networkCachePolicy = networkCachePolicy,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Options &&
            context == other.context &&
            config == other.config &&
            (SDK_INT < 26 || colorSpace == other.colorSpace) &&
            size == other.size &&
            scale == other.scale &&
            allowInexactSize == other.allowInexactSize &&
            allowRgb565 == other.allowRgb565 &&
            premultipliedAlpha == other.premultipliedAlpha &&
            diskCacheKey == other.diskCacheKey &&
            headers == other.headers &&
            parameters == other.parameters &&
            memoryCachePolicy == other.memoryCachePolicy &&
            diskCachePolicy == other.diskCachePolicy &&
            networkCachePolicy == other.networkCachePolicy
    }

    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + config.hashCode()
        result = 31 * result + (colorSpace?.hashCode() ?: 0)
        result = 31 * result + size.hashCode()
        result = 31 * result + scale.hashCode()
        result = 31 * result + allowInexactSize.hashCode()
        result = 31 * result + allowRgb565.hashCode()
        result = 31 * result + premultipliedAlpha.hashCode()
        result = 31 * result + (diskCacheKey?.hashCode() ?: 0)
        result = 31 * result + headers.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + memoryCachePolicy.hashCode()
        result = 31 * result + diskCachePolicy.hashCode()
        result = 31 * result + networkCachePolicy.hashCode()
        return result
    }
}
