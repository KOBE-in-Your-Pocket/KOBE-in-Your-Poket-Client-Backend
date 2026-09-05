package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.tourism.genre.model.Genre
import com.kobeinyourpocket.backend.domain.tourism.genre.repository.GenreRepository
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreCode
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/** [GenreRepository] port の outbound adapter（write のみ）。 */
@Repository
class GenreRepositoryImpl(
    private val genreJpa: GenreJpaRepository,
    private val localizationJpa: GenreLocalizationJpaRepository,
) : GenreRepository {
    @Transactional(readOnly = true)
    override fun findByCode(code: GenreCode): Genre? {
        val entity = genreJpa.findById(code.value).orElse(null) ?: return null
        val rows = localizationJpa.findByIdGenreCode(code.value)
        return Genre(
            code = code,
            displayOrder = entity.displayOrder,
            localizations = GenreLocalizationEntity.toLocalizations(rows),
        )
    }

    override fun existsByCode(code: GenreCode): Boolean = genreJpa.existsById(code.value)

    /**
     * 表示名は毎回入れ替える。更新で言語が減ることは無い（全言語必須）が、
     * 差分更新にすると「消えたはずの行が残る」経路を作るため、単純な置き換えにしている。
     */
    @Transactional
    override fun save(genre: Genre): Genre {
        genreJpa.save(GenreEntity.fromDomain(genre))
        localizationJpa.deleteByIdGenreCode(genre.code.value)
        localizationJpa.flush()
        localizationJpa.saveAll(
            genre.localizations.byLanguage.map { (language, label) ->
                GenreLocalizationEntity.fromDomain(genre.code, language, label)
            },
        )
        return genre
    }

    /** `genre_localization` は `ON DELETE CASCADE`（V13）で DB 側が連動削除する。 */
    @Transactional
    override fun deleteByCode(code: GenreCode) {
        genreJpa.deleteById(code.value)
    }
}
