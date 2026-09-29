package com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo

/**
 * 避難所の適否を判断する災害の種別（値オブジェクト）。
 *
 * 神戸市オープンデータ「指定緊急避難場所・指定避難所」が列として持つ 4 種に対応する（#180）。
 * 元データに無い種別（高潮・内水氾濫など）は増やさない。持っていない情報を種別として並べると、
 * 「対応していない」と「データが無い」の区別が付かなくなる。
 *
 * [wireValue] は Client の契約に合わせた slug。
 */
enum class DisasterType(
    val wireValue: String,
    /** オープンデータ上の列名。シード生成スクリプトと対応を揃える。 */
    val sourceColumn: String,
) {
    /** 土砂災害。 */
    LANDSLIDE("landslide", "土砂災害"),

    /** 洪水。 */
    FLOOD("flood", "洪水"),

    /** 津波。 */
    TSUNAMI("tsunami", "津波"),

    /** 大火事（屋外の緊急避難場所が対象）。 */
    LARGE_FIRE("large-fire", "大火事"),
    ;

    companion object {
        /** wireValue から解決する。未対応・空は `null`。 */
        fun of(value: String): DisasterType? {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.wireValue == normalized }
        }
    }
}
