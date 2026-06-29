package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.tourism.aggregate.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.repository.SpotRepository
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
}
