package com.ys.coil.disk

import com.ys.coil.disk.DiskCache.Editor
import com.ys.coil.disk.DiskCache.Snapshot
import java.io.File

internal class RealDiskCache(
	maxSize: Long,
	directory: File
) : DiskCache {

	override val size: Long
		get() = TODO("Not yet implemented")

	override val maxSize: Long
		get() = TODO("Not yet implemented")

	override val directory: File
		get() = TODO("Not yet implemented")

	override fun get(key: String): Snapshot {
		TODO("Not yet implemented")
	}

	override fun edit(key: String): Editor? {
		TODO("Not yet implemented")
	}

	override fun remove(key: String): Boolean {
		TODO("Not yet implemented")
	}

	override fun clear() {
		TODO("Not yet implemented")
	}
}