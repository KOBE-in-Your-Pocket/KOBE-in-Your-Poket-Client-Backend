package com.kobeinyourpocket.backend.infrastructure.query.tourism

import com.kobeinyourpocket.backend.application.tourism.query.SpotViewCount
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.view.model.SpotView
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotLocalizationEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotLocalizationId
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotLocalizationJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotViewEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotViewJpaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/** 閲覧数の集計（#239 / #241）。スポット別の合計・多い順・削除済みスポットの除外・スポット名の解決を実 DB で確かめる。 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(SpotViewCountQueryJpa::class)
class SpotViewCountQueryJpaTest {
    @Autowired
    private lateinit var spotJpa: SpotJpaRepository

    @Autowired
    private lateinit var localizationJpa: SpotLocalizationJpaRepository

    @Autowired
    private lateinit var spotViewJpa: SpotViewJpaRepository

    @Autowired
    private lateinit var query: SpotViewCountQueryJpa

    private val viewedAt = Instant.parse("2026-11-01T10:00:00Z")

    private fun saveSpot(id: String) {
        spotJpa.save(
            SpotEntity(id = id, genre = "landmark", latitude = 34.6, longitude = 135.1, imageUrl = "https://example.com/$id.jpg"),
        )
    }

    private fun saveName(
        spotId: String,
        language: Language,
        name: String,
    ) {
        localizationJpa.save(
            SpotLocalizationEntity(
                id = SpotLocalizationId(spotId = spotId, language = language.code),
                name = name,
                categoryLabel = "",
                description = "",
                businessHours = "",
                address = "",
            ),
        )
    }

    private fun saveViews(
        spotId: String,
        times: Int,
    ) {
        repeat(times) {
            spotViewJpa.save(SpotViewEntity.fromDomain(SpotView.create(spotId = SpotId.of(spotId), viewedAt = viewedAt)))
        }
        spotViewJpa.flush()
    }

    @Test
    fun `スポットの閲覧数を返す`() {
        saveSpot("kobe-harborland")
        saveSpot("kitano-ijinkan")
        saveViews("kobe-harborland", 3)
        saveViews("kitano-ijinkan", 1)

        assertEquals(3, query.countBySpotId(SpotId.of("kobe-harborland")))
    }

    @Test
    fun `閲覧が無いスポットは 0 を返す`() {
        saveSpot("kobe-harborland")

        assertEquals(0, query.countBySpotId(SpotId.of("kobe-harborland")))
    }

    @Test
    fun `スポット別の閲覧数を多い順に返し、同数は spot_id 順に並べる`() {
        saveSpot("kobe-harborland")
        saveSpot("kitano-ijinkan")
        saveSpot("meriken-park")
        saveViews("kobe-harborland", 1)
        saveViews("kitano-ijinkan", 3)
        saveViews("meriken-park", 1)

        assertEquals(
            listOf(
                SpotViewCount(spotId = "kitano-ijinkan", name = "kitano-ijinkan", viewCount = 3),
                SpotViewCount(spotId = "kobe-harborland", name = "kobe-harborland", viewCount = 1),
                SpotViewCount(spotId = "meriken-park", name = "meriken-park", viewCount = 1),
            ),
            query.countAllBySpot(Language.JA),
        )
    }

    @Test
    fun `閲覧が 1 件も無いスポットは一覧に含めない`() {
        saveSpot("kobe-harborland")
        saveSpot("kitano-ijinkan")
        saveViews("kobe-harborland", 2)

        assertEquals(
            listOf(SpotViewCount(spotId = "kobe-harborland", name = "kobe-harborland", viewCount = 2)),
            query.countAllBySpot(Language.JA),
        )
    }

    @Test
    fun `削除済みのスポットの閲覧は一覧に含めない`() {
        saveSpot("kobe-harborland")
        saveViews("kobe-harborland", 2)
        saveViews("deleted-spot", 5)

        assertEquals(
            listOf(SpotViewCount(spotId = "kobe-harborland", name = "kobe-harborland", viewCount = 2)),
            query.countAllBySpot(Language.JA),
        )
    }

    @Test
    fun `閲覧が 1 件も無ければ空の一覧を返す`() {
        saveSpot("kobe-harborland")

        assertEquals(emptyList(), query.countAllBySpot(Language.JA))
    }

    @Test
    fun `スポット名は要求言語で返す`() {
        saveSpot("kobe-harborland")
        saveName("kobe-harborland", Language.JA, "神戸ハーバーランド")
        saveName("kobe-harborland", Language.EN, "Kobe Harborland")
        saveViews("kobe-harborland", 2)

        assertEquals(
            listOf(SpotViewCount(spotId = "kobe-harborland", name = "神戸ハーバーランド", viewCount = 2)),
            query.countAllBySpot(Language.JA),
        )
    }

    @Test
    fun `要求言語の名前が無ければ既定言語（en）の名前を返す`() {
        saveSpot("kobe-harborland")
        saveName("kobe-harborland", Language.EN, "Kobe Harborland")
        saveViews("kobe-harborland", 1)

        assertEquals(
            listOf(SpotViewCount(spotId = "kobe-harborland", name = "Kobe Harborland", viewCount = 1)),
            query.countAllBySpot(Language.KO),
        )
    }

    @Test
    fun `名前が複数言語あっても閲覧数は重複して数えない`() {
        saveSpot("kobe-harborland")
        saveName("kobe-harborland", Language.JA, "神戸ハーバーランド")
        saveName("kobe-harborland", Language.EN, "Kobe Harborland")
        saveName("kobe-harborland", Language.KO, "고베 하버랜드")
        saveViews("kobe-harborland", 3)

        assertEquals(3, query.countAllBySpot(Language.ZH).single().viewCount)
    }
}
