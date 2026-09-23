package com.kobeinyourpocket.backend.application.manner.command

import com.kobeinyourpocket.backend.application.media.MediaStorage
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem
import com.kobeinyourpocket.backend.domain.manner.manneritem.repository.MannerRepository
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIcon
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIconUrl
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerKind
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalization
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalizations
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerScope
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.RelatedSpotId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** マナー項目の登録・更新・削除。 */
class MannerItemCommandServiceTest {
    /** メモリ上で振る舞う [MannerRepository]。 */
    private class FakeMannerRepository(
        initial: List<MannerItem> = emptyList(),
    ) : MannerRepository {
        val stored = initial.associateBy { it.id }.toMutableMap()

        override fun save(item: MannerItem): MannerItem {
            stored[item.id] = item
            return item
        }

        override fun findById(id: MannerItem.Id): MannerItem? = stored[id]

        override fun findByIdForUpdate(id: MannerItem.Id): MannerItem? = stored[id]

        override fun existsById(id: MannerItem.Id): Boolean = stored.containsKey(id)

        override fun deleteById(id: MannerItem.Id): Boolean = stored.remove(id) != null
    }

    /**
     * 本番では Spring がトランザクション同期を張る。単体テストでは同期だけ有効化し、
     * コミット / ロールバックの決着は [completeTransaction] で再現する
     * （スポットの `UpdateSpotServiceTest` と同じ形）。
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

    /** 確定・差し戻しとも「呼ばれた」ことだけ分かれば良いので既定は true を返す。 */
    private fun mediaStorage(): MediaStorage =
        mockk<MediaStorage>().also {
            every { it.commit(any()) } returns true
            every { it.release(any()) } returns true
        }

    private fun localizations(
        en: String = "No littering",
        languages: List<Language> = Language.entries,
    ) = MannerLocalizations.of(
        languages.associateWith { language ->
            MannerLocalization(
                title = if (language == Language.EN) en else "${language.code} タイトル",
                description = "${language.code} 説明",
            )
        },
    )

    private fun item(
        id: String,
        iconUrl: MannerIconUrl? = null,
    ) = MannerItem.create(
        id = MannerItem.Id.of(id),
        icon = MannerIcon.of("trash"),
        iconUrl = iconUrl,
        kind = MannerKind.RULE,
        scope = MannerScope.JAPAN,
        localizations = localizations(),
    )

    private fun register(
        repository: MannerRepository,
        en: String = "No littering",
        icon: MannerIcon? = MannerIcon.of("trash"),
        iconUrl: MannerIconUrl? = null,
        relatedSpotIds: List<RelatedSpotId> = emptyList(),
        languages: List<Language> = Language.entries,
        media: MediaStorage = mediaStorage(),
    ) = RegisterMannerItemService(repository, media).registerMannerItem(
        icon = icon,
        iconUrl = iconUrl,
        kind = MannerKind.RULE,
        scope = MannerScope.JAPAN,
        relatedSpotIds = relatedSpotIds,
        localizations = localizations(en, languages),
    )

    @Test
    fun `登録時に英語タイトルから id を生成する`() {
        val repository = FakeMannerRepository()

        val created = register(repository, en = "No littering")

        assertEquals("no-littering", created.id.value)
        assertTrue(repository.existsById(MannerItem.Id.of("no-littering")))
    }

    @Test
    fun `id が既存と衝突する場合は連番を付ける`() {
        val repository = FakeMannerRepository(listOf(item("no-littering")))

        val created = register(repository, en = "No littering")

        assertEquals("no-littering-2", created.id.value)
    }

    @Test
    fun `英語タイトルが記号だけで id を作れない場合は例外`() {
        val repository = FakeMannerRepository()

        assertFailsWith<InvalidMannerTitleException> { register(repository, en = "---") }
        assertTrue(repository.stored.isEmpty())
    }

    @Test
    fun `対応言語が欠けている場合は登録を拒否する`() {
        val repository = FakeMannerRepository()

        assertFailsWith<IncompleteMannerLocalizationsException> {
            register(repository, languages = listOf(Language.EN, Language.JA))
        }
        assertTrue(repository.stored.isEmpty())
    }

    @Test
    fun `アイコン画像だけでも登録できる`() {
        val repository = FakeMannerRepository()

        val created =
            register(
                repository,
                icon = null,
                iconUrl = MannerIconUrl.of("https://example.com/icons/no-littering.png"),
            )

        assertNull(created.icon)
        assertEquals("https://example.com/icons/no-littering.png", created.iconUrl?.value)
    }

    @Test
    fun `アイコンのキーも画像も無い場合は集約が拒否する`() {
        val repository = FakeMannerRepository()

        assertFailsWith<IllegalArgumentException> { register(repository, icon = null, iconUrl = null) }
    }

