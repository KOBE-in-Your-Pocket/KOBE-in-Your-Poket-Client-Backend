package com.kobeinyourpocket.backend.domain.manner.manneritem.vo

/**
 * 関連スポットへの ID 参照（値オブジェクト）。
 *
 * Tourism の `Spot.id` を指す参照のみの値（要件定義 M-2: JOIN 配信せず ID 参照に留める）。
 * この BC から見れば他集約の同一性は「ただの値」であり、Tourism の `SpotId` 型には依存せず manner 側で定義する。
 * 文字列としての同一性のみを扱い、実在検証は行わない。[Companion.of] が生成入口。
 */
@JvmInline
value class RelatedSpotId private constructor(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "RelatedSpotId must not be blank" }
    }

    override fun toString(): String = value

    companion object {
        fun of(value: String): RelatedSpotId = RelatedSpotId(value.trim())
    }
}
