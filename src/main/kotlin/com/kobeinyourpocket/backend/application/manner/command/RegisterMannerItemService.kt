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
 * マナー項目の登録ユースケース（write）。
 *
 * **ID は運営に入力させず、英語タイトルから生成する。** ID の命名は運営の関心事ではなく、
 * 手入力にすると表記ゆれ・typo・重複が運用の負担になる（ジャンルマスタと同じ判断）。
 * 生成後は変更しない（Client の項目詳細の遷移先になるため）。
 */
@Service
class RegisterMannerItemService(
    private val mannerRepository: MannerRepository,
) {
    fun registerMannerItem(
        icon: MannerIcon?,
        iconUrl: MannerIconUrl?,
        kind: MannerKind,
        scope: MannerScope,
        relatedSpotIds: List<RelatedSpotId>,
        localizations: MannerLocalizations,
    ): MannerItem {
        requireAllLanguages(localizations)

        val englishTitle = localizations.resolve(MannerLocalizations.FALLBACK).title
        val base = MannerItem.Id.fromTitle(englishTitle) ?: throw InvalidMannerTitleException(englishTitle)

        val item =
            MannerItem.create(
                id = resolveUniqueId(base),
                icon = icon,
                iconUrl = iconUrl,
                kind = kind,
                scope = scope,
                localizations = localizations,
                relatedSpotIds = relatedSpotIds,
            )
        return mannerRepository.save(item)
    }

    /**
     * 既存と衝突しない ID を決める。`no-littering` が埋まっていれば `no-littering-2`、以降 3, 4…。
     *
     * 衝突を 409 で弾かず採番するのは、運営から見ると「似た名前の別項目」を作ること自体は
     * 正当な操作で、ID の衝突は内部事情でしかないため（ジャンルマスタと同じ）。
     * 上限を設けているのは、想定外の状態で無限ループさせないため。
     */
    private fun resolveUniqueId(base: MannerItem.Id): MannerItem.Id {
        if (!mannerRepository.existsById(base)) return base

        for (suffix in 2..MAX_ID_SUFFIX) {
            val candidate = MannerItem.Id.of("${base.value}-$suffix")
            if (!mannerRepository.existsById(candidate)) return candidate
        }
        throw InvalidMannerTitleException(base.value)
    }

    private companion object {
        const val MAX_ID_SUFFIX = 100
    }
}