    @Test
    fun `更新しても id は変わらない`() {
        val repository = FakeMannerRepository(listOf(item("no-littering")))

        val updated =
            UpdateMannerItemService(repository, mediaStorage()).updateMannerItem(
                id = MannerItem.Id.of("no-littering"),
                icon = MannerIcon.of("trash"),
                iconUrl = null,
                kind = MannerKind.MANNER,
                scope = MannerScope.LOCAL,
                relatedSpotIds = listOf(RelatedSpotId.of("mount-rokko")),
                // 英語タイトルを変えても id は追従しない（Client の詳細画面の遷移先のため）
                localizations = localizations(en = "Keep the city clean"),
            )

        assertEquals("no-littering", updated.id.value)
        assertEquals(MannerKind.MANNER, updated.kind)
        assertEquals(listOf("mount-rokko"), updated.relatedSpotIds.map { it.value })
    }

    @Test
    fun `存在しない項目の更新は例外`() {
        val repository = FakeMannerRepository()

        assertFailsWith<MannerItemNotFoundException> {
            UpdateMannerItemService(repository, mediaStorage()).updateMannerItem(
                id = MannerItem.Id.of("missing"),
                icon = MannerIcon.of("trash"),
                iconUrl = null,
                kind = MannerKind.RULE,
                scope = MannerScope.JAPAN,
                relatedSpotIds = emptyList(),
                localizations = localizations(),
            )
        }
    }

    @Test
    fun `対応言語が欠けている更新は拒否する`() {
        val repository = FakeMannerRepository(listOf(item("no-littering")))

        assertFailsWith<IncompleteMannerLocalizationsException> {
            UpdateMannerItemService(repository, mediaStorage()).updateMannerItem(
                id = MannerItem.Id.of("no-littering"),
                icon = MannerIcon.of("trash"),
                iconUrl = null,
                kind = MannerKind.RULE,
                scope = MannerScope.JAPAN,
                relatedSpotIds = emptyList(),
                localizations = localizations(languages = listOf(Language.EN, Language.JA)),
            )
        }
    }

    @Test
    fun `削除すると取得できなくなる`() {
        val repository = FakeMannerRepository(listOf(item("no-littering")))

        DeleteMannerItemService(repository, mediaStorage()).deleteMannerItem(MannerItem.Id.of("no-littering"))

        assertFalse(repository.existsById(MannerItem.Id.of("no-littering")))
    }

    @Test
    fun `存在しない項目の削除は例外`() {
        val repository = FakeMannerRepository()

        assertFailsWith<MannerItemNotFoundException> {
            DeleteMannerItemService(repository, mediaStorage()).deleteMannerItem(MannerItem.Id.of("missing"))
        }
    }

    @Test
    fun `削除したらアイコン画像を staging へ戻す`() {
        // 戻さないと確定済み（タグ無し）のまま残り、ライフサイクル規則の対象外なので永久に消えない
        val media = mediaStorage()
        val repository = FakeMannerRepository(listOf(item("no-littering", iconUrl = ICON_URL)))

        DeleteMannerItemService(repository, media).deleteMannerItem(MannerItem.Id.of("no-littering"))

        // コミット前に戻すと、その後ロールバックしたとき現役の画像を消してしまう。
        verify(exactly = 0) { media.release(any()) }

        commitTransaction()

        verify(exactly = 1) { media.release(ICON_URL.value) }
    }

    @Test
    fun `削除がロールバックしたらアイコン画像を戻さない`() {
        val media = mediaStorage()
        val repository = FakeMannerRepository(listOf(item("no-littering", iconUrl = ICON_URL)))

        DeleteMannerItemService(repository, media).deleteMannerItem(MannerItem.Id.of("no-littering"))
        rollbackTransaction()

        // 項目が残る＝画像も現役のまま。戻すと表示中の画像が消える。
        verify(exactly = 0) { media.release(any()) }
    }

    @Test
    fun `画像を持たない項目の削除では差し戻さない`() {
        val media = mediaStorage()
        val repository = FakeMannerRepository(listOf(item("no-littering")))

        DeleteMannerItemService(repository, media).deleteMannerItem(MannerItem.Id.of("no-littering"))
        commitTransaction()

        verify(exactly = 0) { media.release(any()) }
    }

    @Test
    fun `登録に成功したらアイコン画像を確定する`() {
        // 確定しないと staging のまま期限切れで消え、「登録できたのに画像が翌日消える」状態になる
        val media = mediaStorage()

        register(
            FakeMannerRepository(),
            icon = null,
            iconUrl = MannerIconUrl.of("https://example.com/icon.png"),
            media = media,
        )

        verify(exactly = 1) { media.commit("https://example.com/icon.png") }
        verify(exactly = 0) { media.release(any()) }
    }

    @Test
    fun `保存に失敗したらアイコン画像を staging へ戻す`() {
        val media = mediaStorage()
        val failing =
            object : MannerRepository by FakeMannerRepository() {
                override fun save(item: MannerItem): MannerItem = throw IllegalStateException("boom")
            }

        assertFailsWith<IllegalStateException> {
            register(
                failing,
                icon = null,
                iconUrl = MannerIconUrl.of("https://example.com/icon.png"),
                media = media,
            )
        }

        verify(exactly = 1) { media.release("https://example.com/icon.png") }
    }

    private companion object {
        /** 削除で staging へ戻す対象のアイコン画像。 */
        val ICON_URL = MannerIconUrl.of("https://example.com/icon.png")
    }
}
