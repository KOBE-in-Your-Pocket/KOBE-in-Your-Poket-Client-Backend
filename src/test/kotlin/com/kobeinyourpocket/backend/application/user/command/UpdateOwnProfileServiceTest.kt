package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.application.user.command.UpdateOwnProfileService.IconUpdate
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import com.kobeinyourpocket.backend.domain.user.vo.UserIcon
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * 本人によるプロフィール更新（#179）。
 *
 * 表示名とアイコンはそれぞれ独立に「変更しない」を選べる。とくにアイコンは
 * 「変更しない」と「未設定に戻す」を取り違えると、利用者のアイコンが勝手に消える。
 */
class UpdateOwnProfileServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val service = UpdateOwnProfileService(userRepository)

    private val userId = User.Id.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")

    private fun existingUser(icon: UserIcon? = UserIcon.of("https://example.com/old.png")): User =
        User.create(id = userId, name = "Alice", icon = icon, createdAt = createdAt)

    private fun stubSave(): io.mockk.CapturingSlot<User> {
        val saved = slot<User>()
        every { userRepository.save(capture(saved)) } answers { saved.captured }
        return saved
    }

    @Test
    fun `表示名だけを更新するとアイコンは維持される`() {
        every { userRepository.findById(userId) } returns existingUser()
        val saved = stubSave()

        service.execute(userId, name = "Alice Updated")

        assertEquals("Alice Updated", saved.captured.name)
        assertEquals("https://example.com/old.png", saved.captured.icon?.url)
    }

    @Test
    fun `アイコンだけを差し替えると表示名は維持される`() {
        every { userRepository.findById(userId) } returns existingUser()
        val saved = stubSave()

        service.execute(userId, icon = IconUpdate.Set("https://example.com/new.png"))

        assertEquals("Alice", saved.captured.name)
        assertEquals("https://example.com/new.png", saved.captured.icon?.url)
    }

    @Test
    fun `Clear を渡すとアイコンが未設定に戻る`() {
        every { userRepository.findById(userId) } returns existingUser()
        val saved = stubSave()

        service.execute(userId, icon = IconUpdate.Clear)

        assertNull(saved.captured.icon)
    }

    @Test
    fun `Unchanged ではアイコンを消さない`() {
        every { userRepository.findById(userId) } returns existingUser()
        val saved = stubSave()

        service.execute(userId, name = "Alice Updated", icon = IconUpdate.Unchanged)

        assertEquals("https://example.com/old.png", saved.captured.icon?.url)
    }

    @Test
    fun `更新しても id と createdAt は変わらない`() {
        every { userRepository.findById(userId) } returns existingUser()
        val saved = stubSave()

        service.execute(userId, name = "Alice Updated")

        assertEquals(userId, saved.captured.id)
        assertEquals(createdAt, saved.captured.createdAt)
    }

    @Test
    fun `プロフィール行が無ければ UserNotFoundException`() {
        every { userRepository.findById(userId) } returns null

        assertFailsWith<UserNotFoundException> {
            service.execute(userId, name = "Alice Updated")
        }

        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `空白だけの表示名はドメインの不変条件で弾かれ保存されない`() {
        every { userRepository.findById(userId) } returns existingUser()

        assertFailsWith<IllegalArgumentException> {
            service.execute(userId, name = "   ")
        }

        verify(exactly = 0) { userRepository.save(any()) }
    }
}
