package com.kobeinyourpocket.backend.domain.tourism.review.vo

/**
 * [値オブジェクト] レビュー投稿者。
 *
 * クライアント側の `PublicUser`（id / name / iconUrl）に対応する。
 *
 * [userId] は本人判定の唯一の根拠（#86）。表示名は変更でき重複もするため判定に使わない。
 * V17 より前に投稿されたレビューは投稿者を特定できないため null になり、
 * その投稿は本人操作（編集・削除）の対象外となる。
 */
data class ReviewAuthor(
    val name: String,
    val iconUrl: String? = null,
    val userId: ReviewAuthorId? = null,
) {
    init {
        require(name.isNotBlank()) { "author name must not be blank" }
        require(name.length <= MAX_NAME_LENGTH) {
            "author name must be at most $MAX_NAME_LENGTH characters, got ${name.length}"
        }
    }

    /** [candidate] がこのレビューの投稿者本人か。投稿者不明（[userId] が null）の投稿は常に false。 */
    fun isOwnedBy(candidate: ReviewAuthorId): Boolean = userId != null && userId == candidate

    companion object {
        const val MAX_NAME_LENGTH = 100
    }
}
