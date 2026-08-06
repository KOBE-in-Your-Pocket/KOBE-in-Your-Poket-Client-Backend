package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.media.MediaStorage
import com.kobeinyourpocket.backend.application.tourism.SpotNotFoundException
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
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class UpdateSpotServiceTest {
    private val spotId = SpotId.of("kobe-port-tower")
    private val oldImageUrl = "https://example.com/old.webp"
    private val newImageUrl = "https://example.com/new.webp"

    /**
     * 本番では Spring がトランザクション同期を張る。単体テストでは同期だけ有効化し、
     * コミット / ロールバックの決着は [completeTransaction] で再現する。
     */
    @BeforeTest
    fun beginTransaction() {
        TransactionSynchronizationManager.initSynchronization()
    }

    @AfterTest
    fun endTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    private fun completeTransaction(status: Int) {
        TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCompletion(status) }
    }

    private fun commitTransaction() = completeTransaction(TransactionSynchronization.STATUS_COMMITTED)

    private fun rollbackTransaction() = completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK)

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

    private fun update(
        repository: SpotRepository,
        storage: MediaStorage,
        imageUrl: String = newImageUrl,
    ) = UpdateSpotService(repository, storage).updateSpot(
        id = spotId,
        genre = Genre.GOURMET,
        coordinates = Coordinates.of(34.7000, 135.2000),
        media = SpotMedia(imageUrl),
        localizations = newLocalizations,
    )

    @Test
    fun `updateSpot は id と rating を保持しつつ他フィールドを差し替えて保存する`() {
        val repository = mockk<SpotRepository>()
        every { repository.findSpotByIdForUpdate(spotId) } returns existingSpot()
        val saved = slot<SpotWithLocalizations>()
        every { repository.save(capture(saved)) } answers { saved.captured }

        val updated = update(repository, mediaStorage())

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
    fun `同時更新を直列化するため行ロック付きで読む`() {
        val repository = mockk<SpotRepository>()
        every { repository.findSpotByIdForUpdate(spotId) } returns existingSpot()
        every { repository.save(any()) } answers { firstArg() }

        update(repository, mediaStorage())

        // ロック無しの読み取りだと、同時更新で後勝ちの上書きと画像の誤削除が起きる。
        verify(exactly = 1) { repository.findSpotByIdForUpdate(spotId) }
        verify(exactly = 0) { repository.findSpotById(any()) }
    }

    @Test
    fun `updateSpot は該当 id が無ければ SpotNotFoundException を投げ save しない`() {
        val repository = mockk<SpotRepository>()
        val storage = mediaStorage()
        every { repository.findSpotByIdForUpdate(spotId) } returns null

        assertFailsWith<SpotNotFoundException> { update(repository, storage) }

        verify(exactly = 0) { repository.save(any()) }
        // 存在しないスポット向けの確定でストレージを触らない。
        verify(exactly = 0) { storage.commit(any()) }
    }

    @Test
    fun `画像を差し替えたら 新画像を保存前に確定し コミット後に旧画像を差し戻す`() {
        val repository = mockk<SpotRepository>()
        val storage = mediaStorage()
        every { repository.findSpotByIdForUpdate(spotId) } returns existingSpot()
        every { repository.save(any()) } answers { firstArg() }

        update(repository, storage)

        verifyOrder {
            storage.commit(newImageUrl)
            repository.save(any())
        }
        // コミット前に差し戻すと、その後ロールバックしたとき現役の画像を消してしまう。
        verify(exactly = 0) { storage.release(any()) }

        commitTransaction()

        verify(exactly = 1) { storage.release(oldImageUrl) }
        verify(exactly = 0) { storage.release(newImageUrl) }
    }

    @Test
    fun `画像を変えていなければ差し戻さない（参照中の画像を消さない）`() {
        val repository = mockk<SpotRepository>()
        val storage = mediaStorage()
        every { repository.findSpotByIdForUpdate(spotId) } returns existingSpot()
        every { repository.save(any()) } answers { firstArg() }

        update(repository, storage, imageUrl = oldImageUrl)
        commitTransaction()

        verify(exactly = 1) { storage.commit(oldImageUrl) }
        verify(exactly = 0) { storage.release(any()) }
    }

    @Test
    fun `ロールバックしたら差し替えた新画像を staging へ戻す`() {
        val repository = mockk<SpotRepository>()
        val storage = mediaStorage()
        every { repository.findSpotByIdForUpdate(spotId) } returns existingSpot()
        every { repository.save(any()) } throws RuntimeException("db down")

        assertFailsWith<RuntimeException> { update(repository, storage) }
        rollbackTransaction()

        verify(exactly = 1) { storage.release(newImageUrl) }
        // 更新は失敗＝スポットは旧画像を参照したまま。戻すと消えてしまう。
        verify(exactly = 0) { storage.release(oldImageUrl) }
    }

    @Test
    fun `ロールバックでも画像を変えていなければ差し戻さない`() {
        val repository = mockk<SpotRepository>()
        val storage = mediaStorage()
        every { repository.findSpotByIdForUpdate(spotId) } returns existingSpot()
        every { repository.save(any()) } throws RuntimeException("db down")

        assertFailsWith<RuntimeException> { update(repository, storage, imageUrl = oldImageUrl) }
        rollbackTransaction()

        verify(exactly = 0) { storage.release(any()) }
    }

    @Test
    fun `決着が不明な場合はどちらも差し戻さない`() {
        val repository = mockk<SpotRepository>()
        val storage = mediaStorage()
        every { repository.findSpotByIdForUpdate(spotId) } returns existingSpot()
        every { repository.save(any()) } answers { firstArg() }

        update(repository, storage)
        completeTransaction(TransactionSynchronization.STATUS_UNKNOWN)

        // どちらが現役か判断できないため、消さない側に倒す。
        verify(exactly = 0) { storage.release(any()) }
    }

    @Test
    fun `旧画像の差し戻しに失敗しても決着処理は例外を投げない`() {
        val repository = mockk<SpotRepository>()
        val storage = mediaStorage()
        every { repository.findSpotByIdForUpdate(spotId) } returns existingSpot()
        every { repository.save(any()) } answers { firstArg() }
        every { storage.release(oldImageUrl) } throws IllegalStateException("s3 down")

        val updated = update(repository, storage)
        commitTransaction()

        assertEquals(newImageUrl, updated.spot.media.imageUrl)
    }
}
