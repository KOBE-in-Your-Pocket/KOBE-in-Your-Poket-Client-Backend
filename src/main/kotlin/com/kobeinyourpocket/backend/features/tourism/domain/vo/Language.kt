package com.kobeinyourpocket.backend.features.tourism.domain.vo

/**
 * 対応言語の Value Object。
 *
 * 言語は i18n の対応範囲（API 契約 `?lang=`）として運営側で固定される閉じた集合のため、
 * 文字列ラップ（[Genre]）ではなく列挙で表現する。
 * 既定（フォールバック先）は [DEFAULT]＝[JA]。Client / README の「無指定は ja フォールバック」に対応する。
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
        val DEFAULT: Language = JA

        /**
         * 言語コードから [Language] を解決する。未対応・空のコードは `null`。
         *
         * 「無指定・未対応なら ja」のフォールバックは呼び出し側で `?: DEFAULT` として表現し、
         * ここでは判別不能を `null` で正直に返す（解決責務とフォールバック責務の分離）。
         */
        fun of(code: String): Language? {
            val normalized = code.trim().lowercase()
            return entries.firstOrNull { it.code == normalized }
        }
    }
}
