package com.kobeinyourpocket.backend.domain.manner.manneritem.repository

import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem

/**
 * MannerItem 集約の書き込み用リポジトリ port（command）。infrastructure.persistence.manner が実装する。
 *
 * read（`GET /manner/items?lang=`）は CQRS-lite に従い application.manner.query 側の専用 port で扱う。
 * ここで [findById] を持つのは、更新が「現在の集約を取り出して差し替える」write の操作であり、
 * read モデル（言語解決済みの projection）では集約を復元できないため。
 */
interface MannerRepository {
    fun save(item: MannerItem): MannerItem

    /** 更新対象の取得。存在しなければ null。 */
    fun findById(id: MannerItem.Id): MannerItem?

    /**
     * 更新用に行ロックを取って取得する。存在しなければ null。
     *
     * 同じ項目への同時更新を直列化するため、更新ユースケースはこちらを使う。
     */
    fun findByIdForUpdate(id: MannerItem.Id): MannerItem?

    fun existsById(id: MannerItem.Id): Boolean

    /**
     * 削除する。**実際に消したら true、対象が無ければ false**。
     *
     * 存在確認と削除を分けると、その間に別リクエストが消した場合を取りこぼす。
     * 呼び出し側は戻り値で「見つからなかった」を判定する。
     * 子テーブル（localization / spot）は DB の ON DELETE CASCADE で連動削除される。
     */
    fun deleteById(id: MannerItem.Id): Boolean
}
