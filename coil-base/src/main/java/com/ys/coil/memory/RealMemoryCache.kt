package com.ys.coil.memory

import com.ys.coil.memory.MemoryCache.Key
import com.ys.coil.memory.MemoryCache.Value
import com.ys.coil.util.toImmutableMap

internal class RealMemoryCache(
	private val strongMemoryCache: StrongMemoryCache,
	private val weakMemoryCache: WeakMemoryCache
) : MemoryCache {
	override val size get() = strongMemoryCache.size

	override val maxSize get() = strongMemoryCache.maxSize

	override val keys get() = strongMemoryCache.keys + weakMemoryCache.keys

	override fun get(key: Key): Value? {
		return strongMemoryCache.get(key) ?: weakMemoryCache.get(key)
	}

	override fun set(key: Key, value: Value) {
		// 저장된 키와 값을 변경할 수 없는지 확인합니다.
		strongMemoryCache.set(
			key = key.copy(extras = key.extras.toImmutableMap()),
			bitmap = value.bitmap,
			extras = value.extras.toImmutableMap()
		)
		// weakMemoryCache.set()은 값이 강력한 참조 캐시에서 제거될 때 strongMemoryCache에 의해 호출됩니다.
	}

	override fun remove(key: Key): Boolean {
		// 회귀 테스트가 있습니다.
		val removedStrong = strongMemoryCache.remove(key)
		val removedWeak = weakMemoryCache.remove(key)
		return removedStrong || removedWeak
	}

	override fun clear() {
		strongMemoryCache.clearMemory()
		weakMemoryCache.clearMemory()
	}

	override fun trimMemory(level: Int) {
		strongMemoryCache.trimMemory(level)
		weakMemoryCache.trimMemory(level)
	}
}