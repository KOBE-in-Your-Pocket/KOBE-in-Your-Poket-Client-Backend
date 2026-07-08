package com.kobeinyourpocket.backend.domain.evacuation.shelterfacilitycategory.model

/**
 * 施設種別（ドメインモデル）。
 *
 * 一覧カードのカテゴリ表示用。運営側で追加・拡張されうるため VO ではなく独立モデルとして定義する。
 * Client `ShelterFacilityCategory`（`domain/evacuation-shelter.ts`）の既知 code は [Companion] 定数として提供し、
 * 任意の code は [Companion.of] を入口に生成する。将来マスタ管理集約へ昇格可能。
 */
data class ShelterFacilityCategory(
    val code: Code,
) {
    /** API / DB 永続化用の code 文字列。 */
    val wireValue: String
        get() = code.value

    /**
     * 施設種別 code（識別子）。
     *
     * カテゴリの同一性そのもの。将来マスタ集約へ昇格した際も code を軸に参照する。
     */
    @JvmInline
    value class Code private constructor(
        val value: String,
    ) {
        init {
            require(value.isNotBlank()) { "ShelterFacilityCategory.Code must not be blank" }
        }

        override fun toString(): String = value

        companion object {
            fun of(raw: String): Code = Code(raw.trim().lowercase())
        }
    }

    companion object {
        val GOVERNMENT = of("government")
        val SCHOOL = of("school")
        val PARK = of("park")
        val GYMNASIUM = of("gymnasium")

        fun of(code: String): ShelterFacilityCategory = ShelterFacilityCategory(Code.of(code))
    }
}
