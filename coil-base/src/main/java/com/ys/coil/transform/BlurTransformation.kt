package com.ys.coil.transform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.annotation.RequiresApi
import androidx.core.graphics.applyCanvas
import com.ys.coil.bitmappool.BitmapPool

/**
 * 이미지에 가우시안 블러를 적용하는 [Transformation]
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class BlurTransformation(
    private val context: Context,
    private val radius: Float = DEFAULT_RADIUS,
    private val sampling: Float = DEFAULT_SAMPLING
) : Transformation {

    init {
        require(radius >= 0) { "Radius must be >= 0." }
        require(sampling > 0) { "Sampling must be > 0." }
    }

    companion object {
        private const val DEFAULT_RADIUS = 10f
        private const val DEFAULT_SAMPLING = 1f
    }

    override fun key(): String = "${BlurTransformation::class.java.name}-$radius-$sampling"

    override suspend fun transform(pool: BitmapPool, input: Bitmap): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val scaledWidth = input.width / sampling
        val scaledHeight = input.height / sampling
        val output = pool.get(scaledWidth.toInt(), scaledHeight.toInt(), input.config)
        output.applyCanvas {
            scale(1 / sampling, 1 / sampling)
            drawBitmap(input, 0f, 0f, paint)
        }

        var script: RenderScript? = null
        var tmpInt: Allocation? = null
        var tmpOut: Allocation? = null
        var blur: ScriptIntrinsicBlur? = null
        try {
            script = RenderScript.create(context)
            tmpInt = Allocation.createFromBitmap(script, output, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT)
            tmpOut = Allocation.createTyped(script, tmpInt.type)
            blur = ScriptIntrinsicBlur.create(script, Element.U8_4(script))
            blur.setRadius(radius)
            blur.setInput(tmpInt)
            blur.forEach(tmpOut)
            tmpOut.copyTo(output)
        } finally {
            script?.destroy()
            tmpInt?.destroy()
            tmpOut?.destroy()
            blur?.destroy()
        }
        pool.put(input)

        return output
    }
}