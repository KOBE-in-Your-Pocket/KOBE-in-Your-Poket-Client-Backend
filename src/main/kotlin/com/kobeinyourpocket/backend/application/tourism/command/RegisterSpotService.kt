package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.media.MediaStorage
import com.kobeinyourpocket.backend.domain.tourism.spot.model.Spot
import com.kobeinyourpocket.backend.domain.tourism.spot.model.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotMedia
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * ピン登録ユースケース（write）。domain 集約と [SpotRepository] / [MediaStorage] port のみに依存する。
 *
 * 画像は先に [MediaStorage.commit] で確定させてから保存する。確定に失敗したら登録自体を失敗させ、
 * 画像は staging のまま期限切れで消える（＝「保存済みなのに画像が翌日消える」状態を作らない）。
 * 逆に保存が失敗したときは [MediaStorage.release] で staging に戻し、参照されない画像を残さない。
 */
@Service
class RegisterSpotService(
    private val spotRepository: SpotRepository,
    private val mediaStorage: MediaStorage,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

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

        mediaStorage.commit(media.imageUrl)
        return try {
            spotRepository.save(SpotWithLocalizations(spot = spot, localizations = localizations))
        } catch (e: Exception) {
            releaseQuietly(media)
            throw e
        }
    }

    /**
     * 保存失敗時の巻き戻し。差し戻し自体が失敗しても元の例外を握りつぶさないよう、ログのみ残す
     * （残っても画像 1 件で、ストレージ側の突合で回収できる）。
     */
    private fun releaseQuietly(media: SpotMedia) {
        runCatching { mediaStorage.release(media.imageUrl) }
            .onFailure { logger.error("failed to release media after save failure: {}", media.imageUrl, it) }
    }
}
