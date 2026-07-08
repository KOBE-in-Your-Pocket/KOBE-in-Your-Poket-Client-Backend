package com.kobeinyourpocket.backend.domain.manner

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIcon
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerKind
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalization
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalizations
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerScope
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.RelatedSpotId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private fun localizationsOf(vararg languages: Language) =
    MannerLocalizations.of(languages.associateWith { MannerLocalization("title-${it.code}", "desc-${it.code}") })

class MannerItemIdTest {
    @Test
    fun `前後空白は trim される`() {
        assertEquals("no-photo-in-shrine", MannerItem.Id.of("  no-photo-in-shrine  ").value)
    }

    @Test
    fun `空文字は拒否する`() {
        assertFailsWith<IllegalArgumentException> { MannerItem.Id.of("   ") }
    }
}

class MannerKindTest {
    @Test
    fun `Client と同じリテラルから解決できる`() {
        assertEquals(MannerKind.MANNER, MannerKind.of("manner"))
        assertEquals(MannerKind.RULE, MannerKind.of("rule"))
    }

    @Test
    fun `前後空白と大文字を正規化する`() {
        assertEquals(MannerKind.RULE, MannerKind.of("  RULE  "))
    }

    @Test
    fun `未対応コードは拒否する`() {
        assertFailsWith<IllegalArgumentException> { MannerKind.of("suggestion") }
    }
}

class MannerScopeTest {
    @Test
    fun `Client と同じリテラルから解決できる（kobe ではなく local）`() {
        assertEquals(MannerScope.LOCAL, MannerScope.of("local"))
        assertEquals(MannerScope.JAPAN, MannerScope.of("japan"))
    }

    @Test
    fun `未対応コードは拒否する`() {
        assertFailsWith<IllegalArgumentException> { MannerScope.of("kobe") }
    }
}

class MannerIconTest {
    @Test
    fun `アイコンキーを保持し trim する`() {
        assertEquals("camera-off", MannerIcon.of("  camera-off  ").value)
    }

    @Test
    fun `空文字は拒否する`() {
        assertFailsWith<IllegalArgumentException> { MannerIcon.of("  ") }
    }
}

class RelatedSpotIdTest {
    @Test
    fun `参照する Spot の id を保持する`() {
        assertEquals("kobe-port-tower", RelatedSpotId.of("kobe-port-tower").value)
    }

    @Test
    fun `空文字は拒否する`() {
        assertFailsWith<IllegalArgumentException> { RelatedSpotId.of("") }
    }
}

class MannerLocalizationTest {
    @Test
    fun `title と description を保持する`() {
        val localization = MannerLocalization(title = "撮影禁止", description = "境内での撮影は控えましょう。")

        assertEquals("撮影禁止", localization.title)
        assertEquals("境内での撮影は控えましょう。", localization.description)
    }

    @Test
    fun `title が空なら拒否する`() {
        assertFailsWith<IllegalArgumentException> { MannerLocalization(title = "  ", description = "x") }
    }

    @Test
    fun `description が空なら拒否する`() {
        assertFailsWith<IllegalArgumentException> { MannerLocalization(title = "撮影禁止", description = "  ") }
    }
}

class MannerLocalizationsTest {
    @Test
    fun `要求言語のローカライズを返す`() {
        val localizations = localizationsOf(Language.JA, Language.EN)

        assertEquals("title-ja", localizations.resolve(Language.JA).title)
    }

    @Test
    fun `要求言語が無ければ en へフォールバックする`() {
        val localizations = localizationsOf(Language.EN)

        assertEquals("title-en", localizations.resolve(Language.KO).title)
    }

    @Test
    fun `フォールバック先は en`() {
        assertEquals(Language.EN, MannerLocalizations.FALLBACK)
    }

    @Test
    fun `フォールバック言語 en を含まない場合は拒否する`() {
        assertFailsWith<IllegalArgumentException> {
            localizationsOf(Language.JA)
        }
    }
}

class MannerItemTest {
    @Test
    fun `集約ルートを生成し localizations を所有する`() {
        val item =
            MannerItem.create(
                id = MannerItem.Id.of("no-photo-in-shrine"),
                icon = MannerIcon.of("camera-off"),
                kind = MannerKind.MANNER,
                scope = MannerScope.LOCAL,
                localizations = localizationsOf(Language.JA, Language.EN),
                relatedSpotIds = listOf(RelatedSpotId.of("kobe-port-tower")),
            )

        assertEquals("no-photo-in-shrine", item.id.value)
        assertEquals(MannerKind.MANNER, item.kind)
        assertEquals("title-ja", item.localizations.resolve(Language.JA).title)
    }

    @Test
    fun `relatedSpotIds は省略時に空`() {
        val item =
            MannerItem.create(
                id = MannerItem.Id.of("no-littering"),
                icon = MannerIcon.of("trash"),
                kind = MannerKind.RULE,
                scope = MannerScope.JAPAN,
                localizations = localizationsOf(Language.EN),
            )

        assertTrue(item.relatedSpotIds.isEmpty())
    }

    @Test
    fun `relatedSpotIds は防御的にコピーされ元リストの変更に影響されない`() {
        val source = mutableListOf(RelatedSpotId.of("kobe-port-tower"))
        val item =
            MannerItem.create(
                id = MannerItem.Id.of("no-photo-in-shrine"),
                icon = MannerIcon.of("camera-off"),
                kind = MannerKind.MANNER,
                scope = MannerScope.LOCAL,
                localizations = localizationsOf(Language.EN),
                relatedSpotIds = source,
            )

        source.add(RelatedSpotId.of("mount-rokko"))

        assertEquals(1, item.relatedSpotIds.size)
    }
}
