package com.kobeinyourpocket.backend.infrastructure.query.manner

import com.kobeinyourpocket.backend.application.manner.query.MannerItemQuery
import com.kobeinyourpocket.backend.application.manner.query.MannerItemView
import com.kobeinyourpocket.backend.application.manner.query.MannerLocalizationView
import com.kobeinyourpocket.backend.domain.common.localization.Language
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.springframework.stereotype.Repository

/**
 * [MannerItemQuery] の JPA 実装。
 *
 * ローカライズは**全言語ぶんを引いてメモリ上で解決する**（要求言語 → 無ければ既定 en /
 * [Language.DEFAULT]）。SQL で 1 言語に絞り込まないのは、レスポンスが解決済みの
 * title / description と全言語の localizations の両方を必要とするため。件数の限られた
 * マスタなので、言語ごとの行を展開してもコストにならない。
 *
 * relatedSpotIds は M-2 の ID 参照のみ（JOIN 配信しない）。行の複製を避けるため
 * `manner_item_spot` は別クエリで引き、メモリ上で項目 id ごとにまとめる。
 */
@Repository
class MannerItemQueryJpa(
    private val entityManager: EntityManager,
) : MannerItemQuery {
    override fun findAllResolved(language: Language): List<MannerItemView> {
        val relatedSpotIdsByItemId = findRelatedSpotIds()
        val localizationsByItemId = findLocalizations()

        return entityManager
            .createNativeQuery(SELECT_MANNER_ITEM)
            .resultRows()
            .map { row ->
                toMannerItemView(
                    row = row,
                    language = language,
                    localizations = localizationsByItemId[row[Column.ID] as String].orEmpty(),
                    relatedSpotIds = relatedSpotIdsByItemId[row[Column.ID] as String].orEmpty(),
                )
            }
    }

    private fun findRelatedSpotIds(): Map<String, List<String>> =
        entityManager
            .createNativeQuery(SELECT_RELATED_SPOT_IDS)
            .resultRows()
            .groupBy(keySelector = { it[0] as String }, valueTransform = { it[1] as String })

    /** 項目 id → （言語コード → 文言）。 */
    private fun findLocalizations(): Map<String, Map<String, MannerLocalizationView>> =
        entityManager
            .createNativeQuery(SELECT_LOCALIZATIONS)
            .resultRows()
            .groupBy { it[0] as String }
            .mapValues { (_, rows) ->
                rows.associate {
                    (it[1] as String) to
                        MannerLocalizationView(
                            title = it[2] as String,
                            description = it[3] as String,
                        )
                }
            }

    @Suppress("UNCHECKED_CAST")
    private fun Query.resultRows(): List<Array<Any?>> = resultList as List<Array<Any?>>

    private fun toMannerItemView(
        row: Array<Any?>,
        language: Language,
        localizations: Map<String, MannerLocalizationView>,
        relatedSpotIds: List<String>,
    ): MannerItemView {
        // 要求言語 → 既定言語（en）の順で解決する。どちらも無い項目は登録経路が許さないが、
        // 直接 DB を触られた場合に落ちないよう空文字で凌ぐ（画面には空欄として出る）。
        val resolved =
            localizations[language.code]
                ?: localizations[Language.DEFAULT.code]
                ?: MannerLocalizationView(title = "", description = "")

        return MannerItemView(
            id = row[Column.ID] as String,
            title = resolved.title,
            description = resolved.description,
            icon = row[Column.ICON] as String?,
            iconUrl = row[Column.ICON_URL] as String?,
            kind = row[Column.KIND] as String,
            scope = row[Column.SCOPE] as String,
            relatedSpotIds = relatedSpotIds,
            localizations = localizations,
        )
    }

    /** [SELECT_MANNER_ITEM] の列順と対応する index。列の並び替え時は両方を合わせて更新すること。 */
    private object Column {
        const val ID = 0
        const val ICON = 1
        const val ICON_URL = 2
        const val KIND = 3
        const val SCOPE = 4
    }

    private companion object {
        val SELECT_MANNER_ITEM =
            """
            SELECT
                m.id,
                m.icon,
                m.icon_url,
                m.kind,
                m.scope
            FROM manner_item m
            ORDER BY m.id
            """.trimIndent()

        val SELECT_LOCALIZATIONS =
            """
            SELECT l.manner_item_id, l.language, l.title, l.description
            FROM manner_item_localization l
            ORDER BY l.manner_item_id, l.language
            """.trimIndent()

        val SELECT_RELATED_SPOT_IDS =
            """
            SELECT s.manner_item_id, s.spot_id
            FROM manner_item_spot s
            ORDER BY s.manner_item_id, s.spot_id
            """.trimIndent()
    }
}
