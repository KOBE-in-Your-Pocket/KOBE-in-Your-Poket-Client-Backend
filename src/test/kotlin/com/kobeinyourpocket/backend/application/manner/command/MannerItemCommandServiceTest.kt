package com.kobeinyourpocket.backend.application.manner.command

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

        override fun existsById(id: MannerItem.Id): Boolean = stored.containsKey(id)

        override fun deleteById(id: MannerItem.Id) {
            stored.remove(id)
        }
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

    private fun item(id: String) =
        MannerItem.create(
            id = MannerItem.Id.of(id),
            icon = MannerIcon.of("trash"),
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
    ) = RegisterMannerItemService(repository).registerMannerItem(
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
            UpdateMannerItemService(repository).updateMannerItem(
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
            UpdateMannerItemService(repository).updateMannerItem(
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
            UpdateMannerItemService(repository).updateMannerItem(
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

        DeleteMannerItemService(repository).deleteMannerItem(MannerItem.Id.of("no-littering"))

        assertFalse(repository.existsById(MannerItem.Id.of("no-littering")))
    }

    @Test
    fun `存在しない項目の削除は例外`() {
        val repository = FakeMannerRepository()

        assertFailsWith<MannerItemNotFoundException> {
            DeleteMannerItemService(repository).deleteMannerItem(MannerItem.Id.of("missing"))
        }
    }
}
