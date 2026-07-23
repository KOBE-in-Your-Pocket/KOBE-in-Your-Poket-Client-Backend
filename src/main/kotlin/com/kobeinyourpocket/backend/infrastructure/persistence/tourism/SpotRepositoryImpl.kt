package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.tourism.spot.model.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/** [SpotRepository] port の outbound adapter（write のみ）。 */
@Repository
class SpotRepositoryImpl(
    private val spotJpa: SpotJpaRepository,
    private val localizationJpa: SpotLocalizationJpaRepository,
) : SpotRepository {
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

    override fun existsById(id: SpotId): Boolean = spotJpa.existsById(id.value)

    /** spot_localization・review は `ON DELETE CASCADE`（V1 / V2）で DB 側が連動削除する。 */
    @Transactional
    override fun deleteById(id: SpotId) {
        spotJpa.deleteById(id.value)
    }
}
