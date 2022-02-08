package com.ys.coil.test.util

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** [TestActivity]를 시작하고 [block]을 호출합니다. */
fun withTestActivity(block: (TestActivity) -> Unit) {
	launchActivity<TestActivity>().use { scenario ->
		scenario.moveToState(Lifecycle.State.RESUMED)
		scenario.onActivity(block)
	}
}

/**
 * [ActivityScenario]의 [Activity]에 대한 참조를 가져옵니다.
 *
 * 참고:
 * [ActivityScenario.onActivity]는 범위 외부에서 [Activity]에 대한 참조를 보유하지 말 것을 명시적으로 권장합니다.
 * 그러나 [ActivityScenarioRule]을 사용하는 한 안전해야 합니다.
 */
val <T: Activity> ActivityScenario<T>.activity: T
	get() {
		lateinit var activity: T
		runBlocking(Dispatchers.Main.immediate) {
			// onActivity는 메인 스레드에서 호출될 때 동기적으로 실행됩니다.
			onActivity { activity = it }
		}
		return activity
	}
