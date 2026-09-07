package com.kobeinyourpocket.backend.application.manner.command

/** 指定 ID のマナー項目が存在しない。REST 層で 404 に変換する。 */
class MannerItemNotFoundException(
    id: String,
) : RuntimeException("Manner item not found: $id")

/**
 * 英語タイトルから ID を生成できない。REST 層で 400 に変換する。
 *
 * 英語タイトルが記号のみ（`---` 等）の場合に起こる。ここで既定値を勝手に付けると、
 * 意図しない ID が黙って残るため入力を直してもらう。
 */
class InvalidMannerTitleException(
    title: String,
) : RuntimeException("Cannot derive a manner item id from the English title: '$title'")

/**
 * 対応言語の文言が揃っていない。REST 層で 400 に変換する。
 *
 * 1 言語でも欠けると、その言語のアプリで項目が表示できない。ドメインの不変条件は
 * フォールバック言語（en）の存在までで、「全言語そろえる」は登録時のポリシーとして
 * ここで担保する（`MannerLocalizations` の設計方針）。
 */
class IncompleteMannerLocalizationsException(
    missing: Collection<String>,
) : RuntimeException("Manner item localizations are missing languages: ${missing.sorted().joinToString()}")
