package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.tourism.query.SpotNotFoundException
import com.kobeinyourpocket.backend.domain.tourism.spot.model.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotMedia
import org.springframework.stereotype.Service

/**
 * ピン編集ユースケース（write / #152）。domain 集約と [SpotRepository] port のみに依存する。
 *
 * 更新対象は genre / coordinates / media / 全言語ローカライズ。id と rating は不変（rating は
 * review の平均から read 時に算出されるため引き継ぐ）。該当 id が無ければ [SpotNotFoundException]（404）。
 */
@Service
class UpdateSpotService(
    private val spotRepository: SpotRepository,
) {
    fun updateSpot(
        id: SpotId,
        genre: Genre,
        coordinates: Coordinates,
        media: SpotMedia,
        localizations: SpotLocalizations,
    ): SpotWithLocalizations {
        val existing = spotRepository.findSpotById(id) ?: throw SpotNotFoundException(id)
        val updated =
            existing.copy(
                genre = genre,
                coordinates = coordinates,
                media = media,
            )
        return spotRepository.save(SpotWithLocalizations(spot = updated, localizations = localizations))
    }
}
