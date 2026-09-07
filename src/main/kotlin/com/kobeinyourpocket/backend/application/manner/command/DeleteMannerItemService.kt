package com.kobeinyourpocket.backend.application.manner.command

import com.kobeinyourpocket.backend.application.media.MediaStorage
import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem
import com.kobeinyourpocket.backend.domain.manner.manneritem.repository.MannerRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * マナー項目の削除ユースケース（write）。
 *
 * ジャンルと違い、参照している側を気にする必要はない。`manner_item_spot` は
 * マナー項目に属する子で、スポット側から張られた参照ではないため（M-2: Spot への
 * 外部キーは無く、Spot は自分に紐づくマナーを知らない）。ローカライズとともに
 * ON DELETE CASCADE で連動削除される。
 *
 * **アイコン画像**: 削除すると誰からも参照されなくなるため [MediaStorage.release] で
 * staging へ戻し、ライフサイクル規則に回収させる。戻さないと確定済み（タグ無し）のまま
 * ストレージに残り続ける。規則はタグで絞っているため、放置すると永久に回収されない。
 *
 * **存在判定は行ロックで直列化する。** 画像を戻すには削除前に `icon_url` を読む必要があり、
 * 素朴に「確認 → 削除」と分けると、その間に別リクエストが消した場合を取りこぼして両方が
 * 204 を返す。[MannerRepository.findByIdForUpdate] で行ロックを取れば後続はロック解放まで
 * 待たされ、解放後は行が消えているので 404 になる（204 は 1 度だけ）。
 */
@Service
class DeleteMannerItemService(
    private val mannerRepository: MannerRepository,
    private val mediaStorage: MediaStorage,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun deleteMannerItem(id: MannerItem.Id) {
        // 行ロック。以降この id への削除・更新は本トランザクションの決着まで待たされる。
        val target = mannerRepository.findByIdForUpdate(id) ?: throw MannerItemNotFoundException(id.value)

        // ロックを保持している間は false にならないが、削除の結果で存在を判定する形は崩さない。
        if (!mannerRepository.deleteById(id)) throw MannerItemNotFoundException(id.value)

        target.iconUrl?.let { releaseAfterCommit(it.value) }
    }

    /**
     * トランザクションのコミット後に、参照されなくなった画像を staging へ戻す。
     *
     * ロールバックしたときは項目が残る＝画像も現役のままなので戻さない。決着が不明なときも
     * 同じ理由で触らない（更新側の `releaseAfterCompletion` と同じ判断）。
     */
    private fun releaseAfterCommit(imageUrl: String) {
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCompletion(status: Int) {
                    if (status == TransactionSynchronization.STATUS_COMMITTED) {
                        releaseQuietly(imageUrl)
                    }
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
            .onFailure { logger.error("failed to release media after delete: {}", imageUrl, it) }
    }
}
