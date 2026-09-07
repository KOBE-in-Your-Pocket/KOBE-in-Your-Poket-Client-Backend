package com.kobeinyourpocket.backend.application.manner.command

import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem
import com.kobeinyourpocket.backend.domain.manner.manneritem.repository.MannerRepository
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIcon
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIconUrl
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerKind
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalizations
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerScope
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.RelatedSpotId
import org.springframework.stereotype.Service

/**
 * マナー項目の更新ユースケース（write）。全置換であって部分更新ではない。
 *
 * **ID は変更しない。** Client の項目詳細（`/manner/[id]`）の遷移先であり、変えると既存の
 * リンクが切れる。英語タイトルを変えても ID は追従しない。名前を大きく変えたい場合は
 * 新しく作って旧項目を削除する運用になる。
 */
@Service
class UpdateMannerItemService(
    private val mannerRepository: MannerRepository,
) {
    fun updateMannerItem(
        id: MannerItem.Id,
        icon: MannerIcon?,
        iconUrl: MannerIconUrl?,
        kind: MannerKind,
        scope: MannerScope,
        relatedSpotIds: List<RelatedSpotId>,
        localizations: MannerLocalizations,
    ): MannerItem {
        requireAllLanguages(localizations)

        val current = mannerRepository.findById(id) ?: throw MannerItemNotFoundException(id.value)
        return mannerRepository.save(
            current.update(
                icon = icon,
                iconUrl = iconUrl,
                kind = kind,
                scope = scope,
                relatedSpotIds = relatedSpotIds,
                localizations = localizations,
            ),
        )
    }
}
