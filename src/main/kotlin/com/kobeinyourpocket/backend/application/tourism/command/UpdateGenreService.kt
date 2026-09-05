package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.tourism.GenreNotFoundException
import com.kobeinyourpocket.backend.domain.tourism.genre.model.Genre
import com.kobeinyourpocket.backend.domain.tourism.genre.repository.GenreRepository
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreCode
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreLocalizations
import org.springframework.stereotype.Service

/**
 * ジャンル更新ユースケース（write / #153）。
 *
 * 変更できるのは表示名と並び順のみ。**code は変更しない**（`spot.genre` が参照しており、
 * 変えると既存スポットの参照先が消える）。名前を大きく変えたい場合は、新しいジャンルを
 * 作ってスポットを付け替え、旧ジャンルを削除する運用になる。
 */
@Service
class UpdateGenreService(
    private val genreRepository: GenreRepository,
) {
    fun updateGenre(
        code: GenreCode,
        displayOrder: Int,
        localizations: GenreLocalizations,
    ): Genre {
        val current = genreRepository.findByCode(code) ?: throw GenreNotFoundException(code.value)
        return genreRepository.save(current.update(displayOrder = displayOrder, localizations = localizations))
    }
}
