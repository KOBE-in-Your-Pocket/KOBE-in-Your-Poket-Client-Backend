package com.kobeinyourpocket.backend.application.media

/**
 * ストレージ上のオブジェクトキーの接頭辞。用途ごとに分ける（#184）。
 *
 * 分ける理由は運用側にある。不適切な画像への対処や容量の把握のときに、運営が上げた画像と
 * 利用者が上げた画像が同じ場所に混ざっていると見分けが付かない。
 *
 * **バケットのライフサイクル規則（未確定メディアの自動削除）は prefix で絞り込んでいる。**
 * ここに値を足したら、同じ prefix を対象にする規則も追加すること。規則を足し忘れると、
 * その prefix の未確定メディアが消えずに溜まり続ける。
 */
enum class MediaKeyPrefix(
    val value: String,
) {
    /** 運営が管理画面から上げた画像（スポット・マナーアイコン）。 */
    UPLOADS("uploads"),

    /** 利用者が設定したアイコン。 */
    ICONS("icons"),
    ;

    /** `uploads/xxxx.jpg` のようなキーを組み立てる。 */
    fun keyFor(fileName: String): String = "$value/$fileName"

    companion object {
        /** キーがいずれかの用途の配下にあるか。ストレージ側の受け入れ判定に使う。 */
        fun isKnownKey(key: String): Boolean = entries.any { key.startsWith("${it.value}/") }
    }
}
