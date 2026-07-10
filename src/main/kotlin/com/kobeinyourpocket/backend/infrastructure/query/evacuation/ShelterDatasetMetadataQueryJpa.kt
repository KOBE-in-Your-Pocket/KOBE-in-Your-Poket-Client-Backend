package com.kobeinyourpocket.backend.infrastructure.query.evacuation

import com.kobeinyourpocket.backend.application.evacuation.query.ShelterDatasetMetadataQuery
import com.kobeinyourpocket.backend.application.evacuation.query.ShelterDatasetMetadataView
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

/** [ShelterDatasetMetadataQuery] の JPA 実装。シングルトン行（id=1）を 1 件取得する（#85）。 */
@Repository
class ShelterDatasetMetadataQueryJpa(
    private val entityManager: EntityManager,
) : ShelterDatasetMetadataQuery {
    override fun get(): ShelterDatasetMetadataView =
        entityManager
            .createNativeQuery(SELECT_METADATA)
            .setParameter("id", SINGLETON_ID)
            .resultRow()
            .let(::toView)

    @Suppress("UNCHECKED_CAST")
    private fun Query.resultRow(): Array<Any?> = (resultList as List<Array<Any?>>).single()

    private fun toView(row: Array<Any?>): ShelterDatasetMetadataView =
        ShelterDatasetMetadataView(
            source = row[Column.SOURCE] as String,
            asOf = toLocalDate(row[Column.AS_OF]),
            updatedAt = toInstant(row[Column.UPDATED_AT]),
        )

    private fun toLocalDate(value: Any?): LocalDate =
        when (value) {
            is LocalDate -> value
            is java.sql.Date -> value.toLocalDate()
            else -> error("Unsupported as_of type: ${value?.let { it::class }}")
        }

    private fun toInstant(value: Any?): Instant =
        when (value) {
            is OffsetDateTime -> value.toInstant()
            is Timestamp -> value.toInstant()
            else -> (value as java.util.Date).toInstant()
        }

    /** [SELECT_METADATA] の列順と対応する index。列の並び替え時は両方を合わせて更新すること。 */
    private object Column {
        const val SOURCE = 0
        const val AS_OF = 1
        const val UPDATED_AT = 2
    }

    private companion object {
        const val SINGLETON_ID: Short = 1
        val SELECT_METADATA =
            """
            SELECT source, as_of, updated_at
            FROM shelter_dataset_metadata
            WHERE id = :id
            """.trimIndent()
    }
}
