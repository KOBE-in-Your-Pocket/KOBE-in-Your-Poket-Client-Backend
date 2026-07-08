package com.kobeinyourpocket.backend.infrastructure.persistence.manner.entity

import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.RelatedSpotId
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable

/** `manner_item_spot` の複合主キー (manner_item_id, spot_id)。関連づけそのものが同一性のため PK に含める。 */
@Embeddable
data class MannerItemSpotId(
    @Column(name = "manner_item_id")
    val mannerItemId: String,
    @Column(name = "spot_id")
    val spotId: String,
) : Serializable

/** DB `manner_item_spot`（relatedSpotIds の 1 参照）。M-2 により spot への FK は張らない ID 参照のみ。 */
@Entity
@Table(name = "manner_item_spot")
class MannerItemSpotEntity(
    @EmbeddedId
    var id: MannerItemSpotId,
) {
    fun toRelatedSpotId(): RelatedSpotId = RelatedSpotId.of(id.spotId)

    companion object {
        fun fromDomain(
            mannerItemId: MannerItem.Id,
            relatedSpotId: RelatedSpotId,
        ): MannerItemSpotEntity =
            MannerItemSpotEntity(
                id = MannerItemSpotId(mannerItemId = mannerItemId.value, spotId = relatedSpotId.value),
            )
    }
}
