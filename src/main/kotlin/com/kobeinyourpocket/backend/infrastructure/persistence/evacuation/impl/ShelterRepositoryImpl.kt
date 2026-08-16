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

    override fun existsById(id: EvacuationShelter.Id): Boolean = shelterJpa.existsById(id.value)

    /**
     * 子テーブルを明示的に消してから集約ルートを消す（[save] と対称）。
     *
     * DB 側にも `ON DELETE CASCADE` はある（V7）が、それは Flyway が張る制約である。
     * `ShelterLocalizationEntity` は集約ルートへの JPA 関連を持たない（`@EmbeddedId` の ID 参照のみ）ため、
     * Hibernate の DDL 生成では FK 自体が作られない。DB のカスケードだけに頼ると、
     * Flyway を使わない環境（テストの H2 / `ddl-auto=create-drop`）で孤児行が残り、
     * 本番とテストで挙動が割れる。明示削除で両環境の結果をそろえる。
     */
    @Transactional
    override fun deleteById(id: EvacuationShelter.Id) {
        localizationJpa.deleteByIdShelterId(id.value)
        shelterJpa.deleteById(id.value)
    }
}
