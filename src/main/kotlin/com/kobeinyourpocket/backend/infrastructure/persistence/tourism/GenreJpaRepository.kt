package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import org.springframework.data.jpa.repository.JpaRepository

interface GenreJpaRepository : JpaRepository<GenreEntity, String>

interface GenreLocalizationJpaRepository : JpaRepository<GenreLocalizationEntity, GenreLocalizationId> {
    fun findByIdGenreCode(genreCode: String): List<GenreLocalizationEntity>

    fun deleteByIdGenreCode(genreCode: String)
}
