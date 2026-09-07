package com.kobeinyourpocket.backend.application.manner.command

import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem
import com.kobeinyourpocket.backend.domain.manner.manneritem.repository.MannerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * マナー項目の削除ユースケース（write）。
 *
 * ジャンルと違い、参照している側を気にする必要はない。`manner_item_spot` は
 * マナー項目に属する子で、スポット側から張られた参照ではないため（M-2: Spot への
 * 外部キーは無く、Spot は自分に紐づくマナーを知らない）。ローカライズとともに
 * ON DELETE CASCADE で連動削除される。
 *
 * **存在確認は別に行わない。** 確認と削除を分けると、その間に別リクエストが消した場合を
 * 取りこぼし、両方が 204 を返す。1 文で削除し、消せたかどうかで判定する。
 */
@Service
class DeleteMannerItemService(
    private val mannerRepository: MannerRepository,
) {
    @Transactional
    fun deleteMannerItem(id: MannerItem.Id) {
        if (!mannerRepository.deleteById(id)) throw MannerItemNotFoundException(id.value)
    }
}
