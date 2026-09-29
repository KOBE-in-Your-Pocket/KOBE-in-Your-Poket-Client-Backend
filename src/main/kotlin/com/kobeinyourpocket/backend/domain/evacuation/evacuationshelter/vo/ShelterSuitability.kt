package com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo

/**
 * ある災害に対してその避難所が使えるか（値オブジェクト）。
 *
 * 神戸市オープンデータは ○ / △ / × / － / 空 の 5 値で、これを 4 値に寄せている（#180）。
 * **[CONDITIONAL]（△）を [SUITABLE] に丸めてはいけない。** 条件付きの避難所を「使える」と
 * 表示すると、災害時に誤った判断をさせる。条件の内容は備考（`note`）にある。
 *
 * － と空はどちらも「その災害では対象外」の意味なので [NOT_APPLICABLE] に寄せている。
 * 元データでは、屋内の緊急避難場所は大火事の列が空、屋外は土砂災害・洪水の列が空になっている。
 */
enum class ShelterSuitability(
    val wireValue: String,
) {
    /** ○ 利用できる。 */
    SUITABLE("suitable"),

    /** △ 条件付きで利用できる。備考を読む必要がある。 */
    CONDITIONAL("conditional"),

    /** × 利用できない。 */
    UNSUITABLE("unsuitable"),

    /** － / 空 その災害では対象外。 */
    NOT_APPLICABLE("not-applicable"),
    ;

    companion object {
        /** wireValue から解決する。未対応・空は `null`。 */
        fun of(value: String): ShelterSuitability? {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.wireValue == normalized }
        }
    }
}
