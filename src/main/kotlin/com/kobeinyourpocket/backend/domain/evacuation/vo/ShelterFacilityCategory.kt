package com.kobeinyourpocket.backend.domain.evacuation.vo

/**
 * 施設種別の Value Object（一覧カードのカテゴリ表示用）。
 *
 * 種別は API 契約として固定される閉じた集合のため列挙で表現する。
 * Client `ShelterFacilityCategory`（`domain/evacuation-shelter.ts`）に対応する。
 */
enum class ShelterFacilityCategory(
    val wireValue: String,
) {
    GOVERNMENT("government"),
    SCHOOL("school"),
    PARK("park"),
    GYMNASIUM("gymnasium"),
    ;

    companion object {
        fun of(value: String): ShelterFacilityCategory? {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.wireValue == normalized }
        }
    }
}
