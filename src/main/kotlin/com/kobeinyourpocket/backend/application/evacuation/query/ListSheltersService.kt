package com.kobeinyourpocket.backend.application.evacuation.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import org.springframework.stereotype.Service

/** 避難所一覧取得ユースケース（read）。domain 集約を経由せず [ShelterQuery] port へ委譲する。 */
@Service
class ListSheltersService(
    private val shelterQuery: ShelterQuery,
) {
    fun listShelters(language: Language): List<ShelterView> = shelterQuery.findAllResolved(language)
}
