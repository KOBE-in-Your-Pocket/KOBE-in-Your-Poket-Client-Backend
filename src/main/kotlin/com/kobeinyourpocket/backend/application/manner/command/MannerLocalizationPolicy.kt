package com.kobeinyourpocket.backend.application.manner.command

import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalizations

/**
 * 対応言語がすべて揃っているか検証する（登録・更新で共通の入口ポリシー）。
 *
 * ドメインの不変条件はフォールバック言語（en）の存在までに留めてある。対応言語が将来
 * 増えたときに、既存データが「全言語必須」で一斉に不正化するのを避けるためで、これは
 * 集約の設計として妥当（`MannerLocalizations` の方針）。
 *
 * 一方で、新しく登録・更新する項目に欠けた言語があると、その言語のアプリで項目が
 * 出せない。**入口では全言語そろえる**を application 層のポリシーとして課す。
 */
internal fun requireAllLanguages(localizations: MannerLocalizations) {
    val missing =
        Language.entries
            .filterNot { localizations.languages.contains(it) }
            .map { it.code }

    if (missing.isNotEmpty()) throw IncompleteMannerLocalizationsException(missing)
}
