package com.ys.coil

import android.content.Context
import android.widget.ImageView
import com.ys.coil.request.Disposable
import com.ys.coil.request.ImageRequest
import com.ys.coil.request.ImageResult
import com.ys.coil.util.CoilUtils

/**
 * 싱글톤 [ImageLoader]를 가져옵니다.
 */
inline val Context.imageLoader: ImageLoader
	get() = Coil.imageLoader(this)

/**
 * [data]가 참조하는 이미지를 불러와서 이 [ImageView]에 설정합니다.
 *
 * 예시:
 * ```
 * imageView.load("https://www.example.com/image.jpg") {
 * 크로스페이드(true)
 * 변형(CircleCropTransformation())
 * }
 * ```
 *
 * 기본적으로 지원되는 [data] 유형은 다음과 같습니다.
 *
 * - [String] (treated as a [Uri])
 * - [Uri] (`android.resource`, `content`, `file`, `http`, and `https` schemes)
 * - [HttpUrl]
 * - [File]
 * - [DrawableRes] [Int]
 * - [Drawable]
 * - [Bitmap]
 * - [ByteBuffer]
 *
 * @param data 로드할 데이터입니다.
 * @param imageLoader [ImageRequest]를 대기열에 넣는 데 사용할 [ImageLoader]입니다.
 *  기본적으로 싱글톤 [ImageLoader]가 사용됩니다.
 * @param builder [ImageRequest]를 구성하기 위한 선택적 람다.
 */
inline fun ImageView.load(
	data: Any?,
	imageLoader: ImageLoader = context.imageLoader,
	builder: ImageRequest.Builder.() -> Unit = {}
): Disposable {
	val request = ImageRequest.Builder(context)
		.apply(builder)
		.data(data)
		.target(this)
		.build()

	return imageLoader.enqueue(request)
}

/**
 * 이 뷰에 첨부된 요청을 삭제합니다(있는 경우).
 */
inline fun ImageView.dispose() {
	CoilUtils.dispose(this)
}

/**
 * 이 뷰에 첨부된 가장 최근에 실행된 이미지 요청의 [ImageResult]를 가져옵니다.
 */
inline val ImageView.result: ImageResult?
	get() = CoilUtils.result(this)

@Deprecated(
	message = "Migrate to 'load'.",
	replaceWith = ReplaceWith(
		expression = "load(data, imageLoader, builder)",
		imports = ["coil.imageLoader", "coil.load"]
	),
	level = DeprecationLevel.ERROR // Temporary migration aid.
)
inline fun ImageView.loadAny(
	data: Any?,
	imageLoader: ImageLoader = context.imageLoader,
	builder: ImageRequest.Builder.() -> Unit = {}
) = load(data, imageLoader, builder)

@Deprecated(
	message = "Migrate to 'dispose'.",
	replaceWith = ReplaceWith(
		expression = "dispose",
		imports = ["coil.dispose"]
	),
	level = DeprecationLevel.ERROR // Temporary migration aid.
)

inline fun ImageView.clear() = dispose()
