package com.kobeinyourpocket.backend.domain.evacuation.vo

/**
 * 収容可能人数の Value Object。
 *
 * Client `EvacuationShelter.capacity?` に対応する。`aggregate.EvacuationShelter` 上では通常 `null` 可。
 */
@JvmInline
value class ShelterCapacity(
    val value: Int,
) {
    init {
        require(value > 0) { "capacity must be positive, got $value" }
    }
}
