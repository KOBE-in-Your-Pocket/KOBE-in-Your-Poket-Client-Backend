package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 運営向けレビュー一覧のページ境界（#165）。
 *
 * 管理画面のページャが不正値を送っても落ちないこと、全件取得で重いレスポンスにならないことを押さえる。
 */
class ListAllReviewsServiceTest {
    /** 正規化後の引数を記録するだけの [ReviewQuery]。 */
    private class RecordingReviewQuery : ReviewQuery {
        var requestedPage: Int? = null
        var requestedSize: Int? = null
        var requestedLanguage: Language? = null

        override fun findBySpot(
            spotId: SpotId,
            language: Language,
        ): List<ReviewView> = emptyList()

        override fun findPage(
            page: Int,
            size: Int,
            language: Language,
        ): ReviewPageView {
            requestedPage = page
            requestedSize = size
            requestedLanguage = language
            return ReviewPageView(reviews = emptyList(), page = page, size = size, totalElements = 0)
        }
    }

    private val reviewQuery = RecordingReviewQuery()
    private val service = ListAllReviewsService(reviewQuery)

    @Test
    fun `未指定なら先頭ページと既定件数を使う`() {
        service.listReviews(Language.JA)

        assertEquals(0, reviewQuery.requestedPage)
        assertEquals(ListAllReviewsService.DEFAULT_SIZE, reviewQuery.requestedSize)
    }

    @Test
    fun `負の page は先頭ページに丸める`() {
        service.listReviews(Language.JA, page = -1)

        assertEquals(0, reviewQuery.requestedPage)
    }

    @Test
    fun `0 以下の size は 1 件に丸める`() {
        service.listReviews(Language.JA, size = 0)

        assertEquals(1, reviewQuery.requestedSize)
    }

    @Test
    fun `上限を超える size は MAX_SIZE に丸める`() {
        service.listReviews(Language.JA, size = ListAllReviewsService.MAX_SIZE + 1)

        assertEquals(ListAllReviewsService.MAX_SIZE, reviewQuery.requestedSize)
    }

    @Test
    fun `言語はそのまま port へ渡す`() {
        service.listReviews(Language.KO)

        assertEquals(Language.KO, reviewQuery.requestedLanguage)
    }

    @Test
    fun `totalPages は端数を切り上げる`() {
        assertEquals(3, ReviewPageView(reviews = emptyList(), page = 0, size = 10, totalElements = 21).totalPages)
    }

    @Test
    fun `0 件なら totalPages は 0`() {
        assertEquals(0, ReviewPageView(reviews = emptyList(), page = 0, size = 10, totalElements = 0).totalPages)
    }
}
