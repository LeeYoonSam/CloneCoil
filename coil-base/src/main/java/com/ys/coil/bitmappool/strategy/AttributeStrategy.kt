package com.ys.coil.bitmappool.strategy

import android.graphics.Bitmap
import androidx.annotation.Px
import com.ys.coil.collection.GroupedLinkedMap

/**
 * 반환된 비트맵의 치수가 요청한 치수와 정확히 일치해야 하는 비트맵 재사용 전략입니다.
 */
internal class AttributeStrategy : BitmapPoolStrategy {

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        private inline fun getBitmapString(width: Int, height: Int, config: Bitmap.Config) = "[$width x $height], $config"
    }

    private val groupedMap = GroupedLinkedMap<Key, Bitmap>()

    override fun put(bitmap: Bitmap) {
        groupedMap[Key(bitmap.width, bitmap.height, bitmap.config)] = bitmap
    }

    override fun get(width: Int, height: Int, config: Bitmap.Config): Bitmap? {
        return groupedMap[Key(width, height, config)]
    }

    override fun removeLast(): Bitmap? {
        return groupedMap.removeLast()
    }

    override fun logBitmap(bitmap: Bitmap): String {
        return getBitmapString(bitmap.width, bitmap.height, bitmap.config)
    }

    override fun logBitmap(width: Int, height: Int, config: Bitmap.Config): String {
        return getBitmapString(width, height, config)
    }

    override fun toString() = "AttributeStrategy: groupedMap=$groupedMap"

    private data class Key(
        @Px val width: Int,
        @Px val height: Int,
        val config: Bitmap.Config
    ) {
        override fun toString() = getBitmapString(width, height, config)
    }
}
