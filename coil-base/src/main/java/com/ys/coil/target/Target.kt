package com.ys.coil.target

import android.graphics.drawable.Drawable
import androidx.annotation.MainThread

/**
 * 이미지 로드 결과를 수락하는 리스너.
 *
 * 각 수명 주기 메서드는 최대 한 번만 호출됩니다.
 * [onSuccess] 및 [onError]는 상호 배타적입니다.
 */
interface Target {
    /**
     * 이미지 요청이 시작될때 호출 됩니다.
     */
    @MainThread
    fun onStart(placeHolder: Drawable?) {}

    /**
     * 이미지 요청이 성공하면 호출 됩니다.
     */
    @MainThread
    fun onSuccess(placeHolder: Drawable?) {}

    /**
     * 이미지 요청이 실패하면 호출 됩니다.
     */
    @MainThread
    fun onError(error: Drawable?) {}
}