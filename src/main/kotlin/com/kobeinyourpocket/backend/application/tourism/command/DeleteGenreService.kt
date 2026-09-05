package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.application.tourism.GenreInUseException
import com.kobeinyourpocket.backend.application.tourism.GenreNotFoundException
import com.kobeinyourpocket.backend.application.tourism.query.GenreQuery
import com.kobeinyourpocket.backend.domain.tourism.genre.repository.GenreRepository
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * ジャンル削除ユースケース（write / #153）。
 *
 * **使用中は削除させない。** 消せてしまうと、参照していたスポットのジャンルが不明になり、
 * Client のフィルタからも漏れる。DB にも外部キー制約（V15）があるが、そちらに任せると
 * 制約違反が 500 として出るため、ここで件数を見て 409 として返す。
 *
 * 件数確認と削除を同一トランザクションに入れる。別々だと、確認から削除までの間に
 * そのジャンルのスポットが登録された場合を取りこぼす（その場合は DB 制約が最後の砦になる）。
 */
@Service
class DeleteGenreService(
    private val genreRepository: GenreRepository,
    private val genreQuery: GenreQuery,
) {
    @Transactional
    fun deleteGenre(code: GenreCode) {
        if (!genreRepository.existsByCode(code)) throw GenreNotFoundException(code.value)

        val spotCount = genreQuery.countSpotsByGenre(code)
        if (spotCount > 0) throw GenreInUseException(code.value, spotCount)

        genreRepository.deleteByCode(code)
    }
}
