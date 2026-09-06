package com.kobeinyourpocket.backend.application.evacuation.command

import com.kobeinyourpocket.backend.application.media.MediaStorage
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.repository.ShelterRepository
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCoordinates
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalization
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalizations
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterMedia
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterType
import com.kobeinyourpocket.backend.domain.evacuation.shelterfacilitycategory.model.ShelterFacilityCategory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 避難所登録ユースケース。
 *
 * 全対応言語必須のポリシー（避難所は防災情報のため英語フォールバックに逃がさない）と、
 * 画像の確定・巻き戻しが登録の成否と一致することを押さえる。
 */
class RegisterShelterServiceTest {
    private class RecordingShelterRepository(
        private val failOnSave: Boolean = false,
    ) : ShelterRepository {
        var saved: EvacuationShelter? = null

        override fun save(shelter: EvacuationShelter): EvacuationShelter {
            if (failOnSave) throw IllegalStateException("save failed")
            saved = shelter
            return shelter
        }

        override fun existsById(id: EvacuationShelter.Id): Boolean = saved?.id == id

        override fun deleteById(id: EvacuationShelter.Id) {
            saved = null
        }
    }

    private class RecordingMediaStorage : MediaStorage {
        val committed = mutableListOf<String>()
        val released = mutableListOf<String>()

        override fun store(
            key: String,
            bytes: ByteArray,
            contentType: String,
        ): String = "https://example.com/$key"

        override fun commit(imageUrl: String): Boolean = committed.add(imageUrl)

        override fun release(imageUrl: String): Boolean = released.add(imageUrl)
    }

    private val media = ShelterMedia("https://example.com/uploads/shelter.webp")

    private fun localizations(vararg languages: Language): ShelterLocalizations =
        ShelterLocalizations.of(languages.associateWith { ShelterLocalization("名前", "住所") })

    private val allLanguages = localizations(*Language.entries.toTypedArray())

    private fun register(
        repository: ShelterRepository,
        storage: MediaStorage,
        localizations: ShelterLocalizations = allLanguages,
    ): EvacuationShelter =
        RegisterShelterService(repository, storage).registerShelter(
            coordinates = ShelterCoordinates.of(34.6826, 135.1863),
            type = ShelterType.DUAL_USE,
            facilityCategory = ShelterFacilityCategory.GOVERNMENT,
            media = media,
            accessible = true,
            localizations = localizations,
        )

    @Test
    fun `全言語そろっていれば登録できる`() {
        val repository = RecordingShelterRepository()

        val saved = register(repository, RecordingMediaStorage())

        assertEquals(saved, repository.saved)
        assertEquals(ShelterType.DUAL_USE, saved.type)
        assertEquals(Language.entries.toSet(), saved.localizations.languages)
    }

    @Test
    fun `id は採番される`() {
        val repository = RecordingShelterRepository()

        val first = register(repository, RecordingMediaStorage())
        val second = register(RecordingShelterRepository(), RecordingMediaStorage())

        assertTrue(first.id.value.isNotBlank())
        assertTrue(first.id != second.id, "登録ごとに異なる id が振られること")
    }

    @Test
    fun `対応言語が欠けていたら登録できない`() {
        // en は ShelterLocalizations の不変条件なので、それ以外を欠けさせて検証する
        val missingKorean = localizations(Language.JA, Language.EN, Language.ZH)

        val error =
            assertFailsWith<IllegalArgumentException> {
                register(RecordingShelterRepository(), RecordingMediaStorage(), missingKorean)
            }

        assertContains(error.message.orEmpty(), "localizations must contain exactly the supported languages")
    }

    @Test
    fun `対応言語が欠けていたら保存も画像確定も行わない`() {
        val repository = RecordingShelterRepository()
        val storage = RecordingMediaStorage()

        assertFailsWith<IllegalArgumentException> {
            register(repository, storage, localizations(Language.EN))
        }

        assertNull(repository.saved)
        assertTrue(storage.committed.isEmpty(), "検証で落ちた時点で画像を確定しないこと")
    }

    @Test
    fun `登録に成功したら画像を確定する`() {
        val storage = RecordingMediaStorage()

        register(RecordingShelterRepository(), storage)

        assertEquals(listOf(media.imageUrl), storage.committed)
        assertTrue(storage.released.isEmpty())
    }

    @Test
    fun `保存に失敗したら画像を staging へ戻す`() {
        val storage = RecordingMediaStorage()

        assertFailsWith<IllegalStateException> {
            register(RecordingShelterRepository(failOnSave = true), storage)
        }

        assertEquals(listOf(media.imageUrl), storage.committed)
        assertEquals(listOf(media.imageUrl), storage.released, "確定した画像を差し戻すこと")
    }
}
