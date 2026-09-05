package com.kobeinyourpocket.backend.application.tourism.query

import org.springframework.stereotype.Service

/**
 * ジャンル一覧ユースケース（read / #153）。domain 集約を経由せず [GenreQuery] port へ委譲する。
 *
 * ページングしないのは、ジャンルが運営の管理するマスタで件数が限られるため
 * （Client のフィルタに並べる以上、数十件を超えることは想定しない）。
 */
@Service
class ListGenresService(
    private val genreQuery: GenreQuery,
) {
    fun listGenres(): List<GenreView> = genreQuery.findAll()
}
