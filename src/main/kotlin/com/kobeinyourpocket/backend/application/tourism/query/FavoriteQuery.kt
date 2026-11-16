package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.tourism.favorite.vo.FavoriteOwnerId

/** read 専用 port。application が定義し infrastructure.query が実装する。 */
interface FavoriteQuery {
    /** [ownerId] のお気に入りを、登録日時の新しい順に返す。削除済みのスポットは含めない。 */
    fun findByOwner(ownerId: FavoriteOwnerId): List<FavoriteView>
}
