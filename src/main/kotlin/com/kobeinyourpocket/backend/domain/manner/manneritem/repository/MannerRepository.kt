package com.kobeinyourpocket.backend.domain.manner.manneritem.repository

import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItemWithLocalizations

/**
 * [リポジトリ] write 専用 port（command）。infrastructure.persistence.manner が実装する。
 *
 * read（`GET /manner/items?lang=`）は CQRS-lite に従い application.manner.query 側の専用 port で扱う。
 */
interface MannerRepository {
    fun save(item: MannerItemWithLocalizations): MannerItemWithLocalizations
}
