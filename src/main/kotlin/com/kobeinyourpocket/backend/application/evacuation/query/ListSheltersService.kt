package com.kobeinyourpocket.backend.application.evacuation.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.DisasterType
import org.springframework.stereotype.Service

/** 避難所一覧取得ユースケース（read）。domain 集約を経由せず [ShelterQuery] port へ委譲する。 */
@Service
class ListSheltersService(
    private val shelterQuery: ShelterQuery,
) {
    /** [suitableFor] の意味は [ShelterQuery.findAllResolved] を参照。空なら全件。 */
    fun listShelters(
        language: Language,
        suitableFor: Set<DisasterType> = emptySet(),
    ): List<ShelterView> = shelterQuery.findAllResolved(language, suitableFor)
}
