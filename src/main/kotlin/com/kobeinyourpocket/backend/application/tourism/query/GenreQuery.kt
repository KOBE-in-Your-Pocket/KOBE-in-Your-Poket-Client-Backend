package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreCode

/** ジャンルの read port（CQRS-lite / #153）。 */
interface GenreQuery {
    /** 並び順（display_order → code）で全件返す。件数が少ないためページングしない。 */
    fun findAll(): List<GenreView>

    /** 指定ジャンルを参照しているスポットの件数。削除可否の判定に使う。 */
    fun countSpotsByGenre(code: GenreCode): Long
}
