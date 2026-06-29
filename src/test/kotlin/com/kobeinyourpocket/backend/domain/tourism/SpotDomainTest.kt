package com.kobeinyourpocket.backend.domain.tourism

import com.kobeinyourpocket.backend.domain.tourism.aggregate.Spot
import com.kobeinyourpocket.backend.domain.tourism.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.vo.Language
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotLocalization
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotLocalizations
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotMedia
import com.kobeinyourpocket.backend.domain.tourism.vo.SpotRating
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CoordinatesTest {
    @Test
    fun `有効な緯度経度で生成できる`() {
        val coordinates = Coordinates.of(latitude = 34.6826, longitude = 135.1863)

        assertEquals(34.6826, coordinates.latitude)
        assertEquals(135.1863, coordinates.longitude)
    }

    @Test
    fun `緯度が範囲外なら拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            Coordinates.of(latitude = 91.0, longitude = 135.0)
        }
    }

    @Test
    fun `経度が範囲外なら拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            Coordinates.of(latitude = 34.0, longitude = -181.0)
        }
    }
}

class SpotIdTest {
    @Test
    fun `slug 形式の ID を生成できる`() {
        assertEquals("kobe-port-tower", SpotId.of("kobe-port-tower").value)
    }

    @Test
    fun `前後空白は trim される`() {
        assertEquals("kobe-port-tower", SpotId.of("  kobe-port-tower  ").value)
    }

    @Test
    fun `空文字は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            SpotId.of("   ")
        }
    }
}

class GenreTest {
    @Test
    fun `Client と同じ既定値を持つ`() {
        assertEquals("landmark", Genre.LANDMARK.value)
        assertEquals(Genre.ONSEN, Genre.of("onsen"))
    }

    @Test
    fun `運営側で追加された任意のジャンルを生成できる`() {
        assertEquals("matsuri", Genre.of("matsuri").value)
    }

    @Test
    fun `空文字は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            Genre.of("   ")
        }
    }
}

class SpotTest {
    @Test
    fun `言語非依存フィールドで Spot を生成できる`() {
        val spot =
            Spot.create(
                id = SpotId.of("kobe-port-tower"),
                genre = Genre.LANDMARK,
                coordinates = Coordinates.of(34.6826, 135.1863),
                media = SpotMedia("https://example.com/kobe-port-tower.webp"),
            )

        assertEquals("kobe-port-tower", spot.id.value)
        assertEquals(Genre.LANDMARK, spot.genre)
        assertNull(spot.rating)
    }

    @Test
    fun `rating は任意`() {
        val spot =
            Spot.create(
                id = SpotId.of("mount-rokko"),
                genre = Genre.NATURE,
                coordinates = Coordinates.of(34.7488, 135.2231),
                media = SpotMedia("https://example.com/rokko.webp"),
                rating = SpotRating(4.7),
            )

        assertEquals(4.7, spot.rating?.value)
    }
}

class SpotRatingTest {
    @Test
    fun `0から5の範囲外は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            SpotRating(5.1)
        }
    }
}

class LanguageTest {
    @Test
    fun `対応コードから解決できる`() {
        assertEquals(Language.JA, Language.of("ja"))
        assertEquals(Language.EN, Language.of("en"))
        assertEquals(Language.KO, Language.of("ko"))
        assertEquals(Language.ZH, Language.of("zh"))
    }

    @Test
    fun `前後空白と大文字を正規化する`() {
        assertEquals(Language.EN, Language.of("  EN  "))
    }

    @Test
    fun `未対応コードは null を返す`() {
        assertNull(Language.of("fr"))
        assertNull(Language.of(""))
    }

    @Test
    fun `既定言語は ja`() {
        assertEquals(Language.JA, Language.DEFAULT)
    }
}

class SpotLocalizationTest {
    @Test
    fun `言語別フィールドを保持する`() {
        val localization =
            SpotLocalization(
                name = "神戸ポートタワー",
                categoryLabel = "ランドマーク",
                description = "神戸のシンボル。",
                businessHours = "9:00-23:00",
            )

        assertEquals("神戸ポートタワー", localization.name)
        assertEquals("ランドマーク", localization.categoryLabel)
    }

    @Test
    fun `name が空なら拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            SpotLocalization(name = "  ", categoryLabel = "ランドマーク", description = "x", businessHours = "")
        }
    }

    @Test
    fun `categoryLabel が空なら拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            SpotLocalization(name = "神戸ポートタワー", categoryLabel = "  ", description = "x", businessHours = "")
        }
    }
}

class SpotLocalizationsTest {
    private val ja =
        SpotLocalization(
            name = "神戸ポートタワー",
            categoryLabel = "ランドマーク",
            description = "神戸のシンボル。",
            businessHours = "9:00-23:00",
        )
    private val en =
        SpotLocalization(
            name = "Kobe Port Tower",
            categoryLabel = "Landmark",
            description = "The symbol of Kobe.",
            businessHours = "9:00-23:00",
        )

    @Test
    fun `要求言語のローカライズを返す`() {
        val localizations = SpotLocalizations.of(mapOf(Language.JA to ja, Language.EN to en))

        assertEquals(en, localizations.resolve(Language.EN))
    }

    @Test
    fun `要求言語が無ければ ja へフォールバックする`() {
        val localizations = SpotLocalizations.of(mapOf(Language.JA to ja, Language.EN to en))

        assertEquals(ja, localizations.resolve(Language.KO))
    }

    @Test
    fun `収録言語の集合を返す`() {
        val localizations = SpotLocalizations.of(mapOf(Language.JA to ja, Language.EN to en))

        assertEquals(setOf(Language.JA, Language.EN), localizations.languages)
    }

    @Test
    fun `ja を含まない場合は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            SpotLocalizations.of(mapOf(Language.EN to en))
        }
    }
}
