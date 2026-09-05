package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.genre.model.Genre
import com.kobeinyourpocket.backend.domain.tourism.genre.repository.GenreRepository
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreCode
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreLocalizations
import org.springframework.stereotype.Service

/**
 * ジャンル登録ユースケース（write / #153）。
 *
 * **code は運営に入力させず、英語ラベルから生成する。** ID の命名は運営の関心事ではなく、
 * 手入力にすると表記ゆれ・typo・重複が運用の負担になる。生成後は変更しない
 * （`spot.genre` が参照するため）。
 */
@Service
class RegisterGenreService(
    private val genreRepository: GenreRepository,
) {
    fun registerGenre(
        displayOrder: Int,
        localizations: GenreLocalizations,
    ): Genre {
        val base =
            GenreCode.fromLabel(localizations.resolve(Language.EN))
                ?: throw InvalidGenreLabelException(localizations.resolve(Language.EN))

        val genre =
            Genre(
                code = resolveUniqueCode(base),
                displayOrder = displayOrder,
                localizations = localizations,
            )
        return genreRepository.save(genre)
    }

    /**
     * 既存と衝突しない code を決める。`night-view` が埋まっていれば `night-view-2`、以降 3, 4…。
     *
     * 衝突を 409 で弾かず採番するのは、運営から見ると「同じ英語名の別ジャンル」を作ること自体は
     * 正当な操作で、ID の衝突は内部事情でしかないため。
     * 上限を設けているのは、想定外の状態で無限ループさせないため。
     */
    private fun resolveUniqueCode(base: GenreCode): GenreCode {
        if (!genreRepository.existsByCode(base)) return base

        for (suffix in 2..MAX_CODE_SUFFIX) {
            val candidate = GenreCode.of("${base.value}-$suffix")
            if (!genreRepository.existsByCode(candidate)) return candidate
        }
        throw InvalidGenreLabelException(base.value)
    }

    private companion object {
        const val MAX_CODE_SUFFIX = 100
    }
}

/**
 * 英語ラベルから code を生成できない。REST 層で 400 に変換する。
 *
 * 英語ラベルが記号のみ（`---` 等）の場合に起こる。ここで既定値を勝手に付けると、
 * 意図しない code が黙って残るため入力を直してもらう。
 */
class InvalidGenreLabelException(
    label: String,
) : RuntimeException("Cannot derive a genre code from the English label: '$label'")
