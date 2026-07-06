package com.kobeinyourpocket.backend.domain.evacuation.vo

/**
 * 災害対策基本法上の避難所区分の Value Object。
 *
 * 区分は API 契約として固定される閉じた集合のため列挙で表現する。
 * Client `ShelterType`（`domain/evacuation-shelter.ts`）に対応する。
 */
enum class ShelterType(
    val wireValue: String,
) {
    EMERGENCY("emergency"),
    DESIGNATED("designated"),
    BOTH("both"),
    ;

    companion object {
        fun of(value: String): ShelterType? = entries.resolveByWireValue(value) { it.wireValue }
    }
}
