package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.media.MediaStorage
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.model.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalization
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotMedia
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegisterSpotServiceTest {
    private val localizations =
        SpotLocalizations.of(
            mapOf(
                Language.JA to
                    SpotLocalization(
                        "神戸ポートタワー",
                        "ランドマーク",
                        "神戸のシンボル。",
                        "9:00-23:00",
                        "神戸市中央区波止場町5-5",
                    ),
                Language.EN to
                    SpotLocalization(
                        "Kobe Port Tower",
                        "Landmark",
                        "The symbol of Kobe.",
                        "9:00-23:00",
                        "5-5 Hatobacho, Chuo-ku, Kobe",
                    ),
            ),
        )

    private val imageUrl = "https://example.com/x.webp"

    /** 確定・差し戻しとも「呼ばれた」ことだけ分かれば良いので既定は true を返す。 */
    private fun mediaStorage(): MediaStorage =
        mockk<MediaStorage>().also {
            every { it.commit(any()) } returns true
            every { it.release(any()) } returns true
        }

    @Test
    fun `registerSpot は採番して保存し rating は null`() {
        val repository = mockk<SpotRepository>()
        val saved = slot<SpotWithLocalizations>()
        every { repository.save(capture(saved)) } answers { saved.captured }

        val created =
            RegisterSpotService(repository, mediaStorage()).registerSpot(
                genre = Genre.LANDMARK,
                coordinates = Coordinates.of(34.6826, 135.1863),
                media = SpotMedia(imageUrl),
                localizations = localizations,
            )

        assertTrue(
            created.spot.id.value
                .isNotBlank(),
        )
        assertNull(created.spot.rating)
        assertEquals(localizations, created.localizations)
        verify(exactly = 1) { repository.save(saved.captured) }
    }

    @Test
    fun `保存の前に画像を確定して清理対象から外す`() {
        val repository = mockk<SpotRepository>()
        val storage = mediaStorage()
        every { repository.save(any()) } answers { firstArg() }

        RegisterSpotService(repository, storage).registerSpot(
            genre = Genre.LANDMARK,
            coordinates = Coordinates.of(34.6826, 135.1863),
            media = SpotMedia(imageUrl),
            localizations = localizations,
        )

        // 確定 → 保存の順。逆だと確定漏れで参照中の画像が期限切れになる。
        verifyOrder {
            storage.commit(imageUrl)
            repository.save(any())
        }
        verify(exactly = 0) { storage.release(any()) }
    }

    @Test
    fun `画像の確定に失敗したら保存せず登録を失敗させる`() {
        val repository = mockk<SpotRepository>()
        val storage = mediaStorage()
        every { storage.commit(imageUrl) } throws IllegalStateException("s3 down")

        assertFailsWith<IllegalStateException> {
            RegisterSpotService(repository, storage).registerSpot(
                genre = Genre.LANDMARK,
                coordinates = Coordinates.of(34.6826, 135.1863),
                media = SpotMedia(imageUrl),
                localizations = localizations,
            )
        }

        // 保存されていなければ画像は staging のまま期限切れで消える（不整合を残さない）。
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `保存に失敗したら確定済みの画像を staging へ戻す`() {
        val repository = mockk<SpotRepository>()
        val storage = mediaStorage()
        every { repository.save(any()) } throws RuntimeException("db down")

        assertFailsWith<RuntimeException> {
            RegisterSpotService(repository, storage).registerSpot(
                genre = Genre.LANDMARK,
                coordinates = Coordinates.of(34.6826, 135.1863),
                media = SpotMedia(imageUrl),
                localizations = localizations,
            )
        }

        verify(exactly = 1) { storage.release(imageUrl) }
    }

    @Test
    fun `差し戻しにも失敗した場合は元の保存エラーを伝える`() {
        val repository = mockk<SpotRepository>()
        val storage = mediaStorage()
        every { repository.save(any()) } throws RuntimeException("db down")
        every { storage.release(imageUrl) } throws IllegalStateException("s3 down")

        val thrown =
            assertFailsWith<RuntimeException> {
                RegisterSpotService(repository, storage).registerSpot(
                    genre = Genre.LANDMARK,
                    coordinates = Coordinates.of(34.6826, 135.1863),
                    media = SpotMedia(imageUrl),
                    localizations = localizations,
                )
            }

        assertEquals("db down", thrown.message)
    }
}
