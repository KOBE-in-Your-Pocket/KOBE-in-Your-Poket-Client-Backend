package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.tourism.spot.model.Spot
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotMedia
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotRating
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

/** DB `spot`（言語非依存ベース）。id は slug で採番済みのものを受け取る。 */
@Entity
@Table(name = "spot")
class SpotEntity(
    @Id
    @Column(name = "id")
    var id: String,
    @Column(name = "genre", nullable = false)
    var genre: String,
    @Column(name = "latitude", nullable = false)
    var latitude: Double,
    @Column(name = "longitude", nullable = false)
    var longitude: Double,
    @Column(name = "image_url", nullable = false)
    var imageUrl: String,
    @Column(name = "rating_value")
    var ratingValue: Double? = null,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
) {
    fun toDomainSpot(): Spot =
        Spot(
            id = SpotId.of(id),
            genre = Genre.of(genre),
            coordinates = Coordinates.of(latitude, longitude),
            media = SpotMedia(imageUrl),
            rating = ratingValue?.let { SpotRating(it) },
        )

    companion object {
        fun fromDomain(spot: Spot): SpotEntity =
            SpotEntity(
                id = spot.id.value,
                genre = spot.genre.value,
                latitude = spot.coordinates.latitude,
                longitude = spot.coordinates.longitude,
                imageUrl = spot.media.imageUrl,
                ratingValue = spot.rating?.value,
            )
    }
}
