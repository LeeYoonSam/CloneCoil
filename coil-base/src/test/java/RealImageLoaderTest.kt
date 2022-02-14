import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ys.coil.ImageLoader
import com.ys.coil.fetch.Fetcher
import com.ys.coil.request.ImageRequest
import com.ys.coil.test.util.createTestMainDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class RealImageLoaderTest {
	private lateinit var context: Context
	private lateinit var mainDispatcher: TestCoroutineDispatcher
	private lateinit var imageLoader: ImageLoader

	@Before
	fun before() {
		context = ApplicationProvider.getApplicationContext()
		mainDispatcher = createTestMainDispatcher()
		imageLoader = ImageLoader.Builder(context)
			.diskCache(null)
			.build()
	}

	@After
	fun after() {
		Dispatchers.resetMain()
	}

	/** Regression test: https://github.com/coil-kt/coil/issues/933 */
	@Test
	fun executeIsCancelledIfScopeIsCancelled() {
		val isCancelled = MutableStateFlow(false)

		val scope = CoroutineScope(mainDispatcher)
		scope.launch {
			val request = ImageRequest.Builder(context)
				.data(Unit)
				.dispatcher(mainDispatcher)
				.fetcherFactory<Unit> { _, _, _ ->
					// 취소할 때까지 일시 중단되는 사용자 지정 페처를 사용합니다.
					Fetcher { awaitCancellation() }
				}
				.listener(onCancel = {
					isCancelled.value = true
				})
				.build()
			imageLoader.execute(request)
		}

		assertTrue(scope.isActive)
		assertFalse(isCancelled.value)

		scope.cancel()

		// 요청이 취소될 때까지 일시 중단합니다.
		runBlocking {
			isCancelled.first { it }
		}
	}
}
