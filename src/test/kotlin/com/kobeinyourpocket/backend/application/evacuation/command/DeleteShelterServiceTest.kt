package com.kobeinyourpocket.backend.application.evacuation.command

import com.kobeinyourpocket.backend.application.evacuation.ShelterNotFoundException
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.repository.ShelterRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * 避難所削除ユースケース（#144）。
 *
 * 未登録 ID を黙って成功にしないこと（運営が ID を打ち間違えたまま「消えた」と誤認しない）と、
 * 存在確認に失敗したら削除まで進まないことを押さえる。
 */
class DeleteShelterServiceTest {
    /** 呼び出しを記録するだけの [ShelterRepository]。 */
    private class RecordingShelterRepository(
        private val exists: Boolean,
    ) : ShelterRepository {
        var deletedId: EvacuationShelter.Id? = null

        override fun save(shelter: EvacuationShelter): EvacuationShelter = shelter

        override fun existsById(id: EvacuationShelter.Id): Boolean = exists

        override fun deleteById(id: EvacuationShelter.Id) {
            deletedId = id
        }
    }

    private val id = EvacuationShelter.Id.of("kobe-city-hall")

    @Test
    fun `存在する避難所を削除する`() {
        val repository = RecordingShelterRepository(exists = true)

        DeleteShelterService(repository).execute(id)

        assertEquals(id, repository.deletedId)
    }

    @Test
    fun `未登録なら ShelterNotFoundException を投げる`() {
        val repository = RecordingShelterRepository(exists = false)

        assertFailsWith<ShelterNotFoundException> { DeleteShelterService(repository).execute(id) }
    }

    @Test
    fun `未登録なら削除まで進まない`() {
        val repository = RecordingShelterRepository(exists = false)

        assertFailsWith<ShelterNotFoundException> { DeleteShelterService(repository).execute(id) }

        assertNull(repository.deletedId)
    }
}
