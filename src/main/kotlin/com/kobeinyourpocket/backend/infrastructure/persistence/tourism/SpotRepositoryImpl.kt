package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.tourism.spot.model.Spot
import com.kobeinyourpocket.backend.domain.tourism.spot.model.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** [SpotRepository] port の outbound adapter（write のみ）。 */
@Repository
class SpotRepositoryImpl(
    private val spotJpa: SpotJpaRepository,
    private val localizationJpa: SpotLocalizationJpaRepository,
) : SpotRepository {
    @Transactional(readOnly = true)
    override fun findSpotById(id: SpotId): Spot? = spotJpa.findById(id.value).orElse(null)?.toDomainSpot()

    // ロックは呼び出し側のトランザクション終了まで保持されて初めて意味を持つ。ここで新規
    // トランザクションを開くと即コミットで解放されるため、MANDATORY で外側の存在を必須にする。
    @Transactional(propagation = Propagation.MANDATORY)
    override fun findSpotByIdForUpdate(id: SpotId): Spot? = spotJpa.findByIdForUpdate(id.value).orElse(null)?.toDomainSpot()

    @Transactional
    override fun save(spot: SpotWithLocalizations): SpotWithLocalizations {
        spotJpa.save(SpotEntity.fromDomain(spot.spot))
        localizationJpa.saveAll(
            spot.localizations.byLanguage.map { (language, localization) ->
                SpotLocalizationEntity.fromDomain(spot.spot.id, language, localization)
            },
        )
        return spot
    }
}
