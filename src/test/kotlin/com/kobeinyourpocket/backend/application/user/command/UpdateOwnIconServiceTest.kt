package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.application.media.ImageNormalizer
import com.kobeinyourpocket.backend.application.media.MediaStorage
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import com.kobeinyourpocket.backend.domain.user.vo.UserIcon
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.util.unit.DataSize
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 本人によるアイコン差し替え（#184）。
 *
 * 見たいのは「画像が消えない / 残らない」ための順序。確定（commit）は保存の前で、
 * 参照されなくなった画像はトランザクションの決着後に清理対象へ戻す。
 */
class UpdateOwnIconServiceTest {
    private val userId = User.Id.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
    private val oldIconUrl = "https://cdn.example.com/uploads/old.jpg"
    private val newIconUrl = "https://cdn.example.com/uploads/new.jpg"

    private val userRepository = mockk<UserRepository>()
    private val mediaStorage = mockk<MediaStorage>(relaxed = true)
    private val imageNormalizer = mockk<ImageNormalizer>()
    private val updateOwnProfileService = UpdateOwnProfileService(userRepository)

    private val service =
        UpdateOwnIconService(
            userRepository = userRepository,
            updateOwnProfileService = updateOwnProfileService,
            mediaStorage = mediaStorage,
            imageNormalizer = imageNormalizer,
            maxFileSize = DataSize.ofMegabytes(2),
        )

    /** 本番では Spring が同期を張る。単体テストでは同期だけ有効化し、決着は手で再現する。 */
    @BeforeTest
    fun beginTransaction() {
        TransactionSynchronizationManager.initSynchronization()
        every { imageNormalizer.normalize(any(), any()) } returns
            ImageNormalizer.NormalizedImage(
                bytes = byteArrayOf(1, 2, 3),
                contentType = "image/jpeg",
                extension = "jpg",
            )
        every { mediaStorage.store(any(), any(), any()) } returns newIconUrl
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

    private fun existingUser(icon: String? = oldIconUrl): User = User.create(id = userId, name = "Alice", icon = icon?.let(UserIcon::of))

    private fun stubSave(): io.mockk.CapturingSlot<User> {
        val saved = slot<User>()
        every { userRepository.save(capture(saved)) } answers { saved.captured }
        return saved
    }

    @Test
    fun `アイコンを差し替えると新しい URL が保存される`() {
        every { userRepository.findById(userId) } returns existingUser()
        val saved = stubSave()

        val updated = service.execute(userId, byteArrayOf(9, 9, 9))

        assertEquals(newIconUrl, saved.captured.icon?.url)
        assertEquals(newIconUrl, updated.icon?.url)
        // 表示名は触らない。
        assertEquals("Alice", saved.captured.name)
    }

    @Test
    fun `保存されるのは正規化後のバイト列で、キーは icons 配下`() {
        every { userRepository.findById(userId) } returns existingUser()
        stubSave()
        val key = slot<String>()
        val bytes = slot<ByteArray>()
        every { mediaStorage.store(capture(key), capture(bytes), any()) } returns newIconUrl

        service.execute(userId, byteArrayOf(9, 9, 9))

        // 受け取った生バイトではなく、正規化（縮小・再エンコード）の結果を置く。
        assertEquals(listOf<Byte>(1, 2, 3), bytes.captured.toList())
        // 運営が上げた画像（uploads/）と混ざると、不適切画像の対処や容量把握で見分けが付かない。
        // ライフサイクル規則も prefix で絞るため、ここが変わると未確定画像が消えなくなる。
        assertTrue(key.captured.startsWith("icons/"), "キーが icons 配下でない: ${key.captured}")
        assertTrue(key.captured.endsWith(".jpg"))
    }

    @Test
    fun `確定は保存より先に行う`() {
        every { userRepository.findById(userId) } returns existingUser()
        stubSave()

        service.execute(userId, byteArrayOf(9, 9, 9))

        // 逆順だと、保存に成功した直後に落ちた場合に未確定のまま期限切れで画像が消える。
        verifyOrder {
            mediaStorage.store(any(), any(), any())
            mediaStorage.commit(newIconUrl)
            userRepository.save(any())
        }
    }

    @Test
    fun `コミット後に旧アイコンを清理対象へ戻す`() {
        every { userRepository.findById(userId) } returns existingUser()
        stubSave()

        service.execute(userId, byteArrayOf(9, 9, 9))
        verify(exactly = 0) { mediaStorage.release(any()) }

        commitTransaction()

        verify(exactly = 1) { mediaStorage.release(oldIconUrl) }
        verify(exactly = 0) { mediaStorage.release(newIconUrl) }
    }

    @Test
    fun `ロールバック時は新しい画像を清理対象へ戻す`() {
        every { userRepository.findById(userId) } returns existingUser()
        stubSave()

        service.execute(userId, byteArrayOf(9, 9, 9))
        rollbackTransaction()

        // 確定済みのまま誰からも参照されない画像が残らないようにする。
        verify(exactly = 1) { mediaStorage.release(newIconUrl) }
        verify(exactly = 0) { mediaStorage.release(oldIconUrl) }
    }

    @Test
    fun `アイコン未設定からの初回設定でも、ロールバックすれば新しい画像を戻す`() {
        every { userRepository.findById(userId) } returns existingUser(icon = null)
        stubSave()

        service.execute(userId, byteArrayOf(9, 9, 9))
        rollbackTransaction()

        verify(exactly = 1) { mediaStorage.release(newIconUrl) }
    }

    @Test
    fun `アイコン未設定からの初回設定では、コミット時に何も戻さない`() {
        every { userRepository.findById(userId) } returns existingUser(icon = null)
        stubSave()

        service.execute(userId, byteArrayOf(9, 9, 9))
        commitTransaction()

        verify(exactly = 0) { mediaStorage.release(any()) }
    }

    @Test
    fun `清理の失敗はユースケースを壊さない`() {
        every { userRepository.findById(userId) } returns existingUser()
        stubSave()
        every { mediaStorage.release(any()) } throws RuntimeException("S3 down")

        service.execute(userId, byteArrayOf(9, 9, 9))

        // 決着済みなので、ここで投げても呼び出し側にできることは無い。
        commitTransaction()
    }

    @Test
    fun `プロフィール行が無ければ画像を置かずに UserNotFoundException`() {
        every { userRepository.findById(userId) } returns null

        assertFailsWith<UserNotFoundException> { service.execute(userId, byteArrayOf(9, 9, 9)) }

        // 存在しない利用者のために置いた画像は、誰も参照せず誰も消せない。
        verify(exactly = 0) { mediaStorage.store(any(), any(), any()) }
    }

    @Test
    fun `上限を超えるサイズは画像を置かずに弾く`() {
        every { userRepository.findById(userId) } returns existingUser()

        assertFailsWith<IllegalArgumentException> {
            service.execute(userId, ByteArray(DataSize.ofMegabytes(2).toBytes().toInt() + 1))
        }

        verify(exactly = 0) { mediaStorage.store(any(), any(), any()) }
    }

    @Test
    fun `空のファイルは受け付けない`() {
        assertFailsWith<IllegalArgumentException> { service.execute(userId, ByteArray(0)) }
    }

    @Test
    fun `画像として読めない入力では画像を置かない`() {
        every { userRepository.findById(userId) } returns existingUser()
        every { imageNormalizer.normalize(any(), any()) } throws IllegalArgumentException("not an image")

        assertFailsWith<IllegalArgumentException> { service.execute(userId, byteArrayOf(9, 9, 9)) }

        verify(exactly = 0) { mediaStorage.store(any(), any(), any()) }
    }
}
