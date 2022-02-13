package com.ys.coil.size

/**
 * 범위 정책을 나타냅니다.
 *
 * 개념적으로 View 에서 이미지의 gravity 에 대한 지식 없이 이것을 [ImageView.ScaleType]으로 생각할 수 있습니다.
 *
 * @see RequestBuilder.scale
 */
enum class Scale {

    /**
     * 이미지의 두 치수(너비 및 높이)가 View 의 해당 치수보다 **크거나 같도록** View 에 이미지를 채운다.
     */
    FILL,

    /**
     * 이미지의 두 치수(너비 및 높이)가 View 의 해당 치수보다 **작거나 같도록** 이미지를 View 에 맞춘다.
     */
    FIT
}
