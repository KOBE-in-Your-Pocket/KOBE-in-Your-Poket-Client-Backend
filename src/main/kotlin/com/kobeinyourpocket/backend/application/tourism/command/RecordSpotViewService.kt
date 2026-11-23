package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.tourism.SpotNotFoundException
import com.kobeinyourpocket.backend.domain.tourism.spot.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.view.model.SpotView
import com.kobeinyourpocket.backend.domain.tourism.view.repository.SpotViewRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

/**
 * 観光スポットの閲覧を 1 回数えるユースケース（#240 / PBI #228）。
 *
 * Client が観光スポットのカードから詳細画面に遷移したときに呼ぶ（Client#777）。未ログインでも数える。
 * スポットが存在しなければ 404（[SpotNotFoundException]）。
 *
 * 同じ閲覧者（[viewerKey]）から同じスポットへの閲覧は、一定時間内なら 1 回にまとめる（#260 / #242）。
 * まとめた分もエラーにはせず、数えないだけにする（Client は結果を待たない作りのため）。
 * 記録は行の追加だけなので、同時に呼ばれても数はずれない（#239）。
 */
@Service
class RecordSpotViewService(
    private val spotRepository: SpotRepository,
    private val spotViewRepository: SpotViewRepository,
    private val spotViewDeduplicator: SpotViewDeduplicator,
) {
    /**
     * 閲覧を数える。数えたら true、一定時間内の重複としてまとめたら false。
     *
     * 存在確認を先にするのは、存在しないスポットへの呼び出しで「数えた」扱いを消費しないため。
     */
    fun execute(
        spotId: SpotId,
        viewerKey: String,
    ): Boolean {
        if (!spotRepository.existsById(spotId)) throw SpotNotFoundException(spotId)
        if (!spotViewDeduplicator.tryCount(viewerKey, spotId)) return false
        try {
            spotViewRepository.record(SpotView.create(spotId = spotId))
        } catch (e: DataIntegrityViolationException) {
            // 存在確認と記録の間にスポットが削除された（V26 の fk_spot_views_spot で弾かれる）。
            // 500 のままにせず、事前確認と同じ 404 にそろえる（AddFavoriteService と同じ考え方）。
            throw SpotNotFoundException(spotId).apply { initCause(e) }
        }
        return true
    }
}
