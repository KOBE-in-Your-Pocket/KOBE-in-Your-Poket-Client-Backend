package com.kobeinyourpocket.backend.domain.manner.manneritem.vo

/**
 * [値オブジェクト] 関連スポットへの ID 参照。
 *
 * Tourism の `Spot.id` を指す**参照のみ**の値（要件定義 M-2: JOIN 配信せず ID 参照に留める）。
 * bounded context を跨いだ型依存を避けるため、Tourism の `SpotId` 型は import せず manner 側で定義する。
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
