package com.kobeinyourpocket.backend.domain.tourism.view.repository

import com.kobeinyourpocket.backend.domain.tourism.view.model.SpotView

/** [リポジトリ] write port（command）。 */
interface SpotViewRepository {
    /**
     * 閲覧を 1 件記録する。同じスポットの閲覧も毎回 1 件として追加する（重複の扱いは #242 で決める）。
     *
     * スポットが存在しない場合は記録せず例外を投げる。スポットの存在確認は呼び出し側（ユースケース）で
     * 先に行う前提で、ここで投げるのは確認の直後にスポットが削除されたときなどに限られる。
     */
    fun record(view: SpotView)
}
