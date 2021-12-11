package com.ys.coil_default.api

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.ys.coil.ImageLoader
import com.ys.coil.api.load
import com.ys.coil.api.loadAny
import com.ys.coil.request.LoadRequestBuilder
import com.ys.coil.request.RequestDisposable
import com.ys.coil_default.Coil
import okhttp3.HttpUrl
import java.io.File

// 이 파일은 ImageView에 대한 형식 안전 로드 및 가져오기 확장 함수의 컬렉션을 정의합니다.
//
// Example:
// ```
// imageView.load("https://www.example.com/image.jpg") {
//     memoryCachePolicy(CachePolicy.DISABLED)
//     size(1080, 1920)
// }
// ```

// region URL (String)

inline fun ImageView.load(
	url: String?,
	imageLoader: ImageLoader = Coil.loader(),
	builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
	return imageLoader.load(context, url) {
		target(this@load)
		builder()
	}
}

// endregion
// region URL (HttpUrl)

inline fun ImageView.load(
	url: HttpUrl?,
	imageLoader: ImageLoader = Coil.loader(),
	builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
	return imageLoader.load(context, url) {
		target(this@load)
		builder()
	}
}

// endregion
// region Uri

inline fun ImageView.load(
	uri: Uri?,
	imageLoader: ImageLoader = Coil.loader(),
	builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
	return imageLoader.load(context, uri) {
		target(this@load)
		builder()
	}
}

// endregion
// region File

inline fun ImageView.load(
	file: File?,
	imageLoader: ImageLoader = Coil.loader(),
	builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
	return imageLoader.load(context, file) {
		target(this@load)
		builder()
	}
}

// endregion
// region Resource

inline fun ImageView.load(
	@DrawableRes drawableRes: Int,
	imageLoader: ImageLoader = Coil.loader(),
	builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
	return imageLoader.load(context, drawableRes) {
		target(this@load)
		builder()
	}
}

// endregion
// region Drawable

inline fun ImageView.load(
	drawable: Drawable?,
	imageLoader: ImageLoader = Coil.loader(),
	builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
	return imageLoader.load(context, drawable) {
		target(this@load)
		builder()
	}
}

// endregion
// region Bitmap

inline fun ImageView.load(
	bitmap: Bitmap?,
	imageLoader: ImageLoader = Coil.loader(),
	builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
	return imageLoader.load(context, bitmap) {
		target(this@load)
		builder()
	}
}

// endregion
// region Any

inline fun ImageView.loadAny(
	data: Any?,
	imageLoader: ImageLoader = Coil.loader(),
	builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
	return imageLoader.loadAny(context, data) {
		target(this@loadAny)
		builder()
	}
}

// endregion