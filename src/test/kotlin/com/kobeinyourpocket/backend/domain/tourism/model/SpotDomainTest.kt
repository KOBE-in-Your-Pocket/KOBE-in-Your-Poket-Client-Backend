package com.kobeinyourpocket.backend.domain.tourism.model

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
    fun `Client と同じ apiValue を持つ`() {
        assertEquals("landmark", Genre.LANDMARK.apiValue)
        assertEquals(Genre.ONSEN, Genre.fromApiValue("onsen"))
    }

    @Test
    fun `未知の genre は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            Genre.fromApiValue("unknown")
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
