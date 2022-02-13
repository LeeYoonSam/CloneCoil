package com.ys.coil

import android.app.Application
import android.os.Build.VERSION
import android.util.Log
import com.ys.coil.disk.DiskCache
import com.ys.coil.gif.decode.GifDecoder
import com.ys.coil.gif.decode.ImageDecoderDecoder
import com.ys.coil.memory.MemoryCache
import com.ys.coil.util.DebugLogger
import okhttp3.Dispatcher
import okhttp3.OkHttpClient

class Application : Application(), ImageLoaderFactory {
	override fun newImageLoader(): ImageLoader {
		return ImageLoader.Builder(this)
			.components {
				// GIFs
				if (VERSION.SDK_INT >= 28) {
					add(ImageDecoderDecoder.Factory())
				} else {
					add(GifDecoder.Factory())
				}
			}
			.memoryCache {
				MemoryCache.Builder(this)
					// 최대 크기를 앱의 사용 가능한 메모리의 25%로 설정합니다.
					.maxSizePercent(0.25)
					.build()
			}
			.diskCache {
				DiskCache.Builder(this)
					.directory(filesDir.resolve("image_cache"))
					.maxSizeBytes(512L * 1024 * 1024) // 512MB
					.build()
			}
			.okHttpClient {
				// 호스트별로 동시 네트워크 요청을 제한하지 마십시오.
				val dispatcher = Dispatcher().apply { maxRequestsPerHost = maxRequests }

				// 네트워크 작업에 사용되는 OkHttpClient를 Lazily 생성합니다.
				OkHttpClient.Builder()
					.dispatcher(dispatcher)
					.build()
			}
			// 이미지를 비동기식으로 로드할 때 짧은 크로스페이드를 표시합니다.
			.crossfade(true)
			// 네트워크 캐시 헤더를 무시하고 항상 디스크 캐시에서 읽고 쓰십시오.
			.respectCacheHeaders(false)
			.apply {
				// 디버그 빌드인 경우 표준 Android 로그에 대한 로깅을 활성화합니다.
				if (BuildConfig.DEBUG) {
					logger(DebugLogger(Log.VERBOSE))
				}
			}
			.build()
	}
}
