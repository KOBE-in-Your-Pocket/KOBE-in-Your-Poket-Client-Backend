package com.kobeinyourpocket.backend.application.manner.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import org.springframework.stereotype.Service

/** マナー項目一覧取得ユースケース（read）。domain 集約を経由せず [MannerItemQuery] port へ委譲する。 */
@Service
class ListMannerItemsService(
    private val mannerItemQuery: MannerItemQuery,
) {
    fun listMannerItems(language: Language): List<MannerItemView> = mannerItemQuery.findAllResolved(language)
}
