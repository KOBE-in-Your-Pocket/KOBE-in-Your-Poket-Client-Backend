package com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.repository

import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter

/**
 * EvacuationShelter 集約の書き込み用リポジトリ port（command）。infrastructure.persistence.evacuation が実装する。
 *
 * read（`GET /evacuation/shelters?lang=`）は CQRS-lite に従い application.evacuation.query 側の専用 port で扱う。
 *
 * **現在このポートを呼ぶユースケースは無い。** 避難所は神戸市オープンデータを正とし、
 * 投入は Flyway のシードで行う（#180）。運営が追加・削除する経路は意図的に持たない。
 * 残しているのは、将来オープンデータを定期取得して入れ替える取り込み処理を書くときの
 * 差し込み口になるためと、テストで避難所を用意するのに使っているため。
 */
interface ShelterRepository {
    fun save(shelter: EvacuationShelter): EvacuationShelter
}
