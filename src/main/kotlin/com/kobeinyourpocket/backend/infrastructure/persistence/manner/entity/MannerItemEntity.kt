package com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity

import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIcon
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerKind
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerScope
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

/** DB `manner_item`（言語非依存ベース）。id は slug で採番済みのものを受け取る。 */
@Entity
@Table(name = "manner_item")
class MannerItemEntity(
    @Id
    @Column(name = "id")
    var id: String,
    @Column(name = "icon", nullable = false)
    var icon: String,
    @Column(name = "kind", nullable = false)
    var kind: String,
    @Column(name = "scope", nullable = false)
    var scope: String,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
) {
    /**
     * ベース行と同一項目の各行を [MannerItem] 集約へ復元する。
     * 未知の kind / scope コードは永続化データ不整合として失敗させる。
     */
    fun toDomain(
        localizations: List<MannerItemLocalizationEntity>,
        relatedSpots: List<MannerItemSpotEntity>,
    ): MannerItem =
        MannerItem.create(
            id = MannerItem.Id.of(id),
            icon = MannerIcon.of(icon),
            kind = MannerKind.of(kind),
            scope = MannerScope.of(scope),
            localizations = localizations.toDomainLocalizations(),
            relatedSpotIds = relatedSpots.map { it.toRelatedSpotId() },
        )

    companion object {
        fun fromDomain(item: MannerItem): MannerItemEntity =
            MannerItemEntity(
                id = item.id.value,
                icon = item.icon.value,
                kind = item.kind.code,
                scope = item.scope.code,
            )
    }
}
