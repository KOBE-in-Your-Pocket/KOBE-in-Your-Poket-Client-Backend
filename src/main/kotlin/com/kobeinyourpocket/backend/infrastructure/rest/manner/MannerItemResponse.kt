package com.kobeinyourpocket.backend.infrastructure.rest.manner

import com.kobeinyourpocket.backend.application.manner.query.MannerItemView
import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalizations

/**
 * `GET/POST/PUT /api/v1/manner/items` のレスポンス（Client `MannerItem` 型に一致 / 要件定義 §4.5）。
 *
 * kind は `manner|rule`、scope は `local|japan` のリテラル。
 * relatedSpotIds は Tourism `Spot.id` への ID 参照のみ（M-2）。
 *
 * [title] / [description] は要求言語で解決済み（Client 用）。[localizations] は全言語ぶんで、
 * 運営の管理画面が編集フォームに使う。1 リクエストで両方返すのは、言語ごとに取り直すと
 * 編集のたびに 4 往復になるため（マスタなので件数は限られる）。
 */
data class MannerItemResponse(
    val id: String,
    val title: String,
    val description: String,
    /** アイコン識別キー。画像 URL のみの項目では null。 */
    val icon: String?,
    /**
     * アップロードされたアイコン画像の URL。未設定なら null。
     *
     * Client はこれがあればリモート画像で描き、無ければ [icon] のキーで同梱アセットへ
     * 解決する（従来どおり）。
     */
    val iconUrl: String?,
    val kind: String,
    val scope: String,
    val relatedSpotIds: List<String>,
    /** 言語コード → 文言。 */
    val localizations: Map<String, LocalizationBody>,
) {
    data class LocalizationBody(
        val title: String,
        val description: String,
    )

    companion object {
        fun from(view: MannerItemView): MannerItemResponse =
            MannerItemResponse(
                id = view.id,
                title = view.title,
                description = view.description,
                icon = view.icon,
                iconUrl = view.iconUrl,
                kind = view.kind,
                scope = view.scope,
                relatedSpotIds = view.relatedSpotIds,
                localizations =
                    view.localizations.mapValues { (_, localization) ->
                        LocalizationBody(title = localization.title, description = localization.description)
                    },
            )

        /**
         * 登録・更新の応答。書き込んだ集約をそのまま返す。
         *
         * 解決済みの title / description は、運営が編集した内容をそのまま確認できるよう
         * フォールバック言語（en）の値を入れる。read 側と違い言語指定を伴わないため。
         */
        fun from(item: MannerItem): MannerItemResponse {
            val fallback = item.localizations.resolve(MannerLocalizations.FALLBACK)
            return MannerItemResponse(
                id = item.id.value,
                title = fallback.title,
                description = fallback.description,
                icon = item.icon?.value,
                iconUrl = item.iconUrl?.value,
                kind = item.kind.code,
                scope = item.scope.code,
                relatedSpotIds = item.relatedSpotIds.map { it.value },
                localizations =
                    item.localizations.byLanguage.entries.associate { (language, localization) ->
                        language.code to LocalizationBody(title = localization.title, description = localization.description)
                    },
            )
        }
    }
}
