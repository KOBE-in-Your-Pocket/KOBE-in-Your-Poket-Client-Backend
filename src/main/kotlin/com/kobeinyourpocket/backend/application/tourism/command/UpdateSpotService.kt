package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.media.MediaStorage
import com.kobeinyourpocket.backend.application.tourism.SpotNotFoundException
import com.kobeinyourpocket.backend.domain.tourism.spot.model.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotMedia
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * ピン編集ユースケース（write / #152）。domain 集約と [SpotRepository] / [MediaStorage] port に依存する。
 *
 * 更新対象は genre / coordinates / media / 全言語ローカライズ。id と rating は不変（rating は
 * review の平均から read 時に算出されるため引き継ぐ）。該当 id が無ければ [SpotNotFoundException]（404）。
 *
 * **同時更新**: 読み取りと保存を 1 トランザクションにまとめ、[SpotRepository.findSpotByIdForUpdate]
 * の行ロックで同じスポットへの同時更新を直列化する。ロックが無いと後勝ちで先行変更が消えるうえ、
 * 古い読み取りを基準に「差し替えられた」と誤判定して**まだ参照されている画像を削除**しうる。
 *
 * **画像**: 保存の前に新画像を確定し（[RegisterSpotService] と同じ順序）、差し替えた場合のみ
 * トランザクションの決着後に片方を清理対象へ戻す。コミットされたら旧画像、ロールバックしたら新画像。
 * 差し戻しをコミット前に行うと、その後ロールバックしたときに現役の画像を消してしまう。
 */
@Service
class UpdateSpotService(
    private val spotRepository: SpotRepository,
    private val mediaStorage: MediaStorage,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun updateSpot(
        id: SpotId,
        genre: Genre,
        coordinates: Coordinates,
        media: SpotMedia,
        localizations: SpotLocalizations,
    ): SpotWithLocalizations {
        // 行ロック。以降この id への更新は本トランザクションの決着まで待たされる。
        val existing = spotRepository.findSpotByIdForUpdate(id) ?: throw SpotNotFoundException(id)
        val previousImageUrl = existing.media.imageUrl
        val imageReplaced = previousImageUrl != media.imageUrl

        val updated =
            existing.copy(
                genre = genre,
                coordinates = coordinates,
                media = media,
            )

        // 確定は保存の前。失敗すればロールバックし、新画像は staging のまま期限切れで消える。
        mediaStorage.commit(media.imageUrl)
        if (imageReplaced) {
            // save より前に登録する（save が投げるとロールバック用のフックを張れなくなるため）。
            releaseAfterCompletion(onCommit = previousImageUrl, onRollback = media.imageUrl)
        }
        return spotRepository.save(SpotWithLocalizations(spot = updated, localizations = localizations))
    }

    /**
     * トランザクションの決着後に、不要になった方の画像を staging へ戻す。
     *
     * @param onCommit コミットされた場合に戻す URL（＝もう参照されない旧画像）
     * @param onRollback ロールバックされた場合に戻す URL（＝保存されなかった新画像）
     */
    private fun releaseAfterCompletion(
        onCommit: String,
        onRollback: String,
    ) {
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCompletion(status: Int) {
                    val target =
                        when (status) {
                            TransactionSynchronization.STATUS_COMMITTED -> onCommit
                            TransactionSynchronization.STATUS_ROLLED_BACK -> onRollback
                            // 決着不明。どちらを戻しても現役を消す恐れがあるため何もしない。
                            else -> return
                        }
                    releaseQuietly(target)
                }
            },
        )
    }

    /**
     * 差し戻しの失敗でユースケースを壊さない（この時点でトランザクションは決着済み）。
     * 取りこぼしても画像 1 件で、ストレージ側の突合で回収できる。
     */
    private fun releaseQuietly(imageUrl: String) {
        runCatching { mediaStorage.release(imageUrl) }
            .onFailure { logger.error("failed to release media: {}", imageUrl, it) }
    }
}
