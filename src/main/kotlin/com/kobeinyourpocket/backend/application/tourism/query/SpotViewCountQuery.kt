package com.kobeinyourpocket.backend.application.tourism.query

import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId

/**
 * read 専用 port。application が定義し infrastructure.query が実装する（#239）。
 *
 * 集計は閲覧の合計（全期間）。日別などの集計が必要になったら、ここに足す。
 */
interface SpotViewCountQuery {
    /** [spotId] の閲覧数を返す。記録が無いスポットは 0。 */
    fun countBySpotId(spotId: SpotId): Long

    /**
     * スポット別の閲覧数を、多い順に返す（同数は spot_id 順）。運営向けの閲覧数一覧（#241）で使う。
     * 閲覧が 1 件も無いスポットと、削除済みのスポットは含めない。
     */
    fun countAllBySpot(): List<SpotViewCount>
}
