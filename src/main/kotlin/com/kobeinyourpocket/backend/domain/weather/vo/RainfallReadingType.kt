package com.kobeinyourpocket.backend.domain.weather.vo

/** [値オブジェクト] 降水強度の値が観測値か予測値か（#230）。 */
enum class RainfallReadingType {
    /** 気象レーダーで観測した、今の降水強度。 */
    OBSERVATION,

    /** 60 分先までの予測値。 */
    FORECAST,
}
