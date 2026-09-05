package com.kobeinyourpocket.backend.domain.tourism.genre.repository

import com.kobeinyourpocket.backend.domain.tourism.genre.model.Genre
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreCode

/** ジャンルの永続化 port（write / #153）。読み取り専用の一覧は query 側（CQRS-lite）。 */
interface GenreRepository {
    fun findByCode(code: GenreCode): Genre?

    fun existsByCode(code: GenreCode): Boolean

    fun save(genre: Genre): Genre

    fun deleteByCode(code: GenreCode)
}
