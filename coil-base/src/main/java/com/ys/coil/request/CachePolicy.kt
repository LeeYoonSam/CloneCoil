package com.ys.coil.request

/**
 * 캐시 소스에 대한 읽기/쓰기 정책을 나타냅니다.
 *
 * @see Request.networkCachePolicy
 * @see Request.diskCachePolicy
 * @see Request.memoryCachePolicy
 */
enum class CachePolicy(
    val readEnabled: Boolean,
    val writeEnabled: Boolean
) {
    ENABLED(true, true),
    READ_ONLY(true, false),
    WRITE_ONLY(false, true),
    DISABLED(false, false)
}
