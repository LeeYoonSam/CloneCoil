package com.ys.coil.compose

import android.app.Application
import android.os.Handler
import android.os.StrictMode

class TestApplication : Application() {

	override fun onCreate() {
		super.onCreate()

		// 'MockWebServer.url()'은 내부적으로 네트워크 검사를 수행하고 엄격 모드를 트리거합니다.
		// 테스트에서 이 문제를 해결하기 위해 메인 스레드에서 네트워크를 허용합니다.
		Handler(mainLooper).post {
			val threadPolicy = StrictMode.ThreadPolicy.Builder()
				.detectNetwork()
				.permitNetwork()
				.build()
			StrictMode.setThreadPolicy(threadPolicy)
		}
	}
}
