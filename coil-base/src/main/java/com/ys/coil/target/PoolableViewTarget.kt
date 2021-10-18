package com.ys.coil.target

import android.view.View
import androidx.annotation.MainThread

interface PoolableViewTarget<T: View>: ViewTarget<T> {
    /**
     * 현재 Drawable을 더 이상 사용할 수 없을 때 호출됩니다. 대상은 **반드시** 현재 드로어블 사용을 중지해야 합니다.
     *
     * 실제로 이것은 뷰가 분리되거나 파괴되려고 할 때만 호출됩니다.
     */
    @MainThread
    fun onClear()
}