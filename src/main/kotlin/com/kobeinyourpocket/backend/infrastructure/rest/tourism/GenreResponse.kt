package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.query.GenreView
import com.kobeinyourpocket.backend.domain.tourism.genre.model.Genre

/**
 * `GET/POST/PUT /api/v1/tourism/genres` のレスポンス（#153）。
 *
 * 表示名は**全言語まとめて**返す。ADMIN の編集フォームが全言語を必要とし、件数も少ないため、
 * `?lang=` で 1 言語に解決する形にすると編集のたびに 4 回取得することになる
 * （スポット詳細が実際そうなっている）。Client は `labels[表示言語]` を使う。
 */
data class GenreResponse(
    /** `spot.genre` と突き合わせる識別子。英語ラベルから自動生成され、作成後は変わらない。 */
    val code: String,
    /** Client のジャンルフィルタの並び順。小さいほど前。 */
    val displayOrder: Int,
    /** 言語コード → 表示名。対応言語すべてが入る。 */
    val labels: Map<String, String>,
    /**
     * このジャンルを参照しているスポットの件数。
     *
     * 運営が削除の可否を判断するために返す。使用中のジャンルは削除できない（409）。
     * 登録・更新の応答では 0 ではなく実数を返さないため null になる。
     */
    val spotCount: Long?,
) {
    companion object {
        fun from(view: GenreView): GenreResponse =
            GenreResponse(
                code = view.code,
                displayOrder = view.displayOrder,
                labels = view.labels,
                spotCount = view.spotCount,
            )

        /** 登録・更新の応答。件数は集計しないため null。 */
        fun from(genre: Genre): GenreResponse =
            GenreResponse(
                code = genre.code.value,
                displayOrder = genre.displayOrder,
                labels =
                    genre.localizations.byLanguage.entries
                        .associate { (language, label) -> language.code to label },
                spotCount = null,
            )
    }
}
