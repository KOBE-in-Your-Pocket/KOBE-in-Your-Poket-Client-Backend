package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId

/** read 専用 port。application が定義し infrastructure.query が実装する。 */
interface ReviewQuery {
    fun findBySpot(
        spotId: SpotId,
        language: Language,
    ): List<ReviewView>

    /**
     * 全スポット横断で新しい順に 1 ページ分取得する（運営向け / #165）。
     *
     * [language] はスポット名の解決にのみ使う（en フォールバック）。レビュー本文・投稿者名は
     * 投稿時の言語のまま返す。[page] は 0 始まりで、境界の正規化は [ListAllReviewsService] の責務。
     */
    fun findPage(
        page: Int,
        size: Int,
        language: Language,
    ): ReviewPageView
}
