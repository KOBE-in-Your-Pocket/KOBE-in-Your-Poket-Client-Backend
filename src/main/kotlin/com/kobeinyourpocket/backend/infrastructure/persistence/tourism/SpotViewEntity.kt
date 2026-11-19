package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.view.model.SpotView
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** DB `spot_views`（V26 / #239）。閲覧 1 回 = 1 行。 */
@Entity
@Table(name = "spot_views")
class SpotViewEntity(
    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "spot_id", nullable = false, length = 128, updatable = false)
    var spotId: String,
    @Column(name = "viewed_at", nullable = false, updatable = false)
    var viewedAt: Instant,
) {
    fun toDomain(): SpotView = SpotView(spotId = SpotId.of(spotId), viewedAt = viewedAt)

    companion object {
        /** 閲覧 1 回ごとに新しい行にするため、識別子はここで採番する（ドメインは同一性を持たない）。 */
        fun fromDomain(view: SpotView): SpotViewEntity =
            SpotViewEntity(id = UUID.randomUUID(), spotId = view.spotId.value, viewedAt = view.viewedAt)
    }
}
