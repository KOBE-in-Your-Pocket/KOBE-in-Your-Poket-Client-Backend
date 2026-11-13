package com.kobeinyourpocket.backend.domain.tourism.favorite.repository

import com.kobeinyourpocket.backend.domain.tourism.favorite.model.Favorite
import com.kobeinyourpocket.backend.domain.tourism.favorite.vo.FavoriteOwnerId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId

/** [リポジトリ] write port（command）。 */
interface FavoriteRepository {
    /**
     * お気に入りを保存する。同じ組がすでにあれば何もしない（冪等）。最初の登録日時を残す。
     *
     * 事前の存在確認だけでは同時送信をすり抜けるため、実装は DB の一意制約違反も
     * 「すでにある」と読み替える。
     *
     * スポットが存在しない場合は保存せず例外を投げる（成功扱いにしない）。
     * スポットの存在確認は呼び出し側（ユースケース）で先に行う前提で、ここで投げるのは
     * 確認の直後にスポットが削除されたときなどに限られる。
     */
    fun saveIfAbsent(favorite: Favorite)

    /** お気に入りを解除する。該当が無くても例外にしない（冪等）。 */
    fun delete(
        ownerId: FavoriteOwnerId,
        spotId: SpotId,
    )
}
