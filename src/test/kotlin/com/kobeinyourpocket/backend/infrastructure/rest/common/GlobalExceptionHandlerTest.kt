package com.kobeinyourpocket.backend.infrastructure.rest.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    fun `IllegalArgumentException は 400 とメッセージを返す`() {
        val response = handler.handleIllegalArgument(IllegalArgumentException("Genre must not be blank"))

        assertEquals(400, response.statusCode.value())
        assertEquals("Genre must not be blank", response.body?.message)
        assertEquals("Bad Request", response.body?.error)
        assertTrue(response.body?.violations?.isEmpty() == true)
    }
}
