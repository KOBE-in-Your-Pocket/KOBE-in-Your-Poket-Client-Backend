package com.kobeinyourpocket.backend.domain.weather.repository

import com.kobeinyourpocket.backend.domain.weather.model.WeatherSnapshot
import com.kobeinyourpocket.backend.domain.weather.vo.WeatherPoint

/**
 * [リポジトリ] 天気の write port（#231）。
 *
 * 地点ごとに最新の取得分だけを持つ。履歴は残さない。
 */
interface WeatherRepository {
    /**
     * 地点の天気を [snapshot] の内容に置き換える。
     *
     * その地点の前回の取得分を消してから入れ直す（1 トランザクション）。途中で失敗したら前回の取得分が残る。
     */
    fun replace(snapshot: WeatherSnapshot)

    /** 地点の最新の天気を返す。まだ 1 度も保存していなければ null。 */
    fun findLatest(point: WeatherPoint): WeatherSnapshot?
}
