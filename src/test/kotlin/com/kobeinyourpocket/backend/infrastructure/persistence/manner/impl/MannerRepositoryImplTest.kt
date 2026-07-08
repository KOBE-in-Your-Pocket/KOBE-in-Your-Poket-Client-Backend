package com.kobeinyourpocket.backend.infrastructure.persistence.manner.impl

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem
import com.kobeinyourpocket.backend.domain.manner.manneritem.repository.MannerRepository
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIcon
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerKind
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalization
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalizations
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerScope
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.RelatedSpotId
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.repository.MannerItemJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.repository.MannerItemLocalizationJpaRepository
import com.kobeinyourpocket.backend.infrastructure.persistence.manner.repository.MannerItemSpotJpaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test
import kotlin.test.assertEquals

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(MannerRepositoryImpl::class)
class MannerRepositoryImplTest {
    @Autowired
    private lateinit var repository: MannerRepository

    @Autowired
    private lateinit var itemJpa: MannerItemJpaRepository

    @Autowired
    private lateinit var localizationJpa: MannerItemLocalizationJpaRepository

    @Autowired
    private lateinit var spotJpa: MannerItemSpotJpaRepository

    private fun noPhotoInShrine(vararg relatedSpotIds: String) =
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
                    ),
                ),
            relatedSpotIds = relatedSpotIds.map { RelatedSpotId.of(it) },
        )

    @Test
    fun `save で item・localization・related spot が永続化される`() {
        repository.save(noPhotoInShrine("kobe-port-tower", "mount-rokko"))

        val entity = itemJpa.findById("no-photo-in-shrine").orElseThrow()
        assertEquals("camera-off", entity.icon)
        assertEquals("manner", entity.kind)
        assertEquals("local", entity.scope)

        val localizations = localizationJpa.findAll().filter { it.id.mannerItemId == "no-photo-in-shrine" }
        assertEquals(setOf("ja", "en"), localizations.map { it.id.language }.toSet())

        val spots = spotJpa.findAll().filter { it.id.mannerItemId == "no-photo-in-shrine" }
        assertEquals(setOf("kobe-port-tower", "mount-rokko"), spots.map { it.id.spotId }.toSet())
    }

    @Test
    fun `再 save で related spot の削除が反映される（delete-then-insert）`() {
        repository.save(noPhotoInShrine("kobe-port-tower", "mount-rokko"))
        repository.save(noPhotoInShrine("kobe-port-tower"))

        val spots = spotJpa.findAll().filter { it.id.mannerItemId == "no-photo-in-shrine" }
        assertEquals(setOf("kobe-port-tower"), spots.map { it.id.spotId }.toSet())
    }
}
