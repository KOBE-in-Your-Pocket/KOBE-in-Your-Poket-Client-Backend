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

    /**
     * 集約を復元する。ベース行・ローカライズ・関連スポットを引いて 1 件に組み直す。
     *
     * 1 件ぶんなので子テーブルはそれぞれ 1 クエリで足りる（一覧は read 側の query が担う）。
     */
    @Transactional(readOnly = true)
    override fun findById(id: MannerItem.Id): MannerItem? = toDomain(id, itemJpa.findById(id.value).orElse(null))

    @Transactional
    override fun findByIdForUpdate(id: MannerItem.Id): MannerItem? = toDomain(id, itemJpa.findByIdForUpdate(id.value).orElse(null))

    private fun toDomain(
        id: MannerItem.Id,
        entity: MannerItemEntity?,
    ): MannerItem? =
        entity?.toDomain(
            localizations = localizationJpa.findByIdMannerItemId(id.value),
            relatedSpots = spotJpa.findByIdMannerItemId(id.value),
        )

    override fun existsById(id: MannerItem.Id): Boolean = itemJpa.existsById(id.value)

    /**
     * ベース行だけ消す。localization / spot は V5 の ON DELETE CASCADE で連動削除される。
     *
     * 子を明示的に消さないのは、削除経路を DB の制約と二重に持つと片方の変更が
     * もう片方に伝わらないため。制約は V5 で張られている。
     */
    @Transactional
    override fun deleteById(id: MannerItem.Id) {
        itemJpa.deleteById(id.value)
    }
}
