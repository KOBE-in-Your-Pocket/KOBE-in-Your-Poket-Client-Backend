package com.kobeinyourpocket.backend.application.tourism

import com.kobeinyourpocket.backend.domain.tourism.aggregate.Spot
import com.kobeinyourpocket.backend.domain.tourism.aggregate.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotLocalizations
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotMedia
import org.springframework.stereotype.Service
import java.util.UUID

/** tourism のユースケース。port [SpotRepository] にのみ依存し、言語解決はここで行う。 */
@Service
class SpotService(
    private val spotRepository: SpotRepository,
) {
    /** 全スポットを要求 [language] へ解決して返す（無ければ ja フォールバック）。 */
    fun listSpots(language: Language): List<LocalizedSpot> =
        spotRepository.findAll().map { withLocalizations ->
            LocalizedSpot(
                spot = withLocalizations.spot,
                localization = withLocalizations.localizations.resolve(language),
            )
        }

    /** スポットを採番して登録し、作成された集約を返す（rating は登録時 null）。 */
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
