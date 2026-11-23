package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import org.springframework.stereotype.Service

/**
 * 運営向けのスポット別閲覧数一覧（read / #241）。domain 集約を経由せず [SpotViewCountQuery] port へ委譲する。
 *
 * スポットは数十件の想定のため、件数の上限やページングは付けず全件返す。
 * 増えて重くなった時点で `?limit=` を足す。
 */
@Service
class ListSpotViewCountsService(
    private val spotViewCountQuery: SpotViewCountQuery,
) {
    /** [language] はスポット名の解決にのみ効く。 */
    fun list(language: Language): List<SpotViewCount> = spotViewCountQuery.countAllBySpot(language)
}
