package com.kobeinyourpocket.backend.domain.manner.manneritem.vo

/**
 * [値オブジェクト] マナー項目の適用範囲。
 *
 * `local`（各所・その土地固有）/ `japan`（日本全国）の閉じた集合のため列挙で表現する。
 * Client `MannerScope`（`domain/manner-item.ts`）のリテラルに一致させる（`kobe` ではなく `local`）。
 */
enum class MannerScope(
    val code: String,
) {
    LOCAL("local"),
    JAPAN("japan"),
    ;

    companion object {
        /** コードから [MannerScope] を解決する。未対応・空は不正入力として例外を送出する。 */
        fun of(code: String): MannerScope {
            val normalized = code.trim().lowercase()
            return entries.firstOrNull { it.code == normalized }
                ?: throw IllegalArgumentException("Unknown MannerScope: '$code'")
        }
    }
}
