package com.kobeinyourpocket.backend.infrastructure.persistence.evacuation.entity

import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.DisasterType
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.PetAcceptance
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCoordinates
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterSiting
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterSuitabilities
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterSuitability
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterType
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
    @Column(name = "siting", nullable = false)
    var siting: String,
    @Column(name = "suitability_landslide", nullable = false)
    var suitabilityLandslide: String,
    @Column(name = "suitability_flood", nullable = false)
    var suitabilityFlood: String,
    @Column(name = "suitability_tsunami", nullable = false)
    var suitabilityTsunami: String,
    @Column(name = "suitability_large_fire", nullable = false)
    var suitabilityLargeFire: String,
    @Column(name = "pet_acceptance", nullable = false)
    var petAcceptance: String,
    @Column(name = "phone_number")
    var phoneNumber: String? = null,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
) {
    /**
     * ベース行と localization 行群を [EvacuationShelter] 集約へ復元する。
     * 未知のコードは永続化データ不整合として失敗させる（黙って別の値に寄せる方が危険）。
     */
    fun toDomain(localizations: List<ShelterLocalizationEntity>): EvacuationShelter =
        EvacuationShelter.create(
            id = EvacuationShelter.Id.of(id),
            coordinates = ShelterCoordinates.of(latitude, longitude),
            type = ShelterType.of(type) ?: error("Unknown shelter type in shelter: '$type'"),
            siting = ShelterSiting.of(siting) ?: error("Unknown shelter siting in shelter: '$siting'"),
            suitabilities =
                ShelterSuitabilities.of(
                    landslide = suitability(suitabilityLandslide, "suitability_landslide"),
                    flood = suitability(suitabilityFlood, "suitability_flood"),
                    tsunami = suitability(suitabilityTsunami, "suitability_tsunami"),
                    largeFire = suitability(suitabilityLargeFire, "suitability_large_fire"),
                ),
            petAcceptance =
                PetAcceptance.of(petAcceptance)
                    ?: error("Unknown pet acceptance in shelter: '$petAcceptance'"),
            localizations = localizations.toDomainLocalizations(),
            phoneNumber = phoneNumber,
        )

    private fun suitability(
        value: String,
        column: String,
    ): ShelterSuitability = ShelterSuitability.of(value) ?: error("Unknown suitability in shelter.$column: '$value'")

    companion object {
        fun fromDomain(shelter: EvacuationShelter): ShelterEntity =
            ShelterEntity(
                id = shelter.id.value,
                latitude = shelter.coordinates.latitude,
                longitude = shelter.coordinates.longitude,
                type = shelter.type.wireValue,
                siting = shelter.siting.wireValue,
                suitabilityLandslide = shelter.suitabilities.of(DisasterType.LANDSLIDE).wireValue,
                suitabilityFlood = shelter.suitabilities.of(DisasterType.FLOOD).wireValue,
                suitabilityTsunami = shelter.suitabilities.of(DisasterType.TSUNAMI).wireValue,
                suitabilityLargeFire = shelter.suitabilities.of(DisasterType.LARGE_FIRE).wireValue,
                petAcceptance = shelter.petAcceptance.wireValue,
                phoneNumber = shelter.phoneNumber,
            )
    }
}
