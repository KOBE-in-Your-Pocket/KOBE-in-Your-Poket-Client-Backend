package com.kobeinyourpocket.backend.domain.manner.manneritem.vo

/**
 * [値オブジェクト] マナー項目の種別。
 *
 * `manner`（マナー・推奨）/ `rule`（ルール・規則）の閉じた集合のため列挙で表現する。
 * Client `MannerKind`（`domain/manner-item.ts`）のリテラルに一致させる。
 */
enum class MannerKind(
    val code: String,
) {
    MANNER("manner"),
    RULE("rule"),
    ;

    companion object {
        /** コードから [MannerKind] を解決する。未対応・空は不正入力として例外を送出する。 */
        fun of(code: String): MannerKind {
            val normalized = code.trim().lowercase()
            return entries.firstOrNull { it.code == normalized }
                ?: throw IllegalArgumentException("Unknown MannerKind: '$code'")
        }
    }
}
