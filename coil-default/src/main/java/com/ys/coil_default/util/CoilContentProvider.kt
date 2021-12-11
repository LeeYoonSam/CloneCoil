package com.ys.coil_default.util

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.ys.coil_default.Coil

/**
 * [Coil] 싱글톤을 초기화하는 데 [Context]가 사용되는 [ContentProvider]입니다.
 */
internal class CoilContentProvider: ContentProvider() {
	companion object {
		@SuppressLint("StaticFieldLeak")
		internal lateinit var context: Context
			private set
	}

	override fun onCreate(): Boolean {
		Companion.context = checkNotNull(context)
		return true
	}

	override fun query(
		p0: Uri,
		p1: Array<out String>?,
		p2: String?,
		p3: Array<out String>?,
		p4: String?
	): Cursor?  = null

	override fun getType(p0: Uri): String? = null

	override fun insert(p0: Uri, p1: ContentValues?): Uri? = null

	override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int = 0

	override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int = 0
}