package com.ys.coil.util

import android.graphics.Bitmap
import android.os.Build
import android.os.Build.VERSION
import android.os.SystemClock
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.ys.coil.size.PixelSize
import com.ys.coil.size.Size
import java.io.File

/** 새 [HardwareBitmapService]를 만듭니다. */
internal fun HardwareBitmapService(logger: Logger?): HardwareBitmapService = when {
	VERSION.SDK_INT < 26 || IS_DEVICE_BLOCKED -> ImmutableHardwareBitmapService(false)
	VERSION.SDK_INT == 26 || VERSION.SDK_INT == 27 -> LimitedFileDescriptorHardwareBitmapService(logger)
	else -> ImmutableHardwareBitmapService(true)
}

/** 요청이 [Bitmap.Config.HARDWARE]를 사용할 수 있는지 결정합니다. */
internal sealed class HardwareBitmapService {

	/** 현재 [Bitmap.Config.HARDWARE]를 생성할 수 있으면 'true'를 반환합니다. */
	@MainThread
	abstract fun allowHardwareMainThread(size: Size): Boolean

	/** 주 스레드에서 수행할 수 없는 하드웨어 비트맵 할당 검사를 수행합니다. */
	@WorkerThread
	abstract fun allowHardwareWorkerThread(): Boolean
}

/** [allowHardwareMainThread] 및 [allowHardwareWorkerThread]에 대한 고정 값을 반환합니다. */
private class ImmutableHardwareBitmapService(private val allowHardware: Boolean) : HardwareBitmapService() {
	override fun allowHardwareMainThread(size: Size) = allowHardware
	override fun allowHardwareWorkerThread() = allowHardware
}

/** 파일 설명자가 부족하지 않도록 보호합니다. */
private class LimitedFileDescriptorHardwareBitmapService(
	private val logger: Logger?
) : HardwareBitmapService() {

	override fun allowHardwareMainThread(size: Size): Boolean {
		return size !is PixelSize ||
			(size.width >= MIN_SIZE_DIMENSION && size.height >= MIN_SIZE_DIMENSION)
	}

	override fun allowHardwareWorkerThread(): Boolean {
		return FileDescriptorCounter.hasAvailableFileDescriptors(logger)
	}

	companion object {
		private const val MIN_SIZE_DIMENSION = 100
	}
}

/**
 * API 26 및 27에는 프로세스당 제한된 수의 파일 설명자(1024)가 있습니다.
 * 이 제한은 API 28(32768)에서 안전한 숫자로 증가되었습니다. 각 하드웨어 비트맵 할당은 평균적으로 2개의 파일 설명자를 사용합니다.
 * 또한 이미지가 아닌 다른 로드 작업에서 파일 설명자를 사용할 수 있으므로 이러한 리소스에 대한 경쟁이 증가합니다.
 * 이 클래스는 이 프로세스가 제한에 너무 가까워지면 [Bitmap.Config.HARDWARE] 할당을 비활성화하기 위해 존재합니다.
 * 제한을 통과하면 충돌 및/또는 렌더링 문제가 발생할 수 있기 때문입니다.
 *
 * 참고: 파일 설명자 사용량이 전체 프로세스에서 공유되므로 싱글톤이어야 합니다.
 */
private object FileDescriptorCounter {

	private const val TAG = "FileDescriptorCounter"
	private const val FILE_DESCRIPTOR_LIMIT = 800
	private const val FILE_DESCRIPTOR_CHECK_INTERVAL_DECODES = 30
	private const val FILE_DESCRIPTOR_CHECK_INTERVAL_MILLIS = 30_000

	private val fileDescriptorList = File("/proc/self/fd")
	private var decodesSinceLastFileDescriptorCheck = FILE_DESCRIPTOR_CHECK_INTERVAL_DECODES
	private var lastFileDescriptorCheckTimestamp = SystemClock.uptimeMillis()
	private var hasAvailableFileDescriptors = true

	@Synchronized
	@WorkerThread
	fun hasAvailableFileDescriptors(logger: Logger?): Boolean {
		if (checkFileDescriptors()) {
			decodesSinceLastFileDescriptorCheck = 0
			lastFileDescriptorCheckTimestamp = SystemClock.uptimeMillis()

			val numUsedFileDescriptors = fileDescriptorList.list().orEmpty().count()
			hasAvailableFileDescriptors = numUsedFileDescriptors < FILE_DESCRIPTOR_LIMIT
			if (!hasAvailableFileDescriptors) {
				logger?.log(TAG, Log.WARN) {
					"Unable to allocate more hardware bitmaps. " +
						"Number of used file descriptors: $numUsedFileDescriptors"
				}
			}
		}
		return hasAvailableFileDescriptors
	}

