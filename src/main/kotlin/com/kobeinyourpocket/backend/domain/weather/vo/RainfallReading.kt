package com.kobeinyourpocket.backend.domain.weather.vo

import java.time.Instant

/**
 * [値オブジェクト] ある時刻の降水強度（#230）。
 *
 * [mmPerHour] は気象レーダーの降水の強さを時間雨量（mm/h）に換算した値で、実際の雨量とは異なる。
 *
 * 不変条件:
 * - [mmPerHour] は 0 以上
 */
data class RainfallReading(
    val type: RainfallReadingType,
    val at: Instant,
    val mmPerHour: Double,
) {
    init {
        require(mmPerHour >= 0.0) { "mmPerHour must not be negative, got $mmPerHour" }
    }
}
