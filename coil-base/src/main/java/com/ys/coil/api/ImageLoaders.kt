package com.ys.coil.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.DrawableRes
import com.ys.coil.ImageLoader
import com.ys.coil.request.GetRequestBuilder
import com.ys.coil.request.LoadRequestBuilder
import com.ys.coil.request.RequestDisposable
import okhttp3.HttpUrl
import java.io.File

// 이 파일은  ImageLoader 의 타입을 안전하게 불러오고 확장함수 컬렉션을 정의 합니다.
// Example:
// ```
// imageLoader.load(context, "https://www.example.com/image.jpg") {
//     memoryCachePolicy(CachePolicy.DISABLED)
//     size(1080, 1920)
// }
// ```

// region URL (String)

inline fun ImageLoader.load(
    context: Context,
    url: String?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = load(LoadRequestBuilder(context, defaults).data(url).apply(builder).build())

suspend inline fun ImageLoader.get(
    url: String,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get(GetRequestBuilder(defaults).data(url).apply(builder).build())

// endregion

// region URL (HttpUrl)

inline fun ImageLoader.load(
    context: Context,
    url: HttpUrl?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = load(LoadRequestBuilder(context, defaults).data(url).apply(builder).build())

suspend inline fun ImageLoader.get(
    url: HttpUrl,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get(GetRequestBuilder(defaults).data(url).apply(builder).build())

// endregion

// region Uri

inline fun ImageLoader.load(
    context: Context,
    uri: Uri?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = load(LoadRequestBuilder(context, defaults).data(uri).apply(builder).build())

suspend inline fun ImageLoader.get(
    uri: Uri,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get(GetRequestBuilder(defaults).data(uri).apply(builder).build())

// endregion

// region File

inline fun ImageLoader.load(
    context: Context,
    file: File?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = load(LoadRequestBuilder(context, defaults).data(file).apply(builder).build())

suspend inline fun ImageLoader.get(
    file: File,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get(GetRequestBuilder(defaults).data(file).apply(builder).build())

// endregion

// region Resource

inline fun ImageLoader.load(
    context: Context,
    @DrawableRes drawableRes: Int,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = load(LoadRequestBuilder(context, defaults).data(drawableRes).apply(builder).build())

suspend inline fun ImageLoader.get(
    @DrawableRes drawableRes: Int,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get(GetRequestBuilder(defaults).data(drawableRes).apply(builder).build())

// endregion

// region Drawable

inline fun ImageLoader.load(
    context: Context,
    drawable: Drawable?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = load(LoadRequestBuilder(context, defaults).data(drawable).apply(builder).build())

suspend inline fun ImageLoader.get(
    drawable: Drawable,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get(GetRequestBuilder(defaults).data(drawable).apply(builder).build())

// endregion

// region Bitmap

inline fun ImageLoader.load(
    context: Context,
    bitmap: Bitmap?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = load(LoadRequestBuilder(context, defaults).data(bitmap).apply(builder).build())

suspend inline fun ImageLoader.get(
    bitmap: Bitmap,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get(GetRequestBuilder(defaults).data(bitmap).apply(builder).build())

// endregion

// region Any

inline fun ImageLoader.loadAny(
    context: Context,
    data: Any?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = load(LoadRequestBuilder(context, defaults).data(data).apply(builder).build())

suspend inline fun ImageLoader.getAny(
    data: Any,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get(GetRequestBuilder(defaults).data(data).apply(builder).build())

// endregion

// region Request Creation

inline fun ImageLoader.newLoadBuilder(context: Context) = LoadRequestBuilder(context, defaults)

inline fun ImageLoader.newGetBuilder() = GetRequestBuilder(defaults)

// endregion