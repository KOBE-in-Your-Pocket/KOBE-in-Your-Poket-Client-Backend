package com.kobeinyourpocket.backend.domain.tourism.genre.vo

/**
 * [値オブジェクト] ジャンルの識別子。`spot.genre` に保存され、外部キーで参照される（#153）。
 *
 * 運営には入力させず [fromLabel] で英語ラベルから生成する。ID の命名は運営の関心事ではなく、
 * 手入力にすると表記ゆれ・typo・重複が運用の負担になるため。生成後は**変更しない**
 * （既存スポットが参照しており、変えると参照先が消える）。
 */
@JvmInline
value class GenreCode private constructor(
    val value: String,
) {
    override fun toString(): String = value

    companion object {
        /** DB の `genre.code` と同じ上限長（V13）。 */
        const val MAX_LENGTH = 64

        /** 英小文字・数字をハイフンで繋いだ形。DB の CHECK 制約（V13）と一致させる。 */
        private val PATTERN = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")

        /** 既存の値（DB からの復元・API のパス）を [GenreCode] にする。形式が違えば例外。 */
        fun of(value: String): GenreCode {
            val normalized = value.trim()
            require(normalized.length <= MAX_LENGTH) { "Genre code must be $MAX_LENGTH characters or less" }
            require(PATTERN.matches(normalized)) { "Genre code must match ${PATTERN.pattern}: '$value'" }
            return GenreCode(normalized)
        }

        /**
         * 英語ラベルから slug を生成する（`Night View` → `night-view`）。
         *
         * 英数字以外は区切りとして扱う。日本語ラベルから作らないのは、ローマ字化の規則を
         * 持たないと `夜景` が意味のある slug にならないため。英語ラベルは登録時必須なので
         * 常に入力がある。
         *
         * 生成結果が空になる場合（英語ラベルが記号だけ等）は null を返し、呼び出し側で
         * 代替の決め方に委ねる。ここで勝手な既定値を返すと、意図しない code が黙って残る。
         */
        fun fromLabel(label: String): GenreCode? {
            val slug =
                label
                    .trim()
                    .lowercase()
                    .replace(Regex("[^a-z0-9]+"), "-")
                    .trim('-')
                    .take(MAX_LENGTH)
                    .trim('-')

            return if (PATTERN.matches(slug)) GenreCode(slug) else null
        }
    }
}
