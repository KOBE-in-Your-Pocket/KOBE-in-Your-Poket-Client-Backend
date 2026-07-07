package com.kobeinyourpocket.backend.domain.tourism.review.vo

/**
 * [値オブジェクト] レビュー投稿者。
 *
 * クライアント側の `PublicUser`（name / iconUrl）に対応する。
 * PublicUser の仕様は確定前のため、フィールド追加・変更を前提とした薄い seam として定義する。
 */
data class ReviewAuthor(
    val name: String,
    val iconUrl: String? = null,
) {
    init {
        require(name.isNotBlank()) { "author name must not be blank" }
        require(name.length <= MAX_NAME_LENGTH) {
            "author name must be at most $MAX_NAME_LENGTH characters, got ${name.length}"
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 100
    }
}
