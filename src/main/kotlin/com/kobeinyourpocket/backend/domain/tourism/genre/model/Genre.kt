package com.kobeinyourpocket.backend.domain.tourism.genre.model

import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreCode
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreLocalizations

/**
 * [集約] スポットのジャンル区分（#153）。
 *
 * `spot.genre` が参照するマスタ。表示名を持たせることで、運営が追加したジャンルも
 * ADMIN・Client の双方で正しく表示できるようにする。
 *
 * [code] は不変。更新できるのは表示名と並び順だけで、[rename] のような操作は用意しない。
 */
data class Genre(
    val code: GenreCode,
    /** Client のジャンルフィルタの並び順。小さいほど前。 */
    val displayOrder: Int,
    val localizations: GenreLocalizations,
) {
    init {
        require(displayOrder >= 0) { "Genre displayOrder must be zero or positive" }
    }

    /** 表示名・並び順を差し替えた新しい状態を返す。code は変えない。 */
    fun update(
        displayOrder: Int,
        localizations: GenreLocalizations,
    ): Genre = copy(displayOrder = displayOrder, localizations = localizations)
}
