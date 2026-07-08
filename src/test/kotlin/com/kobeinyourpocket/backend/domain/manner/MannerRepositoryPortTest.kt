package com.kobeinyourpocket.backend.domain.manner

import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem
import com.kobeinyourpocket.backend.domain.manner.manneritem.repository.MannerRepository
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIcon
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerKind
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalization
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalizations
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerScope
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.RelatedSpotId
import com.kobeinyourpocket.backend.domain.tourism.localization.Language
import kotlin.test.Test
import kotlin.test.assertEquals

/** MannerRepository write port の契約を Fake で検証する。 */
class MannerRepositoryPortTest {
    private class FakeMannerRepository : MannerRepository {
        private val store = linkedMapOf<MannerItem.Id, MannerItem>()

        override fun save(item: MannerItem): MannerItem {
            store[item.id] = item
            return item
        }

        fun get(id: MannerItem.Id): MannerItem? = store[id]
    }

    @Test
    fun `save した集約を取得できる`() {
        val repository = FakeMannerRepository()
        val item =
            MannerItem.create(
                id = MannerItem.Id.of("no-photo-in-shrine"),
                icon = MannerIcon.of("camera-off"),
                kind = MannerKind.MANNER,
                scope = MannerScope.LOCAL,
                localizations =
                    MannerLocalizations.of(
                        mapOf(
                            Language.JA to MannerLocalization("撮影禁止", "境内での撮影は控えましょう。"),
                            Language.EN to MannerLocalization("No photography", "Please refrain from taking photos."),
                            Language.KO to MannerLocalization("촬영 금지", "경내에서는 촬영을 삼가 주세요."),
                            Language.ZH to MannerLocalization("禁止拍照", "请勿在境内拍照。"),
                        ),
                    ),
                relatedSpotIds = listOf(RelatedSpotId.of("kobe-port-tower")),
            )

        repository.save(item)

        assertEquals(item, repository.get(MannerItem.Id.of("no-photo-in-shrine")))
    }
}
