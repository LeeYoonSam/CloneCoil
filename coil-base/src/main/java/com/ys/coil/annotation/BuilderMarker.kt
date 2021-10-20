package com.ys.coil.annotation

/**
 * DSL의 일부인 빌더 클래스를 표시합니다.
 * 이는 [DslMarker]로도 표시되는 경우 외부 범위 호출을 제한합니다.
 */
@DslMarker
@Retention(AnnotationRetention.SOURCE)
annotation class BuilderMarker
