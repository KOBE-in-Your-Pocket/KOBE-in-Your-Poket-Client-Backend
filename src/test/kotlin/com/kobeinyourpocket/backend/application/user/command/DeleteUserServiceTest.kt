package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.application.user.auth.AuthGateway
import com.kobeinyourpocket.backend.application.user.auth.AuthGatewayException
import com.kobeinyourpocket.backend.domain.report.repository.ReportRepository
import com.kobeinyourpocket.backend.domain.report.vo.ReporterId
import com.kobeinyourpocket.backend.domain.tourism.review.repository.ReviewRepository
import com.kobeinyourpocket.backend.domain.tourism.review.vo.ReviewAuthorId
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DeleteUserServiceTest {
    private val authGateway = mockk<AuthGateway>()
    private val userRepository = mockk<UserRepository>()
    private val reviewRepository = mockk<ReviewRepository>()
    private val reportRepository = mockk<ReportRepository>()
    private val service = DeleteUserService(authGateway, userRepository, reviewRepository, reportRepository)

    private val userId = User.Id.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
    private val authorId = ReviewAuthorId.of(userId.value)
    private val reporterId = ReporterId.of(userId.value)
    private val existingUser = User.create(id = userId, name = "Alice")

    @Test
    fun `存在するユーザーを削除すると Auth の後に本人のレビュー・通報と DB プロフィールを削除する`() {
        every { userRepository.findById(userId) } returns existingUser
        justRun { authGateway.deleteUser(userId) }
        every { reviewRepository.deleteByAuthorId(authorId) } returns 2
        every { reportRepository.deleteByReporterId(reporterId) } returns 1
        justRun { userRepository.deleteById(userId) }

        service.execute(userId)

        // Auth HTTP はトランザクション外。ロールバックで Auth だけ消える事態を避ける。
        // レビュー・通報の削除を Auth の後に置くのは、退会が成立しないときにそれらだけ消えるのを防ぐため。
        verifyOrder {
            userRepository.findById(userId)
            authGateway.deleteUser(userId)
            reviewRepository.deleteByAuthorId(authorId)
            reportRepository.deleteByReporterId(reporterId)
            userRepository.deleteById(userId)
        }
    }

    @Test
    fun `レビューも通報も 1 件も無くても削除は成功する`() {
        every { userRepository.findById(userId) } returns existingUser
        justRun { authGateway.deleteUser(userId) }
        every { reviewRepository.deleteByAuthorId(authorId) } returns 0
        every { reportRepository.deleteByReporterId(reporterId) } returns 0
        justRun { userRepository.deleteById(userId) }

        service.execute(userId)

        verify(exactly = 1) { userRepository.deleteById(userId) }
    }

    @Test
    fun `存在しないユーザー ID を指定すると UserNotFoundException`() {
        every { userRepository.findById(userId) } returns null

        assertFailsWith<UserNotFoundException> {
            service.execute(userId)
        }

        verify(exactly = 0) { authGateway.deleteUser(any()) }
        verify(exactly = 0) { reviewRepository.deleteByAuthorId(any()) }
        verify(exactly = 0) { reportRepository.deleteByReporterId(any()) }
        verify(exactly = 0) { userRepository.deleteById(any()) }
    }

    @Test
    fun `Supabase Admin API が失敗したら AuthGatewayException が伝播しレビューも DB も削除されない`() {
        every { userRepository.findById(userId) } returns existingUser
        every { authGateway.deleteUser(userId) } throws
            AuthGatewayException(status = 500, message = "Supabase internal error")

        assertFailsWith<AuthGatewayException> {
            service.execute(userId)
        }

        // 退会が成立していないのにレビュー・通報だけ消える状態を作らない。
        verify(exactly = 0) { reviewRepository.deleteByAuthorId(any()) }
        verify(exactly = 0) { reportRepository.deleteByReporterId(any()) }
        verify(exactly = 0) { userRepository.deleteById(any()) }
    }
}
