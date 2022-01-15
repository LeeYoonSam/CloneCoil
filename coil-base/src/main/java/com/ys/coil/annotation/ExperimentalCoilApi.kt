package com.ys.coil.annotation

/**
 * 아직 **실험적**인 선언을 표시합니다.
 *
 * 이 주석으로 표시된 대상은 설계가 아직 진행 중이므로 향후 주요 변경 사항이 포함될 수 있습니다.
 */
@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class ExperimentalCoilApi