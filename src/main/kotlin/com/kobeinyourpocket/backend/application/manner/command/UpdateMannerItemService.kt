package com.kobeinyourpocket.backend.application.manner.command

import com.kobeinyourpocket.backend.application.media.MediaStorage
import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem
import com.kobeinyourpocket.backend.domain.manner.manneritem.repository.MannerRepository
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIcon
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIconUrl
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerKind
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalizations
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerScope
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.RelatedSpotId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * マナー項目の更新ユースケース（write）。全置換であって部分更新ではない。
 *
 * **ID は変更しない。** Client の項目詳細（`/manner/[id]`）の遷移先であり、変えると既存の
 * リンクが切れる。英語タイトルを変えても ID は追従しない。名前を大きく変えたい場合は
 * 新しく作って旧項目を削除する運用になる。
 *
 * **同時更新**: 読み取りと保存を 1 トランザクションにまとめ、[MannerRepository.findByIdForUpdate]
 * の行ロックで同じ項目への同時更新を直列化する（スポット更新と同じ）。
 *
 * **アイコン画像**: 保存の前に新画像を確定し、差し替えた場合のみトランザクションの決着後に
 * 片方を清理対象へ戻す。コミットされたら旧画像、ロールバックしたら新画像。差し戻しを
 * コミット前に行うと、その後ロールバックしたときに現役の画像を消してしまう。
 */
@Service
class UpdateMannerItemService(
    private val mannerRepository: MannerRepository,
    private val mediaStorage: MediaStorage,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun updateMannerItem(
        id: MannerItem.Id,
        icon: MannerIcon?,
        iconUrl: MannerIconUrl?,
        kind: MannerKind,
        scope: MannerScope,
        relatedSpotIds: List<RelatedSpotId>,
        localizations: MannerLocalizations,
    ): MannerItem {
        requireAllLanguages(localizations)

        // 行ロック。以降この id への更新は本トランザクションの決着まで待たされる。
        val current = mannerRepository.findByIdForUpdate(id) ?: throw MannerItemNotFoundException(id.value)
        val previousIconUrl = current.iconUrl?.value
        val nextIconUrl = iconUrl?.value

        // 確定は保存の前。失敗すればロールバックし、新画像は staging のまま期限切れで消える。
        nextIconUrl?.let { mediaStorage.commit(it) }
        if (previousIconUrl != null && previousIconUrl != nextIconUrl) {
            // save より前に登録する（save が投げるとロールバック用のフックを張れなくなるため）。
            releaseAfterCompletion(onCommit = previousIconUrl, onRollback = nextIconUrl)
        }

        return mannerRepository.save(
            current.update(
                icon = icon,
                iconUrl = iconUrl,
                kind = kind,
                scope = scope,
                relatedSpotIds = relatedSpotIds,
                localizations = localizations,
            ),
        )
    }

    /**
     * トランザクションの決着後に、不要になった方の画像を staging へ戻す。
     *
     * @param onCommit コミットされた場合に戻す URL（＝もう参照されない旧画像）
     * @param onRollback ロールバックされた場合に戻す URL（＝保存されなかった新画像）。
     *   画像を外しただけの更新では新画像が無いため null になり、その場合は何もしない
     */
    private fun releaseAfterCompletion(
        onCommit: String,
        onRollback: String?,
    ) {
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCompletion(status: Int) {
                    val target =
                        when (status) {
                            TransactionSynchronization.STATUS_COMMITTED -> onCommit
                            TransactionSynchronization.STATUS_ROLLED_BACK -> onRollback
                            // 決着不明。どちらを戻しても現役を消す恐れがあるため何もしない。
                            else -> null
                        }
                    target?.let { releaseQuietly(it) }
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
