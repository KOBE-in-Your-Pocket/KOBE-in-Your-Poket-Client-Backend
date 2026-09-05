package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.genre.model.Genre
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreCode
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreLocalizations
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.io.Serializable
import java.time.Instant

/** DB `genre`（言語非依存のマスタ本体）。 */
@Entity
@Table(name = "genre")
class GenreEntity(
    @Id
    @Column(name = "code")
    var code: String,
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
) {
    companion object {
        fun fromDomain(genre: Genre): GenreEntity =
            GenreEntity(
                code = genre.code.value,
                displayOrder = genre.displayOrder,
            )
    }
}

/** `genre_localization` の複合主キー (genre_code, language)。 */
@Embeddable
data class GenreLocalizationId(
    @Column(name = "genre_code")
    val genreCode: String,
    @Column(name = "language")
    val language: String,
) : Serializable

/** DB `genre_localization`（言語別の表示名 1 行）。 */
@Entity
@Table(name = "genre_localization")
class GenreLocalizationEntity(
    @EmbeddedId
    var id: GenreLocalizationId,
    @Column(name = "label", nullable = false)
    var label: String,
) {
    companion object {
        fun fromDomain(
            code: GenreCode,
            language: Language,
            label: String,
        ): GenreLocalizationEntity =
            GenreLocalizationEntity(
                id = GenreLocalizationId(genreCode = code.value, language = language.code),
                label = label,
            )

        /** 行の集合を domain の VO へ戻す。未知の言語コードは無視する。 */
        fun toLocalizations(rows: List<GenreLocalizationEntity>): GenreLocalizations =
            GenreLocalizations.of(
                rows.mapNotNull { row -> Language.of(row.id.language)?.let { it to row.label } }.toMap(),
            )
    }
}
