package com.kobeinyourpocket.backend.application.tourism

/**
 * 削除しようとしたジャンルがスポットから参照されている。REST 層で 409 に変換する。
 *
 * 参照件数をメッセージに含めるのは、運営が「何件付け替えれば消せるのか」を
 * その場で判断できるようにするため。
 */
class GenreInUseException(
    code: String,
    val spotCount: Long,
) : RuntimeException("Genre '$code' is used by $spotCount spot(s)")
