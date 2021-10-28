package com.ys.coil.memory

import android.graphics.Bitmap
import android.util.Log
import android.util.SparseIntArray
import androidx.annotation.VisibleForTesting
import androidx.core.util.set
import com.ys.coil.bitmappool.BitmapPool
import com.ys.coil.collection.SparseIntArraySet
import com.ys.coil.collection.plusAssign
import com.ys.coil.util.log
import java.lang.ref.WeakReference

/**
 * [Bitmap]에 대한 참조를 계산합니다. 더 이상 참조되지 않는 경우 [bitmapPool]에 비트맵을 추가합니다.
 *
 * 참고: 이 클래스는 스레드로부터 안전하지 않습니다. 실제로는 메인 스레드에서만 호출됩니다.
 */
internal class BitmapReferenceCounter(private val bitmapPool: BitmapPool) {

    companion object {
        private const val TAG = "BitmapReferenceCounter"
    }

    private val counts = SparseIntArray()
    private val invalidKeys = SparseIntArraySet()

    /**
     * 이 [Bitmap]에 대한 참조 카운트를 1만큼 늘립니다.
     */
    fun increment(bitmap: Bitmap) {
        val key = bitmap.key()
        val count = counts[key]
        val newCount = count + 1
        counts[key] = newCount
        log(TAG, Log.VERBOSE) { "INCREMENT: [$key, $newCount]" }
    }

    /**
     * 이 [Bitmap]에 대한 참조 카운트를 하나씩 줄입니다.
     *
     * 참조 횟수가 이제 0이면 [Bitmap]을 [bitmapPool]에 추가합니다.
     */
    fun decrement(bitmap: Bitmap) {
        val key = bitmap.key()
        val count = counts[key]
        val newCount = count - 1
        counts[key] = newCount
        log(TAG, Log.VERBOSE) { "DECREMENT: [$key, $newCount]" }

        if (newCount <= 0) {
            counts.delete(key)
            val isValid = !invalidKeys.remove(key)
            if (isValid) {
                bitmapPool.put(bitmap)
            }
        }
    }

    /**
     * 이 비트맵을 유효하지 않은 것으로 표시하여 더 이상 참조되지 않을 때 비트맵 풀로 반환되지 않도록 합니다.
     */
    fun invalidate(bitmap: Bitmap) {
        invalidKeys += bitmap.key()
    }

    @VisibleForTesting
    fun count(bitmap: Bitmap): Int {
        return counts[bitmap.key()]
    }

    @VisibleForTesting
    fun invalid(bitmap: Bitmap): Boolean {
        return invalidKeys.contains(bitmap.key())
    }

    /**
     * [System.identityHashCode]는 [Bitmap]에 대해 "고유한" 키를 제공하므로 [WeakReference] 사용을 피할 수 있습니다.
     */
    private inline fun Bitmap.key(): Int {
        return System.identityHashCode(this)
    }
}