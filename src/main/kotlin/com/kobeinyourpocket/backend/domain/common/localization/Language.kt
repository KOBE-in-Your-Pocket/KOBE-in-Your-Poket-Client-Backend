package com.kobeinyourpocket.backend.domain.common.localization

/**
 * [値オブジェクト] 対応言語。
 *
 * 言語は i18n の対応範囲（API 契約 `?lang=`）として運営側で固定される閉じた集合のため、
 * 文字列ラップではなく列挙で表現する。tourism / evacuation / manner 等、複数 feature が
 * 共用する汎用ドメイン（Localization は独立層にしない方針／architecture.md §4）の VO（#74）。
 * 既定（フォールバック先）は [DEFAULT]＝[EN]。Client `FALLBACK_LANGUAGE`（README の「無指定は en フォールバック」）に対応する（#84）。
 */
enum class Language(
    val code: String,
) {
    JA("ja"),
    EN("en"),
    KO("ko"),
    ZH("zh"),
    ;

    companion object {
        /** フォールバック先の既定言語。 */
        val DEFAULT: Language = EN

        /**
         * 言語コードから [Language] を解決する。未対応・空のコードは `null`。
         *
         * 「無指定・未対応なら en」のフォールバックは呼び出し側で `?: DEFAULT` として表現し、
         * ここでは判別不能を `null` で正直に返す（解決責務とフォールバック責務の分離）。
         */
        fun of(code: String): Language? {
            val normalized = code.trim().lowercase()
            return entries.firstOrNull { it.code == normalized }
        }
    }
}
