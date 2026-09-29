package com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo

/**
 * 屋内か屋外か（値オブジェクト）。
 *
 * 神戸市オープンデータの「分類」に対応する（#180）。屋外の緊急避難場所は大火事のときに
 * 使う公園・広場で、屋内の施設とは使い方が違う。従来の施設種別（学校 / 公園 / 体育館…）は
 * 元データが持っていないため、この 2 値に置き換えている。
 */
enum class ShelterSiting(
    val wireValue: String,
) {
    /** 屋内の緊急避難場所。 */
    INDOOR("indoor"),

    /** 屋外の緊急避難場所。 */
    OUTDOOR("outdoor"),
    ;

    companion object {
        /** wireValue から解決する。未対応・空は `null`。 */
        fun of(value: String): ShelterSiting? {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.wireValue == normalized }
        }
    }
}
