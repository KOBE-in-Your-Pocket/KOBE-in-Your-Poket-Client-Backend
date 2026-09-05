package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.tourism.GenreInUseException
import com.kobeinyourpocket.backend.application.tourism.GenreNotFoundException
import com.kobeinyourpocket.backend.application.tourism.query.GenreQuery
import com.kobeinyourpocket.backend.application.tourism.query.GenreView
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.genre.model.Genre
import com.kobeinyourpocket.backend.domain.tourism.genre.repository.GenreRepository
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreCode
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreLocalizations
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** ジャンルの登録・更新・削除（#153）。 */
class GenreCommandServiceTest {
    /** メモリ上で振る舞う [GenreRepository]。 */
    private class FakeGenreRepository(
        initial: List<Genre> = emptyList(),
    ) : GenreRepository {
        val stored = initial.associateBy { it.code }.toMutableMap()

        override fun findByCode(code: GenreCode): Genre? = stored[code]

        override fun existsByCode(code: GenreCode): Boolean = stored.containsKey(code)

        override fun save(genre: Genre): Genre {
            stored[genre.code] = genre
            return genre
        }

        override fun deleteByCode(code: GenreCode) {
            stored.remove(code)
        }
    }

    /** スポット件数だけを返す [GenreQuery]。 */
    private class FakeGenreQuery(
        private val counts: Map<String, Long> = emptyMap(),
    ) : GenreQuery {
        override fun findAll(): List<GenreView> = emptyList()

        override fun countSpotsByGenre(code: GenreCode): Long = counts[code.value] ?: 0
    }

    private fun labels(en: String = "Night View") =
        GenreLocalizations.of(
            mapOf(Language.JA to "夜景", Language.EN to en, Language.KO to "야경", Language.ZH to "夜景"),
        )

    private fun genre(
        code: String,
        order: Int = 1,
    ) = Genre(GenreCode.of(code), displayOrder = order, localizations = labels())

    @Test
    fun `登録時に英語ラベルから code を生成する`() {
        val repository = FakeGenreRepository()
        val service = RegisterGenreService(repository)

        val created = service.registerGenre(displayOrder = 6, localizations = labels("Night View"))

        assertEquals("night-view", created.code.value)
        assertEquals(6, created.displayOrder)
        assertTrue(repository.existsByCode(GenreCode.of("night-view")))
    }

    @Test
    fun `code が衝突したら連番を付けて採番する`() {
        // 同じ英語名の別ジャンルを作ること自体は正当な操作で、ID 衝突は内部事情でしかない。
        val repository = FakeGenreRepository(listOf(genre("night-view")))
        val service = RegisterGenreService(repository)

        val created = service.registerGenre(displayOrder = 1, localizations = labels("Night View"))

        assertEquals("night-view-2", created.code.value)
    }

    @Test
    fun `連番も埋まっていればさらに繰り上げる`() {
        val repository = FakeGenreRepository(listOf(genre("night-view"), genre("night-view-2")))
        val service = RegisterGenreService(repository)

        assertEquals("night-view-3", service.registerGenre(1, labels("Night View")).code.value)
    }

    @Test
    fun `上限長ちょうどの code と衝突しても採番できる`() {
        // 単純に連結すると 64 文字を超えて GenreCode.of が弾き、採番できるはずの候補を作れない。
        val longLabel = "a".repeat(GenreCode.MAX_LENGTH)
        val existing = GenreCode.of(longLabel)
        val repository = FakeGenreRepository(listOf(Genre(existing, 1, labels(longLabel))))
        val service = RegisterGenreService(repository)

        val created = service.registerGenre(displayOrder = 1, localizations = labels(longLabel))

        assertTrue(created.code.value.length <= GenreCode.MAX_LENGTH)
        assertTrue(created.code.value.endsWith("-2"))
    }

    @Test
    fun `英語ラベルから code を作れなければ登録を拒否する`() {
        val service = RegisterGenreService(FakeGenreRepository())

        assertFailsWith<InvalidGenreLabelException> {
            service.registerGenre(displayOrder = 1, localizations = labels("---"))
        }
    }

    @Test
    fun `更新しても code は変わらない`() {
        val repository = FakeGenreRepository(listOf(genre("night-view", order = 1)))
        val service = UpdateGenreService(repository)

        val updated =
            service.updateGenre(
                code = GenreCode.of("night-view"),
                displayOrder = 9,
                localizations = labels("Nightscape"),
            )

        assertEquals("night-view", updated.code.value)
        assertEquals(9, updated.displayOrder)
        assertEquals("Nightscape", updated.localizations.resolve(Language.EN))
    }

    @Test
    fun `存在しないジャンルの更新は 404 相当の例外`() {
        val service = UpdateGenreService(FakeGenreRepository())

        assertFailsWith<GenreNotFoundException> {
            service.updateGenre(GenreCode.of("unknown"), 1, labels())
        }
    }

    @Test
    fun `使われていないジャンルは削除できる`() {
        val repository = FakeGenreRepository(listOf(genre("night-view")))
        val service = DeleteGenreService(repository, FakeGenreQuery())

        service.deleteGenre(GenreCode.of("night-view"))

        assertTrue(!repository.existsByCode(GenreCode.of("night-view")))
    }

    @Test
    fun `使用中のジャンルは削除できず件数を伝える`() {
        // 消せてしまうと参照元スポットのジャンルが不明になるため。
        val repository = FakeGenreRepository(listOf(genre("onsen")))
        val service = DeleteGenreService(repository, FakeGenreQuery(mapOf("onsen" to 3)))

        val ex = assertFailsWith<GenreInUseException> { service.deleteGenre(GenreCode.of("onsen")) }

        assertEquals(3, ex.spotCount)
        assertTrue(repository.existsByCode(GenreCode.of("onsen")))
    }

    @Test
    fun `削除直前にスポットが登録されて FK 違反になっても 409 相当に収束する`() {
        // 事前確認では 0 件でも、確認後・削除前に別トランザクションが登録しうる。
        // DB の外部キー違反（500）ではなく、事前確認と同じ 409 に寄せる。
        val repository =
            object : GenreRepository by FakeGenreRepository(listOf(genre("onsen"))) {
                override fun deleteByCode(code: GenreCode): Unit = throw DataIntegrityViolationException("fk_spot_genre")
            }
        val service = DeleteGenreService(repository, FakeGenreQuery())

        val ex = assertFailsWith<GenreInUseException> { service.deleteGenre(GenreCode.of("onsen")) }

        // 中断したトランザクションでは件数を数え直せないため不明として返す
        assertEquals(null, ex.spotCount)
    }

    @Test
    fun `存在しないジャンルの削除は 404 相当の例外`() {
        val service = DeleteGenreService(FakeGenreRepository(), FakeGenreQuery())

        assertFailsWith<GenreNotFoundException> { service.deleteGenre(GenreCode.of("unknown")) }
    }
}
