package com.kobeinyourpocket.backend.application.manner.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class ListMannerItemsServiceTest {
    private val jaView =
        MannerItemView(
            id = "arima-onsen-bathing",
            title = "有馬温泉の入浴マナー",
            description = "湯船に入る前にかけ湯で体を流しましょう。",
            icon = "hot-spring",
            kind = "manner",
            scope = "local",
            relatedSpotIds = listOf("arima-onsen"),
        )

    private val enView =
        jaView.copy(
            title = "Arima Onsen bathing etiquette",
            description = "Rinse your body before entering the bath.",
        )

    @Test
    fun `要求言語を MannerItemQuery port に渡して解決済み MannerItemView を返す`() {
        val mannerItemQuery = mockk<MannerItemQuery>()
        every { mannerItemQuery.findAllResolved(Language.JA) } returns listOf(jaView)

        val result = ListMannerItemsService(mannerItemQuery).listMannerItems(Language.JA)

        assertEquals("有馬温泉の入浴マナー", result.single().title)
        verify(exactly = 1) { mannerItemQuery.findAllResolved(Language.JA) }
    }

    @Test
    fun `要求言語のローカライズが無い場合は en フォールバック済み MannerItemView を返す`() {
        val mannerItemQuery = mockk<MannerItemQuery>()
        every { mannerItemQuery.findAllResolved(Language.KO) } returns listOf(enView)

        val result = ListMannerItemsService(mannerItemQuery).listMannerItems(Language.KO)

        assertEquals("Arima Onsen bathing etiquette", result.single().title)
        verify(exactly = 1) { mannerItemQuery.findAllResolved(Language.KO) }
    }
}
