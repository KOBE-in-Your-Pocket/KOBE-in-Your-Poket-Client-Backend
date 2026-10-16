package com.kobeinyourpocket.backend.domain.user

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.model.UserBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserBlockDomainTest {
    private val alice = User.Id.of("11111111-1111-1111-1111-111111111111")
    private val bob = User.Id.of("22222222-2222-2222-2222-222222222222")

    @Test
    fun `ほかのユーザーはブロックできる`() {
        val block = UserBlock.create(blockerId = alice, blockedId = bob)

        assertEquals(alice, block.blockerId)
        assertEquals(bob, block.blockedId)
    }

    @Test
    fun `自分自身はブロックできない`() {
        assertFailsWith<IllegalArgumentException> {
            UserBlock.create(blockerId = alice, blockedId = alice)
        }
    }
}
