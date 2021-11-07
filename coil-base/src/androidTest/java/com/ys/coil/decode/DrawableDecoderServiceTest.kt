package com.ys.coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import com.ys.coil.bitmappool.RealBitmapPool
import com.ys.coil.size.PixelSize
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class DrawableDecoderServiceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var service: DrawableDecoderService

    @Before
    fun before() {
        service = DrawableDecoderService(context, RealBitmapPool(0))
    }

//    /**
//     * 참고: 이 테스트는 기본 라이브러리에서 AppCompatResources를 가져오지 않기 때문에 롤리팝 이전 버전에서는 실패합니다.
//     */
//    @Test
//    fun vectorIsConvertedCorrectly() {
//        val output = service.convertIfNecessary(
//            drawable = context.getDrawableCompat(R.drawable.ic_android),
//            size = PixelSize(200, 200),
//            config = Bitmap.Config.ARGB_8888
//        )
//
//        assertTrue(output is BitmapDrawable)
//        assertTrue(output.bitmap.run { width == 200 && height == 200 })
//    }

    @Test
    fun colorIsNotConverted() {
        val input = ColorDrawable(Color.BLACK)
        val output = service.convertIfNecessary(
            drawable = input,
            size = PixelSize(200, 200),
            config = Bitmap.Config.ARGB_8888
        )

        assertEquals(input, output)
    }
}