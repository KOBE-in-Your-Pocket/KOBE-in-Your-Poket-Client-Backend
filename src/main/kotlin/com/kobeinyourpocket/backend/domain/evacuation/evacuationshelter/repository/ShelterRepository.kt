package com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.repository

import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter

/**
 * EvacuationShelter 集約の書き込み用リポジトリ port（command）。infrastructure.persistence.evacuation が実装する。
 *
 * read（`GET /evacuation/shelters?lang=`）は CQRS-lite に従い application.evacuation.query 側の専用 port で扱う。
 */
interface ShelterRepository {
    fun save(shelter: EvacuationShelter): EvacuationShelter
}
