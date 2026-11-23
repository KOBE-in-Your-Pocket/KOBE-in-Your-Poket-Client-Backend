package com.kobeinyourpocket.backend.application.tourism.command

import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId

/**
 * 閲覧の重複・連打をまとめる port（#260 / 数え方は #242 で決定）。application が定義し infrastructure が実装する。
 *
 * [viewerKey] は閲覧者を見分けるためのキー（実装では利用者 IP）。閲覧はログインなしでも数えるため、
 * ユーザー ID では見分けられない。誰が閲覧したかは記録しない（キーは DB に保存しない）。
 */
interface SpotViewDeduplicator {
    /**
     * [viewerKey] から [spotId] への閲覧を数えてよいなら true。
     * 一定時間内の 2 回目以降は false（呼んだ時点で 1 回目として扱われる）。
     */
    fun tryCount(
        viewerKey: String,
        spotId: SpotId,
    ): Boolean
}
