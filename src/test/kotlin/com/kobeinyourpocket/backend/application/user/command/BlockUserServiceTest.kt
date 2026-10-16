package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.model.UserBlock
import com.kobeinyourpocket.backend.domain.user.repository.UserBlockRepository
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BlockUserServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val userBlockRepository = mockk<UserBlockRepository>()
    private val service = BlockUserService(userRepository, userBlockRepository)

    private val alice = User.Id.of("11111111-1111-1111-1111-111111111111")
    private val bob = User.Id.of("22222222-2222-2222-2222-222222222222")

    @Test
    fun `存在するユーザーをブロックすると保存する`() {
        every { userRepository.findById(bob) } returns User.create(id = bob, name = "Bob")
        val saved = slot<UserBlock>()
        justRun { userBlockRepository.saveIfAbsent(capture(saved)) }

        service.execute(blockerId = alice, blockedId = bob)

        assertEquals(alice, saved.captured.blockerId)
        assertEquals(bob, saved.captured.blockedId)
    }

    @Test
    fun `存在しないユーザーをブロックすると UserNotFoundException で保存しない`() {
        every { userRepository.findById(bob) } returns null

        assertFailsWith<UserNotFoundException> {
            service.execute(blockerId = alice, blockedId = bob)
        }

        verify(exactly = 0) { userBlockRepository.saveIfAbsent(any()) }
    }

    @Test
    fun `自分自身をブロックすると IllegalArgumentException で相手の確認も保存もしない`() {
        assertFailsWith<IllegalArgumentException> {
            service.execute(blockerId = alice, blockedId = alice)
        }

        verify(exactly = 0) { userRepository.findById(any()) }
        verify(exactly = 0) { userBlockRepository.saveIfAbsent(any()) }
    }
}
