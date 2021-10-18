package com.ys.coil.target

import android.graphics.drawable.Drawable

class FakeTarget : Target {
    var start = false
    var success = false
    var error = false

    override fun onStart(placeHolder: Drawable?) {
        this.start = true
    }

    override fun onSuccess(result: Drawable) {
        this.success = true
    }

    override fun onError(error: Drawable?) {
        this.error = false
    }
}