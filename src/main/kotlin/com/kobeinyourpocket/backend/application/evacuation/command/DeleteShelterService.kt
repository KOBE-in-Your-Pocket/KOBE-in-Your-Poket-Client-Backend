package com.kobeinyourpocket.backend.application.evacuation.command

import com.kobeinyourpocket.backend.application.evacuation.ShelterNotFoundException
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.repository.ShelterRepository
import org.springframework.stereotype.Service

/**
 * 避難所削除ユースケース（運営ロール限定。REST 側は `@PreAuthorize` で担保 / #144）。
 *
 * 避難所データは神戸市オープンデータの Flyway seed（V9）由来で、個別削除はデータ誤り・
 * 施設閉鎖時の運用手段として使う。
 *
 * 削除済みレコードの tombstone は持たない。`shelter` への書き込みで V10 のトリガーが
 * `shelter_dataset_metadata.updated_at` を進めるため、クライアントは `meta.updatedAt` の
 * 差分を検知して全量を取り直す（要件定義 §4.4 E-2）。
 */
@Service
class DeleteShelterService(
    private val shelterRepository: ShelterRepository,
) {
    fun execute(id: EvacuationShelter.Id) {
        if (!shelterRepository.existsById(id)) throw ShelterNotFoundException(id)
        shelterRepository.deleteById(id)
    }
}
