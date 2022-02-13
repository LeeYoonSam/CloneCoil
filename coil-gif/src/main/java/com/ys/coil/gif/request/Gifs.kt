@file:Suppress("UNCHECKED_CAST", "unused")
@file:JvmName("Gifs")

package com.ys.coil.gif.request

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import com.ys.coil.annotation.ExperimentalCoilApi
import com.ys.coil.gif.decode.GifDecoder.Companion.ANIMATED_TRANSFORMATION_KEY
import com.ys.coil.gif.decode.GifDecoder.Companion.ANIMATION_END_CALLBACK_KEY
import com.ys.coil.gif.decode.GifDecoder.Companion.ANIMATION_START_CALLBACK_KEY
import com.ys.coil.gif.decode.GifDecoder.Companion.REPEAT_COUNT_KEY
import com.ys.coil.gif.drawable.MovieDrawable
import com.ys.coil.gif.transform.AnimatedTransformation
import com.ys.coil.request.ImageRequest
import com.ys.coil.request.Parameters

/**
 * 결과가 애니메이션 [Drawable]인 경우 애니메이션을 반복할 횟수를 설정합니다.
 *
 * Default: [MovieDrawable.REPEAT_INFINITE]
 *
 * @see MovieDrawable.setRepeatCount
 * @see AnimatedImageDrawable.setRepeatCount
 */
fun ImageRequest.Builder.repeatCount(repeatCount: Int): ImageRequest.Builder {
    require(repeatCount >= MovieDrawable.REPEAT_INFINITE) { "Invalid repeatCount: $repeatCount" }
    return setParameter(REPEAT_COUNT_KEY, repeatCount)
}

/**
 * 결과가 애니메이션된 [Drawable]인 경우 애니메이션을 반복할 횟수를 가져옵니다.
 */
fun Parameters.repeatCount(): Int? = value(REPEAT_COUNT_KEY) as Int?

/**
 * 애니메이션 [Drawable]인 경우 결과에 적용할 [AnimatedTransformation]을 설정합니다.
 *
 * Default: `null`
 *
 * @see MovieDrawable.setAnimatedTransformation
 * @see ImageDecoder.setPostProcessor
 */
@ExperimentalCoilApi
fun ImageRequest.Builder.animatedTransformation(animatedTransformation: AnimatedTransformation): ImageRequest.Builder {
    return setParameter(ANIMATED_TRANSFORMATION_KEY, animatedTransformation)
}

/**
 * 애니메이션 [Drawable]인 경우 결과에 적용될 [AnimatedTransformation]을 가져옵니다.
 */
@ExperimentalCoilApi
fun Parameters.animatedTransformation(): AnimatedTransformation? {
    return value(ANIMATED_TRANSFORMATION_KEY) as AnimatedTransformation?
}

/**
 * 결과가 애니메이션된 [Drawable]인 경우 애니메이션 시작 시 호출될 콜백을 설정합니다.
 */
fun ImageRequest.Builder.onAnimationStart(callback: (() -> Unit)?): ImageRequest.Builder {
    return setParameter(ANIMATION_START_CALLBACK_KEY, callback)
}

/**
 * 결과가 애니메이션된 [Drawable]인 경우 애니메이션 시작 시 호출될 콜백을 가져옵니다.
 */
fun Parameters.animationStartCallback(): (() -> Unit)? {
    return value(ANIMATION_START_CALLBACK_KEY) as (() -> Unit)?
}

/**
 * 결과가 애니메이션된 [Drawable]인 경우 애니메이션이 끝날 때 호출될 콜백을 설정합니다.
 */
fun ImageRequest.Builder.onAnimationEnd(callback: (() -> Unit)?): ImageRequest.Builder {
    return setParameter(ANIMATION_END_CALLBACK_KEY, callback)
}

/**
 * 결과가 애니메이션된 [Drawable]인 경우 애니메이션이 끝날 때 호출될 콜백을 가져옵니다.
 */
fun Parameters.animationEndCallback(): (() -> Unit)? {
    return value(ANIMATION_END_CALLBACK_KEY) as (() -> Unit)?
}
