package com.ys.coil.test.util

import androidx.annotation.FloatRange
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 두 배열 간의 상호 상관 관계를 반환합니다.
 *
 * https://en.wikipedia.org/wiki/Cross-correlation
 */
@FloatRange(from = -1.0, to = 1.0)
fun crossCorrelation(x: IntArray, y: IntArray): Double {
    require(x.count() == y.count()) { "Input arrays must be of equal size." }

    val xVar = x.variance()
    val yVar = y.variance()
    val squaredVariance = sqrt(xVar * yVar)

    val xAvg = x.average()
    val yAvg = y.average()
    val count = x.count()

    var sum = 0.0
    for (index in 0 until count) {
        sum += (x[index] - xAvg) * (y[index] - yAvg)
    }
    return sum / count / squaredVariance
}

/**
 * 배열에 있는 요소의 평균 값을 반환합니다.
 */
fun IntArray.variance(): Double {
    if (isEmpty()) return Double.NaN
    val average = average()
    return sumOf { (it - average).pow(2) } / count()
}

/**
 * 주어진 값을 [precision] 소수 자릿수로 가장 가까운 [Double]로 반올림합니다.
 */
fun Double.round(precision: Int): Double {
    val multiplier = 10.0.pow(precision)
    return (this * multiplier).roundToInt() / multiplier
}
