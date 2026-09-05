package com.kobeinyourpocket.backend.domain.tourism

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.genre.model.Genre
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreCode
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreLocalizations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** ジャンルの不変条件と code 生成規則（#153）。 */
class GenreDomainTest {
    private fun labels(
        ja: String = "夜景",
        en: String = "Night View",
        ko: String = "야경",
        zh: String = "夜景",
    ) = GenreLocalizations.of(
        mapOf(Language.JA to ja, Language.EN to en, Language.KO to ko, Language.ZH to zh),
    )

    @Test
    fun `英語ラベルから slug を作る`() {
        assertEquals("night-view", GenreCode.fromLabel("Night View")?.value)
        assertEquals("hot-spring", GenreCode.fromLabel("  Hot Spring  ")?.value)
        assertEquals("cafe-bakery", GenreCode.fromLabel("Cafe & Bakery")?.value)
        assertEquals("art-museum", GenreCode.fromLabel("Art / Museum")?.value)
    }

    @Test
    fun `slug にできない英語ラベルは null を返す`() {
        // 既定値を勝手に付けると、意図しない code が黙って残るため。
        assertNull(GenreCode.fromLabel("---"))
        assertNull(GenreCode.fromLabel("　"))
        assertNull(GenreCode.fromLabel("温泉"))
    }

    @Test
    fun `code は英小文字・数字・ハイフンの形だけ受け付ける`() {
        assertEquals("night-view", GenreCode.of("night-view").value)
        assertFailsWith<IllegalArgumentException> { GenreCode.of("Night View") }
        assertFailsWith<IllegalArgumentException> { GenreCode.of("night_view") }
        assertFailsWith<IllegalArgumentException> { GenreCode.of("-night") }
        assertFailsWith<IllegalArgumentException> { GenreCode.of("") }
    }

    @Test
    fun `表示名は全言語そろっていないと作れない`() {
        // 1 言語でも欠けると、その言語のアプリでジャンル名が出せないため。
        val missingKo = mapOf(Language.JA to "夜景", Language.EN to "Night View", Language.ZH to "夜景")
        assertFailsWith<IllegalArgumentException> { GenreLocalizations.of(missingKo) }
    }

    @Test
    fun `空白だけの表示名は欠落として扱う`() {
        assertFailsWith<IllegalArgumentException> { labels(ko = "   ") }
    }

    @Test
    fun `更新できるのは表示名と並び順だけで code は変わらない`() {
        val genre = Genre(GenreCode.of("night-view"), displayOrder = 1, localizations = labels())

        val updated = genre.update(displayOrder = 9, localizations = labels(ja = "夜景スポット"))

        assertEquals(GenreCode.of("night-view"), updated.code)
        assertEquals(9, updated.displayOrder)
        assertEquals("夜景スポット", updated.localizations.resolve(Language.JA))
    }

    @Test
    fun `並び順に負値は使えない`() {
        assertFailsWith<IllegalArgumentException> {
            Genre(GenreCode.of("night-view"), displayOrder = -1, localizations = labels())
        }
    }
}
