package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.view.model.SpotView
import com.kobeinyourpocket.backend.domain.tourism.view.repository.SpotViewRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/** 閲覧の記録（#239）。閲覧 1 回ごとに 1 行増えることを実 DB で確かめる。 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(SpotViewRepositoryImpl::class)
class SpotViewRepositoryImplTest {
    @Autowired
    private lateinit var repository: SpotViewRepository

    @Autowired
    private lateinit var spotViewJpa: SpotViewJpaRepository

    private val harborland = SpotId.of("kobe-harborland")
    private val kitano = SpotId.of("kitano-ijinkan")
    private val firstAt = Instant.parse("2026-11-01T10:00:00Z")
    private val laterAt = Instant.parse("2026-11-02T10:00:00Z")

    private fun stored(): List<SpotView> = spotViewJpa.findAll().map { it.toDomain() }.sortedBy { it.viewedAt }

    @Test
    fun `記録した閲覧を同じ内容で復元できる`() {
        repository.record(SpotView.create(spotId = harborland, viewedAt = firstAt))

        assertEquals(listOf(SpotView(spotId = harborland, viewedAt = firstAt)), stored())
    }

    @Test
    fun `同じスポットの閲覧も毎回 1 件として追加する`() {
        repository.record(SpotView.create(spotId = harborland, viewedAt = firstAt))
        repository.record(SpotView.create(spotId = harborland, viewedAt = laterAt))

        assertEquals(
            listOf(SpotView(spotId = harborland, viewedAt = firstAt), SpotView(spotId = harborland, viewedAt = laterAt)),
            stored(),
        )
    }

    @Test
    fun `同じ時刻に同じスポットを閲覧しても別々の 1 件として残る`() {
        repository.record(SpotView.create(spotId = harborland, viewedAt = firstAt))
        repository.record(SpotView.create(spotId = harborland, viewedAt = firstAt))

        assertEquals(2, spotViewJpa.count())
    }

    @Test
    fun `別のスポットの閲覧も記録できる`() {
        repository.record(SpotView.create(spotId = harborland, viewedAt = firstAt))
        repository.record(SpotView.create(spotId = kitano, viewedAt = laterAt))

        assertEquals(listOf(harborland, kitano), stored().map { it.spotId })
    }
}
