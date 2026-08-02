package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.media.MediaStorage
import com.kobeinyourpocket.backend.application.tourism.query.SpotNotFoundException
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.model.Spot
import com.kobeinyourpocket.backend.domain.tourism.spot.model.SpotWithLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.repository.SpotRepository
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalization
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotLocalizations
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotMedia
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotRating
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class UpdateSpotServiceTest {
    private val spotId = SpotId.of("kobe-port-tower")
    private val oldImageUrl = "https://example.com/old.webp"
    private val newImageUrl = "https://example.com/new.webp"

    private val newLocalizations =
        SpotLocalizations.of(
            mapOf(
                Language.JA to
                    SpotLocalization(
                        "神戸ポートタワー（改）",
                        "展望",
                        "リニューアル後の説明。",
                        "10:00-22:00",
                        "神戸市中央区波止場町5-5",
                    ),
                Language.EN to
                    SpotLocalization(
                        "Kobe Port Tower (renewed)",
                        "Observation",
                        "Description after renewal.",
                        "10:00-22:00",
                        "5-5 Hatobacho, Chuo-ku, Kobe",
                    ),
            ),
        )

    private fun existingSpot() =
        Spot(
            id = spotId,
            genre = Genre.LANDMARK,
            coordinates = Coordinates.of(34.6826, 135.1863),
            media = SpotMedia(oldImageUrl),
            rating = SpotRating(4.2),
        )

    /** 確定・差し戻しとも「呼ばれた」ことだけ分かれば良いので既定は true を返す。 */
    private fun mediaStorage(): MediaStorage =
        mockk<MediaStorage>().also {
            every { it.commit(any()) } returns true
            every { it.release(any()) } returns true
        }

    @Test
    fun `updateSpot は id と rating を保持しつつ他フィールドを差し替えて保存する`() {
        val repository = mockk<SpotRepository>()
        every { repository.findSpotById(spotId) } returns existingSpot()
        val saved = slot<SpotWithLocalizations>()
        every { repository.save(capture(saved)) } answers { saved.captured }

        val updated =
            UpdateSpotService(repository, mediaStorage()).updateSpot(
                id = spotId,
                genre = Genre.GOURMET,
                coordinates = Coordinates.of(34.7000, 135.2000),
                media = SpotMedia(newImageUrl),
                localizations = newLocalizations,
            )

        // id と rating は不変
        assertEquals(spotId, updated.spot.id)
        assertEquals(SpotRating(4.2), updated.spot.rating)
        // 差し替え対象は反映
        assertEquals(Genre.GOURMET, updated.spot.genre)
        assertEquals(Coordinates.of(34.7000, 135.2000), updated.spot.coordinates)
        assertEquals(newImageUrl, updated.spot.media.imageUrl)
        assertSame(newLocalizations, updated.localizations)
        verify(exactly = 1) { repository.save(saved.captured) }
    }

    @Test
    fun `updateSpot は該当 id が無ければ SpotNotFoundException を投げ save しない`() {
        val repository = mockk<SpotRepository>()
        val storage = mediaStorage()
        every { repository.findSpotById(spotId) } returns null

        assertFailsWith<SpotNotFoundException> {
            UpdateSpotService(repository, storage).updateSpot(
                id = spotId,
                genre = Genre.GOURMET,
                coordinates = Coordinates.of(34.7000, 135.2000),
                media = SpotMedia(newImageUrl),
                localizations = newLocalizations,
            )
        }

        verify(exactly = 0) { repository.save(any()) }
        // 存在しないスポット向けの確定でストレージを触らない。
        verify(exactly = 0) { storage.commit(any()) }
    }

    @Test
    fun `画像を差し替えたら 新画像を保存前に確定し 保存後に旧画像を差し戻す`() {
        val repository = mockk<SpotRepository>()
        val storage = mediaStorage()
        every { repository.findSpotById(spotId) } returns existingSpot()
        every { repository.save(any()) } answers { firstArg() }

        UpdateSpotService(repository, storage).updateSpot(
            id = spotId,
            genre = Genre.GOURMET,
            coordinates = Coordinates.of(34.7000, 135.2000),
            media = SpotMedia(newImageUrl),
            localizations = newLocalizations,
        )

        // 旧画像の差し戻しは保存後（保存前に戻すと、保存失敗時に参照中の画像を消す）。
        verifyOrder {
            storage.commit(newImageUrl)
            repository.save(any())
            storage.release(oldImageUrl)
        }
        verify(exactly = 0) { storage.release(newImageUrl) }
    }

    @Test
    fun `画像を変えていなければ差し戻さない（参照中の画像を消さない）`() {
        val repository = mockk<SpotRepository>()
        val storage = mediaStorage()
        every { repository.findSpotById(spotId) } returns existingSpot()
        every { repository.save(any()) } answers { firstArg() }

        UpdateSpotService(repository, storage).updateSpot(
            id = spotId,
            genre = Genre.GOURMET,
            coordinates = Coordinates.of(34.7000, 135.2000),
            media = SpotMedia(oldImageUrl),
            localizations = newLocalizations,
        )

        verify(exactly = 1) { storage.commit(oldImageUrl) }
        verify(exactly = 0) { storage.release(any()) }
    }

    @Test
    fun `保存に失敗し画像を差し替えていたら新画像を staging へ戻す`() {
        val repository = mockk<SpotRepository>()
        val storage = mediaStorage()
        every { repository.findSpotById(spotId) } returns existingSpot()
        every { repository.save(any()) } throws RuntimeException("db down")

        assertFailsWith<RuntimeException> {
            UpdateSpotService(repository, storage).updateSpot(
                id = spotId,
                genre = Genre.GOURMET,
                coordinates = Coordinates.of(34.7000, 135.2000),
                media = SpotMedia(newImageUrl),
                localizations = newLocalizations,
            )
        }

        verify(exactly = 1) { storage.release(newImageUrl) }
        // 更新は失敗＝スポットは旧画像を参照したまま。旧画像を戻すと消えてしまう。
        verify(exactly = 0) { storage.release(oldImageUrl) }
    }

    @Test
    fun `保存に失敗し画像を変えていなければ差し戻さない`() {
        val repository = mockk<SpotRepository>()
        val storage = mediaStorage()
        every { repository.findSpotById(spotId) } returns existingSpot()
        every { repository.save(any()) } throws RuntimeException("db down")

        assertFailsWith<RuntimeException> {
            UpdateSpotService(repository, storage).updateSpot(
                id = spotId,
                genre = Genre.GOURMET,
                coordinates = Coordinates.of(34.7000, 135.2000),
                media = SpotMedia(oldImageUrl),
                localizations = newLocalizations,
            )
        }

        // 差し戻すと、スポットがまだ参照している画像を期限切れで消してしまう。
        verify(exactly = 0) { storage.release(any()) }
    }

    @Test
    fun `旧画像の差し戻しに失敗しても更新結果を返す`() {
        val repository = mockk<SpotRepository>()
        val storage = mediaStorage()
        every { repository.findSpotById(spotId) } returns existingSpot()
        every { repository.save(any()) } answers { firstArg() }
        every { storage.release(oldImageUrl) } throws IllegalStateException("s3 down")

        val updated =
            UpdateSpotService(repository, storage).updateSpot(
                id = spotId,
                genre = Genre.GOURMET,
                coordinates = Coordinates.of(34.7000, 135.2000),
                media = SpotMedia(newImageUrl),
                localizations = newLocalizations,
            )

        assertEquals(newImageUrl, updated.spot.media.imageUrl)
    }
}
