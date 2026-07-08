package com.kobeinyourpocket.backend.infrastructure.persistence.manner.impl

import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem
import com.kobeinyourpocket.backend.domain.manner.manneritem.repository.MannerRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemLocalizationEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity.MannerItemSpotEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.repository.MannerItemJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.repository.MannerItemLocalizationJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.repository.MannerItemSpotJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * [MannerRepository] port の outbound adapter（write のみ）。
 *
 * localizations / relatedSpotIds は集約の一部（membership が減ることもある）。
 * 単なる upsert では削除済みの子行が残るため、子テーブルは delete-then-insert で
 * 集約の現在状態にそろえる（同一トランザクション内）。
 */
@Repository
class MannerRepositoryImpl(
    private val itemJpa: MannerItemJpaRepository,
    private val localizationJpa: MannerItemLocalizationJpaRepository,
    private val spotJpa: MannerItemSpotJpaRepository,
) : MannerRepository {
    @Transactional
    override fun save(item: MannerItem): MannerItem {
        itemJpa.save(MannerItemEntity.fromDomain(item))

        localizationJpa.deleteByIdMannerItemId(item.id.value)
        localizationJpa.saveAll(
            item.localizations.byLanguage.map { (language, localization) ->
                MannerItemLocalizationEntity.fromDomain(item.id, language, localization)
            },
        )

        spotJpa.deleteByIdMannerItemId(item.id.value)
        spotJpa.saveAll(
            item.relatedSpotIds.map { relatedSpotId ->
                MannerItemSpotEntity.fromDomain(item.id, relatedSpotId)
            },
        )

        return item
    }
}
