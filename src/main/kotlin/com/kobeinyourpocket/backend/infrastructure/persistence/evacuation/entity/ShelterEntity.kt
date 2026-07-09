package com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.entity

import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCapacity
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCoordinates
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterMedia
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterType
import com.kobeinyourpocket.backend.domain.evacuation.shelterfacilitycategory.model.ShelterFacilityCategory
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

/** DB `shelter`（言語非依存ベース）。id は EvacuationShelter.Id をそのまま受け取る。 */
@Entity
@Table(name = "shelter")
class ShelterEntity(
    @Id
    @Column(name = "id")
    var id: String,
    @Column(name = "latitude", nullable = false)
    var latitude: Double,
    @Column(name = "longitude", nullable = false)
    var longitude: Double,
    @Column(name = "type", nullable = false)
    var type: String,
    @Column(name = "facility_category", nullable = false)
    var facilityCategory: String,
    @Column(name = "image_url", nullable = false)
    var imageUrl: String,
    @Column(name = "accessible", nullable = false)
    var accessible: Boolean,
    @Column(name = "capacity")
    var capacity: Int? = null,
    @Column(name = "external_url")
    var externalUrl: String? = null,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
) {
    /**
     * ベース行と localization 行群を [EvacuationShelter] 集約へ復元する。
     * 未知の type コードは永続化データ不整合として失敗させる。
     */
    fun toDomain(localizations: List<ShelterLocalizationEntity>): EvacuationShelter =
        EvacuationShelter.create(
            id = EvacuationShelter.Id.of(id),
            coordinates = ShelterCoordinates.of(latitude, longitude),
            type = ShelterType.of(type) ?: error("Unknown shelter type in shelter: '$type'"),
            facilityCategory = ShelterFacilityCategory.of(facilityCategory),
            media = ShelterMedia(imageUrl),
            accessible = accessible,
            localizations = localizations.toDomainLocalizations(),
            capacity = capacity?.let { ShelterCapacity(it) },
            externalUrl = externalUrl,
        )

    companion object {
        fun fromDomain(shelter: EvacuationShelter): ShelterEntity =
            ShelterEntity(
                id = shelter.id.value,
                latitude = shelter.coordinates.latitude,
                longitude = shelter.coordinates.longitude,
                type = shelter.type.wireValue,
                facilityCategory = shelter.facilityCategory.wireValue,
                imageUrl = shelter.media.imageUrl,
                accessible = shelter.accessible,
                capacity = shelter.capacity?.value,
                externalUrl = shelter.externalUrl?.toString(),
            )
    }
}
