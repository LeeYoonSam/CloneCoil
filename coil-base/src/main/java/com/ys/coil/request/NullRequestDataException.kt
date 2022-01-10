package com.ys.coil.request

import com.ys.coil.ImageLoader

/**
 * 비어 있거나 null 데이터가 있는 [ImageRequest]가 [ImageLoader]에 의해 실행되면 예외가 발생합니다.
 */
class NullRequestDataException : RuntimeException("The request's data is null.")
