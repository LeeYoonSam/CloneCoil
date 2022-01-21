package com.ys.coil.test.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import okhttp3.Headers
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.buffer
import okio.source
import kotlin.coroutines.CoroutineContext

fun createMockWebServer(vararg images: String): MockWebServer {
	val server = MockWebServer()
	images.forEach { server.enqueueImage(it) }
	return server.apply { start() }
}

fun MockWebServer.enqueueImage(image: String, headers: Headers = Headers.headersOf()): Long {
	val buffer = Buffer()
	val context = ApplicationProvider.getApplicationContext<Context>()
	context.assets.open(image).source().buffer().readAll(buffer)
	enqueue(MockResponse().setHeaders(headers).setBody(buffer))
	return buffer.size
}

/** Runs the given [block] on the main thread by default. */
fun runBlockingTest(
	context: CoroutineContext = Dispatchers.Main.immediate,
	block: suspend CoroutineScope.() -> Unit
) = runBlocking(context, block)

@OptIn(ExperimentalCoroutinesApi::class)
fun createTestMainDispatcher(): TestCoroutineDispatcher {
	return TestCoroutineDispatcher().apply { Dispatchers.setMain(this) }
}
