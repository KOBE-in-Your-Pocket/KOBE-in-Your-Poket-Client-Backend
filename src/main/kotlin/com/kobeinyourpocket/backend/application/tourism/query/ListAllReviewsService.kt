package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import org.springframework.stereotype.Service

/**
 * 運営向けレビュー横断一覧ユースケース（read / #165）。domain 集約を経由せず [ReviewQuery] port へ委譲する。
 *
 * ページ境界の正規化をここに置く。REST 層の既定値と application の上限が二重管理になると、
 * 別の入口から上限を超えた size が素通りするため（[com.kobeinyourpocket.backend.application.user.query.ListUsersService] と同方針）。
 */
@Service
class ListAllReviewsService(
    private val reviewQuery: ReviewQuery,
) {
    /**
     * [page]（0 始まり）/ [size] は未指定可。負値は下限へ、上限超過は [MAX_SIZE] へ丸める。
     *
     * 不正値を 400 で弾かず丸めているのは、一覧取得が破壊的でなく、管理画面のページャ操作を
     * 落とすより安全側の結果を返す方が実用的なため。
     */
    fun listReviews(
        language: Language,
        page: Int? = null,
        size: Int? = null,
    ): ReviewPageView =
        reviewQuery.findPage(
            page = (page ?: 0).coerceAtLeast(0),
            size = (size ?: DEFAULT_SIZE).coerceIn(1, MAX_SIZE),
            language = language,
        )

    companion object {
        /** 管理画面の 1 画面分として十分な件数。 */
        const val DEFAULT_SIZE = 50

        /** 1 リクエストで返す上限。全件取得による重いレスポンスを防ぐ。 */
        const val MAX_SIZE = 200
    }
}