	private fun checkFileDescriptors(): Boolean {
		// 비용이 많이 들기 때문에(1-2밀리초) 정해진 시간/디코딩 후에 사용 가능한 파일 디스크립터가 있는지 확인하십시오.
		return decodesSinceLastFileDescriptorCheck++ >= FILE_DESCRIPTOR_CHECK_INTERVAL_DECODES ||
			SystemClock.uptimeMillis() > lastFileDescriptorCheckTimestamp + FILE_DESCRIPTOR_CHECK_INTERVAL_MILLIS
	}
}

/**
 * 손상/불완전/불안정한 하드웨어 비트맵 구현이 있는 장치 목록을 유지 관리합니다.
 *
 * 모델명은 [Google 공식 기기 목록](https://support.google.com/googleplay/answer/1727131?hl=ko)에서 가져옵니다.
 */
private val IS_DEVICE_BLOCKED = when (VERSION.SDK_INT) {
	26 -> run {
		val model = Build.MODEL ?: return@run false

		// Samsung Galaxy (ALL)
		if (model.removePrefix("SAMSUNG-").startsWith("SM-")) return@run true

		val device = Build.DEVICE ?: return@run false

		return@run device in arrayOf(
			"nora", "nora_8917", "nora_8917_n", // Moto E5
			"james", "rjames_f", "rjames_go", "pettyl", // Moto E5 Play
			"hannah", "ahannah", "rhannah", // Moto E5 Plus

			"ali", "ali_n", // Moto G6
			"aljeter", "aljeter_n", "jeter", // Moto G6 Play
			"evert", "evert_n", "evert_nt", // Moto G6 Plus

			"G3112", "G3116", "G3121", "G3123", "G3125", // Xperia XA1
			"G3412", "G3416", "G3421", "G3423", "G3426", // Xperia XA1 Plus
			"G3212", "G3221", "G3223", "G3226", // Xperia XA1 Ultra

			"BV6800Pro", // BlackView BV6800Pro
			"CatS41", // Cat S41
			"Hi9Pro", // CHUWI Hi9 Pro
			"manning", // Lenovo K8 Note
			"N5702L" // NUU Mobile G3
		)
	}
	27 -> run {
		val device = Build.DEVICE ?: return@run false

		return@run device in arrayOf(
			"mcv1s", // LG Tribute Empire
			"mcv3", // LG K11
			"mcv5a", // LG Q7
			"mcv7a", // LG Stylo 4

			"A30ATMO", // T-Mobile REVVL 2
			"A70AXLTMO", // T-Mobile REVVL 2 PLUS

			"A3A_8_4G_TMO", // Alcatel 9027W
			"Edison_CKT", // Alcatel ONYX
			"EDISON_TF", // Alcatel TCL XL2
			"FERMI_TF", // Alcatel A501DL
			"U50A_ATT", // Alcatel TETRA
			"U50A_PLUS_ATT", // Alcatel 5059R
			"U50A_PLUS_TF", // Alcatel TCL LX
			"U50APLUSTMO", // Alcatel 5059Z
			"U5A_PLUS_4G", // Alcatel 1X

			"RCT6513W87DK5e", // RCA Galileo Pro
			"RCT6873W42BMF9A", // RCA Voyager
			"RCT6A03W13", // RCA 10 Viking
			"RCT6B03W12", // RCA Atlas 10 Pro
			"RCT6B03W13", // RCA Atlas 10 Pro+
			"RCT6T06E13", // RCA Artemis 10

			"A3_Pro", // Umidigi A3 Pro
			"One", // Umidigi One
			"One_Max", // Umidigi One Max
			"One_Pro", // Umidigi One Pro
			"Z2", // Umidigi Z2
			"Z2_PRO", // Umidigi Z2 Pro

			"Armor_3", // Ulefone Armor 3
			"Armor_6", // Ulefone Armor 6

			"Blackview", // Blackview BV6000
			"BV9500", // Blackview BV9500
			"BV9500Pro", // Blackview BV9500Pro

			"A6L-C", // Nuu A6L-C
			"N5002LA", // Nuu A7L
			"N5501LA", // Nuu A5L

			"Power_2_Pro", // Leagoo Power 2 Pro
			"Power_5", // Leagoo Power 5
			"Z9", // Leagoo Z9

			"V0310WW", // Blu VIVO VI+
			"V0330WW", // Blu VIVO XI

			"A3", // BenQ A3
			"ASUS_X018_4", // Asus ZenFone Max Plus M1 (ZB570TL)
			"C210AE", // Wiko Life
			"fireball", // DROID Incredible 4G LTE
			"ILA_X1", // iLA X1
			"Infinix-X605_sprout", // Infinix NOTE 5 Stylus
			"j7maxlte", // Samsung Galaxy J7 Max
			"KING_KONG_3", // Cubot King Kong 3
			"M10500", // Packard Bell M10500
			"S70", // Altice ALTICE S70
			"S80Lite", // Doogee S80Lite
			"SGINO6", // SGiNO 6
			"st18c10bnn", // Barnes and Noble BNTV650
			"TECNO-CA8" // Tecno CAMON X Pro
		)
	}
	else -> false
}
