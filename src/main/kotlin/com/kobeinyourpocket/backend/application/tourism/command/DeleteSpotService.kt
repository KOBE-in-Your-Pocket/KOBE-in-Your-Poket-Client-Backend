package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.tourism.query.SpotNotFoundException
import com.kobeinyourpocket.backend.domain.tourism.spot.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import org.springframework.stereotype.Service

/**
 * スポット削除ユースケース（admin ロール専用。REST 側は `@PreAuthorize` で担保）。
 *
 * spot_localization・review は `ON DELETE CASCADE`（V1 / V2）で DB 側が連動削除する。
 * manner_item_spot は Spot への ID 参照のみで FK を張らない設計（M-2）のため、
 * 削除後も関連づけの行自体は残り得るが実在検証はしない（既存方針を踏襲）。
 */
@Service
class DeleteSpotService(
    private val spotRepository: SpotRepository,
) {
    fun execute(id: SpotId) {
        if (!spotRepository.existsById(id)) throw SpotNotFoundException(id)
        spotRepository.deleteById(id)
    }
}
