package com.kobeinyourpocket.backend.infrastructure.query.manner

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem
import com.kobeinyourpocket.backend.domain.manner.manneritem.repository.MannerRepository
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIcon
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerKind
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalization
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalizations
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerScope
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.RelatedSpotId
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.impl.MannerRepositoryImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test
import kotlin.test.assertEquals

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(MannerRepositoryImpl::class, MannerItemQueryJpa::class)
class MannerItemQueryJpaTest {
    @Autowired
    private lateinit var mannerRepository: MannerRepository

    @Autowired
    private lateinit var mannerItemQuery: MannerItemQueryJpa

    private val arimaOnsen =
        MannerItem.create(
            id = MannerItem.Id.of("arima-onsen-bathing"),
            icon = MannerIcon.of("hot-spring"),
            kind = MannerKind.MANNER,
            scope = MannerScope.LOCAL,
            localizations =
                MannerLocalizations.of(
                    mapOf(
                        Language.JA to MannerLocalization("有馬温泉の入浴マナー", "湯船に入る前にかけ湯で体を流しましょう。"),
                        Language.EN to
                            MannerLocalization(
                                "Arima Onsen bathing etiquette",
                                "Rinse your body before entering the bath.",
                            ),
                    ),
                ),
            relatedSpotIds = listOf(RelatedSpotId.of("arima-onsen")),
        )

    private val noLittering =
        MannerItem.create(
            id = MannerItem.Id.of("no-littering"),
            icon = MannerIcon.of("trash"),
            kind = MannerKind.RULE,
            scope = MannerScope.JAPAN,
            localizations =
                MannerLocalizations.of(
                    mapOf(
                        Language.EN to MannerLocalization("No littering", "Please carry your trash with you."),
                    ),
                ),
        )

    @Test
    fun `要求言語で解決した MannerItemView を返す`() {
        mannerRepository.save(arimaOnsen)

        val result = mannerItemQuery.findAllResolved(Language.JA).single()

        assertEquals("arima-onsen-bathing", result.id)
        assertEquals("有馬温泉の入浴マナー", result.title)
        assertEquals("湯船に入る前にかけ湯で体を流しましょう。", result.description)
        assertEquals("hot-spring", result.icon)
        assertEquals("manner", result.kind)
        assertEquals("local", result.scope)
        assertEquals(listOf("arima-onsen"), result.relatedSpotIds)
    }

    @Test
    fun `要求言語のローカライズが無ければ en へフォールバックする`() {
        mannerRepository.save(noLittering)

        val result = mannerItemQuery.findAllResolved(Language.KO).single()

        assertEquals("No littering", result.title)
        assertEquals("Please carry your trash with you.", result.description)
    }

    @Test
    fun `関連スポットの無い項目は relatedSpotIds が空配列になる`() {
        mannerRepository.save(noLittering)

        val result = mannerItemQuery.findAllResolved(Language.EN).single()

        assertEquals(emptyList(), result.relatedSpotIds)
    }

    @Test
    fun `全件を id 順で返す`() {
        mannerRepository.save(noLittering)
        mannerRepository.save(arimaOnsen)

        val result = mannerItemQuery.findAllResolved(Language.EN)

        assertEquals(listOf("arima-onsen-bathing", "no-littering"), result.map { it.id })
    }
}
