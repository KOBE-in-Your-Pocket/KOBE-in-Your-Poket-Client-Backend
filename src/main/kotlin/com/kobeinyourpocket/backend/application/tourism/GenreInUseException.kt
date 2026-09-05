package com.kobeinyourpocket.backend.application.tourism

/**
 * 削除しようとしたジャンルがスポットから参照されている。REST 層で 409 に変換する。
 *
 * 参照件数をメッセージに含めるのは、運営が「何件付け替えれば消せるのか」を
 * その場で判断できるようにするため。
 *
 * [spotCount] が null になるのは、件数確認の直後に別トランザクションが同じジャンルの
 * スポットを登録し、DB の外部キー制約で削除が弾かれた場合。**その時点でトランザクションは
 * 中断されており件数を数え直せない**ため、件数不明として扱う。
 */
class GenreInUseException(
    code: String,
    val spotCount: Long?,
) : RuntimeException(
        if (spotCount == null) {
            "Genre '$code' is still referenced by spots"
        } else {
            "Genre '$code' is used by $spotCount spot(s)"
        },
    )
