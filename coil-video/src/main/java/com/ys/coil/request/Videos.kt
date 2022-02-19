@file:Suppress("unused")
@file:JvmName("Videos")

package com.ys.coil.request

import android.media.MediaMetadataRetriever.OPTION_CLOSEST
import android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
import android.media.MediaMetadataRetriever.OPTION_NEXT_SYNC
import android.media.MediaMetadataRetriever.OPTION_PREVIOUS_SYNC
import com.ys.coil.decode.VideoFrameDecoder
import com.ys.coil.decode.VideoFrameDecoder.Companion.VIDEO_FRAME_MICROS_KEY
import com.ys.coil.decode.VideoFrameDecoder.Companion.VIDEO_FRAME_OPTION_KEY

/**
 * 비디오에서 추출할 프레임의 시간을 **밀리초**로 설정합니다.
 *
 * 기본값: 0
 */
fun ImageRequest.Builder.videoFrameMillis(frameMillis: Long): ImageRequest.Builder {
	return videoFrameMicros(1000 * frameMillis)
}

/**
 * 비디오에서 추출할 프레임의 시간 **마이크로초**를 설정합니다.
 *
 * 기본값: 0
 */
fun ImageRequest.Builder.videoFrameMicros(frameMicros: Long): ImageRequest.Builder {
	require(frameMicros >= 0 ) { "frameMicros must be >= 0." }
	return setParameter(VideoFrameDecoder.VIDEO_FRAME_MICROS_KEY, frameMicros)
}

/**
 * 비디오 프레임을 디코딩하는 방법에 대한 옵션을 설정합니다.
 *
 * [OPTION_PREVIOUS_SYNC], [OPTION_NEXT_SYNC], [OPTION_CLOSEST_SYNC], [OPTION_CLOSEST] 중 하나여야 합니다.
 *
 * 기본값: [OPTION_CLOSEST_SYNC]
 *
 * @see MediaMetadataRetriever
 */
fun ImageRequest.Builder.videoFrameOption(option: Int): ImageRequest.Builder {
	require(option == OPTION_PREVIOUS_SYNC ||
		option == OPTION_NEXT_SYNC ||
		option == OPTION_CLOSEST_SYNC ||
		option == OPTION_CLOSEST) { "Invalid video frame option: $option." }
	return setParameter(VIDEO_FRAME_OPTION_KEY, option)
}

/**
 * 비디오에서 추출할 프레임의 시간 **마이크로초**를 가져옵니다.
 */
fun Parameters.videoFrameMicros(): Long? = value(VIDEO_FRAME_MICROS_KEY) as Long?

/**
 * 비디오 프레임을 디코딩하는 방법에 대한 옵션을 가져옵니다.
 */
fun Parameters.videoFrameOption(): Int? = value(VIDEO_FRAME_OPTION_KEY) as Int?
