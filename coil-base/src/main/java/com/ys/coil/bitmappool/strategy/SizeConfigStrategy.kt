package com.ys.coil.bitmappool.strategy

import android.graphics.Bitmap

class SizeConfigStrategy : BitmapPoolStrategy {
    override fun put(bitmap: Bitmap) {
        TODO("Not yet implemented")
    }

    override fun get(width: Int, height: Int, config: Bitmap.Config): Bitmap? {
        TODO("Not yet implemented")
    }

    override fun removeLast(): Bitmap? {
        TODO("Not yet implemented")
    }

    override fun logBitmap(bitmap: Bitmap): String {
        TODO("Not yet implemented")
    }

    override fun logBitmap(width: Int, height: Int, config: Bitmap.Config): String {
        TODO("Not yet implemented")
    }

}
