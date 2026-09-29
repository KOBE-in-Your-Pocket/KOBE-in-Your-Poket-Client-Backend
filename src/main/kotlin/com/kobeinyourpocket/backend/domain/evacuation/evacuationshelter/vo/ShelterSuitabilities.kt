package com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo

/**
 * 災害種別ごとの適否の集合（値オブジェクト）。
 *
 * **全種別について値を持つ**ことを不変条件にしている。欠けた種別を「不明」として扱うと、
 * 表示側で毎回「キーが無い場合」の分岐が要り、そこを書き忘れると危険側に倒れる。
 * 元データが空の種別は [ShelterSuitability.NOT_APPLICABLE] として明示的に埋める。
 */
data class ShelterSuitabilities(
    val byDisasterType: Map<DisasterType, ShelterSuitability>,
) {
    init {
        val missing = DisasterType.entries.filterNot(byDisasterType::containsKey)
        require(missing.isEmpty()) {
            "ShelterSuitabilities must cover all disaster types, missing: ${missing.map(DisasterType::wireValue)}"
        }
    }

    /** 指定災害での適否。全種別を持つため必ず非 null を返す。 */
    fun of(disasterType: DisasterType): ShelterSuitability = byDisasterType.getValue(disasterType)

    companion object {
        /** 防御的コピーを取り生成する。 */
        fun of(byDisasterType: Map<DisasterType, ShelterSuitability>): ShelterSuitabilities = ShelterSuitabilities(byDisasterType.toMap())

        /** 種別ごとの値を並べて生成する（永続化からの復元で使う）。 */
        fun of(
            landslide: ShelterSuitability,
            flood: ShelterSuitability,
            tsunami: ShelterSuitability,
            largeFire: ShelterSuitability,
        ): ShelterSuitabilities =
            ShelterSuitabilities(
                mapOf(
                    DisasterType.LANDSLIDE to landslide,
                    DisasterType.FLOOD to flood,
                    DisasterType.TSUNAMI to tsunami,
                    DisasterType.LARGE_FIRE to largeFire,
                ),
            )
    }
}
