package com.kobeinyourpocket.backend.domain.manner.manneritem.repository

import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem

/**
 * MannerItem 集約の書き込み用リポジトリ port（command）。infrastructure.persistence.manner が実装する。
 *
 * read（`GET /manner/items?lang=`）は CQRS-lite に従い application.manner.query 側の専用 port で扱う。
 */
interface MannerRepository {
    fun save(item: MannerItem): MannerItem
}
