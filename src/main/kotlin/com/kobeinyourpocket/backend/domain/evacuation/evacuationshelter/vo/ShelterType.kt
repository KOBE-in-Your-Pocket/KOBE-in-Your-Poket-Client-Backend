package com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo

/**
 * 災害対策基本法上の避難区分（値オブジェクト）。
 *
 * 区分は API 契約として固定される閉じた集合のため列挙で表現する。
 * [wireValue] は Client `ShelterType`（`domain/evacuation-shelter.ts`）の slug と一致させ、
 * 災害対策基本法の用語（指定緊急避難場所 / 指定避難所 / 兼用）に沿う。
 *
 */
enum class ShelterType(
    val wireValue: String,
) {
    /** 指定緊急避難場所 — 危険が切迫した際に命を守るため緊急避難する場所。 */
    DESIGNATED_EMERGENCY_EVACUATION_SITE("designated-emergency-evacuation-site"),

    /** 指定避難所 — 避難生活を送るため市町村が指定する施設。 */
    DESIGNATED_EVACUATION_SHELTER("designated-evacuation-shelter"),

    /** 兼用 — 指定緊急避難場所かつ指定避難所。 */
    DUAL_USE("dual-use"),
    ;

    companion object {
        /** wireValue から解決する。未対応・空は `null`。 */
        fun of(value: String): ShelterType? {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.wireValue == normalized }
        }
    }
}
