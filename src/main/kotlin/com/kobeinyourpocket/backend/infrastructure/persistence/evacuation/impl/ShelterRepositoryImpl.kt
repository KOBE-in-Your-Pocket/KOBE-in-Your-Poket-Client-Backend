package com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.impl

import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.repository.ShelterRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.entity.ShelterEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.entity.ShelterLocalizationEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.repository.ShelterJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.repository.ShelterLocalizationJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * [ShelterRepository] port の outbound adapter（write のみ）。
 *
 * localizations は集約の一部（membership が減ることもある）。単なる upsert では
 * 削除済みの子行が残るため、子テーブルは delete-then-insert で集約の現在状態に
 * そろえる（同一トランザクション内）。
 */
@Repository
class ShelterRepositoryImpl(
    private val shelterJpa: ShelterJpaRepository,
    private val localizationJpa: ShelterLocalizationJpaRepository,
) : ShelterRepository {
    @Transactional
    override fun save(shelter: EvacuationShelter): EvacuationShelter {
        shelterJpa.save(ShelterEntity.fromDomain(shelter))

        localizationJpa.deleteByIdShelterId(shelter.id.value)
        localizationJpa.saveAll(
            shelter.localizations.byLanguage.map { (language, localization) ->
                ShelterLocalizationEntity.fromDomain(shelter.id, language, localization)
            },
        )

        return shelter
    }
}
