package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.tourism.ReviewNotFoundException
import com.kobeinyourpocket.backend.domain.tourism.review.model.Review
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewId
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
    }

    private val id = ReviewId.of(UUID.randomUUID())

    @Test
    fun `存在するレビューを削除する`() {
        val repository = RecordingReviewRepository(exists = true)

        DeleteReviewService(repository).execute(id)

        assertEquals(id, repository.deletedId)
    }

    @Test
    fun `未登録なら ReviewNotFoundException を投げる`() {
        val repository = RecordingReviewRepository(exists = false)

        assertFailsWith<ReviewNotFoundException> { DeleteReviewService(repository).execute(id) }
    }

    @Test
    fun `未登録なら削除まで進まない`() {
        val repository = RecordingReviewRepository(exists = false)

        assertFailsWith<ReviewNotFoundException> { DeleteReviewService(repository).execute(id) }

        assertNull(repository.deletedId)
    }
}
