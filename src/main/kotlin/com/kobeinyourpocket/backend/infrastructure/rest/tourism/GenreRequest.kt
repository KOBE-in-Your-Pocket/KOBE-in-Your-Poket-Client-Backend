package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreLocalizations
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty

/**
 * ジャンルの登録・更新リクエスト（#153）。
 *
 * **code は受け取らない。** 登録時は英語ラベルから自動生成し、更新時はパスの値を使う
 * （`spot.genre` が参照するため変更させない）。
 */
data class GenreRequest(
    @field:Min(0)
    val displayOrder: Int = 0,
    /** 言語コード → 表示名。対応言語すべてが必要。 */
    @field:NotEmpty
    val labels: Map<String, String> = emptyMap(),
) {
    /**
     * ドメインの VO に変換する。未対応の言語コードは捨てる。
     *
     * 「全言語そろっているか」の検証は [GenreLocalizations] の不変条件に任せる。
     * ここで先に弾くと、同じ規則が REST 層とドメイン層の 2 箇所に散る。
     */
    fun toLocalizations(): GenreLocalizations =
        GenreLocalizations.of(
            labels.mapNotNull { (code, label) -> Language.of(code)?.let { it to label } }.toMap(),
        )
}
