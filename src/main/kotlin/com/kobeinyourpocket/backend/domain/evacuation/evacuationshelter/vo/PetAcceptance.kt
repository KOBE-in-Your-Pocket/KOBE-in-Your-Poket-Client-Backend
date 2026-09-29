package com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo

/**
 * ペット同行避難の受け入れ可否（値オブジェクト）。
 *
 * 神戸市オープンデータは ○ / × / 調整中 の 3 値。「調整中」が 38 件あり、可否の 2 値には潰せない。
 * ペットを連れている利用者にとっては避難先の選択を左右する情報なので、そのまま 3 値で持つ。
 */
enum class PetAcceptance(
    val wireValue: String,
) {
    /** ○ 受け入れる。 */
    ACCEPTED("accepted"),

    /** × 受け入れない。 */
    NOT_ACCEPTED("not-accepted"),

    /** 調整中。 */
    UNDER_CONSIDERATION("under-consideration"),
    ;

    companion object {
        /** wireValue から解決する。未対応・空は `null`。 */
        fun of(value: String): PetAcceptance? {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.wireValue == normalized }
        }
    }
}
