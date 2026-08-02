package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.media.MediaStorage
import com.kobeinyourpocket.backend.application.tourism.query.SpotNotFoundException
import com.kobeinyourpocket.backend.domain.tourism.spot.model.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotMedia
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * ピン編集ユースケース（write / #152）。domain 集約と [SpotRepository] / [MediaStorage] port に依存する。
 *
 * 更新対象は genre / coordinates / media / 全言語ローカライズ。id と rating は不変（rating は
 * review の平均から read 時に算出されるため引き継ぐ）。該当 id が無ければ [SpotNotFoundException]（404）。
 *
 * 画像の扱いは [RegisterSpotService] と同じく「保存の前に確定、保存が失敗したら差し戻す」。
 * 加えて、**画像が差し替わった場合は旧画像を staging へ戻して清理対象にする**（放置すると
 * 誰からも参照されない画像が残り続ける）。
 */
@Service
class UpdateSpotService(
    private val spotRepository: SpotRepository,
    private val mediaStorage: MediaStorage,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun updateSpot(
        id: SpotId,
        genre: Genre,
        coordinates: Coordinates,
        media: SpotMedia,
        localizations: SpotLocalizations,
    ): SpotWithLocalizations {
        val existing = spotRepository.findSpotById(id) ?: throw SpotNotFoundException(id)
        val previousImageUrl = existing.media.imageUrl
        val imageReplaced = previousImageUrl != media.imageUrl

        val updated =
            existing.copy(
                genre = genre,
                coordinates = coordinates,
                media = media,
            )

        // 確定は保存の前。失敗したら保存せず、新画像は staging のまま期限切れで消える。
        mediaStorage.commit(media.imageUrl)
        val saved =
            try {
                spotRepository.save(SpotWithLocalizations(spot = updated, localizations = localizations))
            } catch (e: Exception) {
                // 差し替えていないなら、その画像は依然このスポットから参照されている。戻すと消えてしまう。
                if (imageReplaced) {
                    releaseQuietly(media.imageUrl, "rollback after save failure")
                }
                throw e
            }

        // 保存できた時点で旧画像は参照されなくなる。ライフサイクル規則に回収させる。
        if (imageReplaced) {
            releaseQuietly(previousImageUrl, "previous image of spot ${id.value}")
        }
        return saved
    }

    /**
     * 差し戻しの失敗でユースケースを壊さない（ロールバック時は元の例外、成功時は更新結果を優先する）。
     * 取りこぼした画像は 1 件で、ストレージ側の突合で回収できる。
     */
    private fun releaseQuietly(
        imageUrl: String,
        reason: String,
    ) {
        runCatching { mediaStorage.release(imageUrl) }
            .onFailure { logger.error("failed to release media ({}): {}", reason, imageUrl, it) }
    }
}
