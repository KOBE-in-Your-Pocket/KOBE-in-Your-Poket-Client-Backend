package com.kobeinyourpocket.backend.tourism.application.command

import com.kobeinyourpocket.backend.tourism.domain.aggregate.Spot
import com.kobeinyourpocket.backend.tourism.domain.aggregate.SpotWithLocalizations
import com.kobeinyourpocket.backend.tourism.domain.repository.SpotRepository
import com.kobeinyourpocket.backend.tourism.domain.vo.Coordinates
import com.kobeinyourpocket.backend.tourism.domain.vo.Genre
import com.kobeinyourpocket.backend.tourism.domain.vo.SpotId
import com.kobeinyourpocket.backend.tourism.domain.vo.SpotLocalizations
import com.kobeinyourpocket.backend.tourism.domain.vo.SpotMedia
import org.springframework.stereotype.Service
import java.util.UUID

/** ピン登録ユースケース（write）。domain 集約と [SpotRepository] port のみに依存する。 */
@Service
class RegisterSpotService(
    private val spotRepository: SpotRepository,
) {
    fun registerSpot(
        genre: Genre,
        coordinates: Coordinates,
        media: SpotMedia,
        localizations: SpotLocalizations,
    ): SpotWithLocalizations {
        val spot =
            Spot(
                id = SpotId.of(UUID.randomUUID().toString()),
                genre = genre,
                coordinates = coordinates,
                media = media,
                rating = null,
            )
        return spotRepository.save(SpotWithLocalizations(spot = spot, localizations = localizations))
    }
}
