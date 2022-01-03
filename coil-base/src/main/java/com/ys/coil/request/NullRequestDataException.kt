package com.ys.coil.request

import com.ys.coil.ImageLoader

/**
 * Exception thrown when an [ImageRequest] with empty/null data is executed by an [ImageLoader].
 */
class NullRequestDataException : RuntimeException("The request's data is null.")
