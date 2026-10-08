package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.report.command.HandleReviewReportsService
import com.kobeinyourpocket.backend.application.tourism.ReviewNotFoundException
import com.kobeinyourpocket.backend.domain.report.vo.ReportHandlerId
import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * 運営によるレビュー削除ユースケース（#165）。
 *
 * 未登録 ID を黙って成功にしないこと（運営が「消えた」と誤認しない）と、
 * 存在確認に失敗したら削除まで進まないことを押さえる。
 * 削除したレビューへの未対応の通報が、削除した運営の対応として閉じられること（#145）も確かめる。
 */
class DeleteReviewServiceTest {
    private class RecordingReviewRepository(
        private val exists: Boolean,
    ) : ReviewRepository {
        var deletedId: ReviewId? = null

        override fun save(review: Review): Review = review

        override fun findById(id: ReviewId): Review? = null

        override fun existsById(id: ReviewId): Boolean = exists

        override fun deleteById(id: ReviewId) {
            deletedId = id
        }

        /** 退会処理専用（#528）。運営のモデレーション削除では使わない。 */
        override fun deleteByAuthorId(authorId: ReviewAuthorId): Int = 0
    }

    private val id = ReviewId.of(UUID.randomUUID())
    private val operator = ReportHandlerId.of("33333333-3333-3333-3333-333333333333")
    private val reports = mockk<HandleReviewReportsService>()

    init {
        every { reports.approveOnReviewDeleted(any(), any(), any()) } returns 0
    }

    private fun service(repository: ReviewRepository) = DeleteReviewService(repository, reports)

    @Test
    fun `存在するレビューを削除する`() {
        val repository = RecordingReviewRepository(exists = true)

        service(repository).execute(id, operator)

        assertEquals(id, repository.deletedId)
    }

    @Test
    fun `削除したレビューへの未対応の通報を、削除した運営の対応として閉じる`() {
        DeleteReviewService(RecordingReviewRepository(exists = true), reports).execute(id, operator)

        verify(exactly = 1) { reports.approveOnReviewDeleted(id, operator, any()) }
    }

    @Test
    fun `未登録なら ReviewNotFoundException を投げる`() {
        val repository = RecordingReviewRepository(exists = false)

        assertFailsWith<ReviewNotFoundException> { service(repository).execute(id, operator) }
    }

    @Test
    fun `未登録なら削除まで進まない`() {
        val repository = RecordingReviewRepository(exists = false)

        assertFailsWith<ReviewNotFoundException> { service(repository).execute(id, operator) }

        assertNull(repository.deletedId)
        verify(exactly = 0) { reports.approveOnReviewDeleted(any(), any(), any()) }
    }
}
