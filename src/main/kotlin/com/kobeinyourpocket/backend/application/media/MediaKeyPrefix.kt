package com.kobeinyourpocket.backend.application.media

/**
 * ストレージ上のオブジェクトキーの接頭辞。用途ごとに分ける（#184）。
 *
 * 分ける理由は運用側にある。不適切な画像への対処や容量の把握のときに、運営が上げた画像と
 * 利用者が上げた画像が同じ場所に混ざっていると見分けが付かない。
 *
 * **バケット側の設定（実行ロールの書き込み権限・公開読み取り・未確定メディアの自動削除）は
 * `uploads/` を対象にしている。** 新しい値は `uploads/` の下に置けば設定を足さずに済む。
 * それ以外の場所に置く場合は、同じ prefix を対象にする IAM 権限・バケットポリシー・
 * ライフサイクル規則も追加すること。足し忘れると保存が Access Denied になるか、
 * 未確定メディアが消えずに溜まり続ける。
 */
enum class MediaKeyPrefix(
    val value: String,
) {
    /** 運営が管理画面から上げた画像（スポット・マナーアイコン）。 */
    UPLOADS("uploads"),

    /**
     * 利用者が設定したアイコン。
     *
     * 当初は `icons/` だったが、実行ロールの権限が `uploads/` 配下にしか無く保存に失敗したため、
     * バケット側の設定を流用できる `uploads/` の下へ移した。
     */
    ICONS("uploads/icons"),
    ;

    /** `uploads/xxxx.jpg` のようなキーを組み立てる。 */
    fun keyFor(fileName: String): String = "$value/$fileName"

    companion object {
        /** キーがいずれかの用途の配下にあるか。ストレージ側の受け入れ判定に使う。 */
        fun isKnownKey(key: String): Boolean = entries.any { key.startsWith("${it.value}/") }
    }
}
