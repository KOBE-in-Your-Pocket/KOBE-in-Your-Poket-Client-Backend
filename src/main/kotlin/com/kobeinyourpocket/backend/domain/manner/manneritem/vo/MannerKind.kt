package com.kobeinyourpocket.backend.domain.manner.manneritem.vo

/**
 * マナー項目の種別（値オブジェクト）。
 *
 * `manner`（推奨マナー）/ `rule`（規則）の閉じた集合のため列挙で表現する。
 * Client `MannerKind`（`domain/manner-item.ts`）のリテラルに一致させる。
 */
enum class MannerKind(
    val code: String,
) {
    MANNER("manner"),
    RULE("rule"),
    ;

    companion object {
        /** コードから解決する。未対応・空は不正入力として例外を送出する。 */
        fun of(code: String): MannerKind {
            val normalized = code.trim().lowercase()
            return entries.firstOrNull { it.code == normalized }
                ?: throw IllegalArgumentException("Unknown MannerKind: '$code'")
        }
    }
}
