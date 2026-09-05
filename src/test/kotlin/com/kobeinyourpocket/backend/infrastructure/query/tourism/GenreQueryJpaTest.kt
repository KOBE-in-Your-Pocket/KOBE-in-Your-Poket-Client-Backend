package com.kobeinyourpocket.backend.infrastructure.query.tourism

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreCode
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.GenreEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.GenreJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.GenreLocalizationEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.GenreLocalizationId
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.GenreLocalizationJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotEntity
import com.kobeinyourpocket.backend.infrastructure.persistence.tourism.SpotJpaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test
import kotlin.test.assertEquals

/** ジャンル一覧のクエリ（#153）。並び順・件数集計・全言語のまとめ方を実 DB で確かめる。 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(GenreQueryJpa::class)
class GenreQueryJpaTest {
    @Autowired
    private lateinit var genreJpa: GenreJpaRepository

    @Autowired
    private lateinit var localizationJpa: GenreLocalizationJpaRepository

    @Autowired
    private lateinit var spotJpa: SpotJpaRepository

    @Autowired
    private lateinit var query: GenreQueryJpa

    private fun saveGenre(
        code: String,
        order: Int,
        ja: String,
    ) {
        genreJpa.save(GenreEntity(code = code, displayOrder = order))
        listOf(Language.JA to ja, Language.EN to "$code-en", Language.KO to "$code-ko", Language.ZH to "$code-zh")
            .forEach { (language, label) ->
                localizationJpa.save(
                    GenreLocalizationEntity(
                        id = GenreLocalizationId(genreCode = code, language = language.code),
                        label = label,
                    ),
                )
            }
    }

    private fun saveSpot(
        id: String,
        genre: String,
    ) {
        spotJpa.save(
            SpotEntity(
                id = id,
                genre = genre,
                latitude = 34.6,
                longitude = 135.1,
                imageUrl = "https://example.com/$id.jpg",
            ),
        )
    }

    @Test
    fun `display_order の昇順で返し、全言語のラベルをまとめる`() {
        saveGenre("onsen", order = 5, ja = "温泉")
        saveGenre("landmark", order = 1, ja = "名所")

        val genres = query.findAll()

        assertEquals(listOf("landmark", "onsen"), genres.map { it.code })
        assertEquals(4, genres[0].labels.size)
        assertEquals("名所", genres[0].labels["ja"])
        assertEquals("landmark-ko", genres[0].labels["ko"])
    }

    @Test
    fun `display_order が同値なら code 順で安定させる`() {
        saveGenre("b-genre", order = 1, ja = "B")
        saveGenre("a-genre", order = 1, ja = "A")

        assertEquals(listOf("a-genre", "b-genre"), query.findAll().map { it.code })
    }

    @Test
    fun `スポット件数を集計する`() {
        saveGenre("onsen", order = 1, ja = "温泉")
        saveGenre("nature", order = 2, ja = "自然")
        saveSpot("arima-onsen", "onsen")
        saveSpot("kinosaki", "onsen")
        saveSpot("mount-rokko", "nature")

        val byCode = query.findAll().associateBy { it.code }

        assertEquals(2, byCode.getValue("onsen").spotCount)
        assertEquals(1, byCode.getValue("nature").spotCount)
    }

    @Test
    fun `使われていないジャンルも一覧から消えない`() {
        // 0 件のジャンルこそ削除候補として見せたいため、spot との JOIN で絞らない。
        saveGenre("unused", order = 1, ja = "未使用")

        val genres = query.findAll()

        assertEquals(listOf("unused"), genres.map { it.code })
        assertEquals(0, genres.single().spotCount)
    }

    @Test
    fun `code 指定でスポット件数を数える`() {
        saveGenre("onsen", order = 1, ja = "温泉")
        saveSpot("arima-onsen", "onsen")
        saveSpot("kinosaki", "onsen")

        assertEquals(2, query.countSpotsByGenre(GenreCode.of("onsen")))
        assertEquals(0, query.countSpotsByGenre(GenreCode.of("nature")))
    }
}
