package com.kobeinyourpocket.backend.application.evacuation.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.DisasterType

/** read 専用 port。application が定義し infrastructure.query が実装する。 */
interface ShelterQuery {
    /**
     * 言語解決済みの避難所を返す。
     *
     * [suitableFor] が空なら全件。空でなければ、いずれかの災害種別で
     * [com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterSuitability.SUITABLE]（○）
     * の避難所だけを返す（OR）。△（条件付き）は含めない。
     */
    fun findAllResolved(
        language: Language,
        suitableFor: Set<DisasterType>,
    ): List<ShelterView>
}
