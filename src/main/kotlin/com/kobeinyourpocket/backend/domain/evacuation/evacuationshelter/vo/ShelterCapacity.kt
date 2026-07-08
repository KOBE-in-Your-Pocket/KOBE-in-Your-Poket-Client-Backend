package com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo

/**
 * 収容可能人数（値オブジェクト）。
 *
 * Client `EvacuationShelter.capacity?` に対応する。集約上では通常 `null` 可。
 */
@JvmInline
value class ShelterCapacity(
    val value: Int,
) {
    init {
        require(value > 0) { "capacity must be positive, got $value" }
    }
}
