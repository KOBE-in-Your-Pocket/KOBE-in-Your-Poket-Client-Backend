package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.domain.tourism.spot.model.Spot
import com.kobeinyourpocket.backend.domain.tourism.spot.model.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotMedia
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
