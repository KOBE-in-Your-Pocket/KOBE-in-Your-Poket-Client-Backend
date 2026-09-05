package com.kobeinyourpocket.backend.application.tourism

/** 指定コードのジャンルが存在しない。REST 層で 404 に変換する。 */
class GenreNotFoundException(
    code: String,
) : RuntimeException("Genre not found: $code")
