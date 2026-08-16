package com.kobeinyourpocket.backend.application.evacuation

import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter

/** 指定 [EvacuationShelter.Id] の避難所が存在しない場合の例外（REST では 404 / #144）。 */
class ShelterNotFoundException(
    id: EvacuationShelter.Id,
) : RuntimeException("Shelter not found: ${id.value}")
