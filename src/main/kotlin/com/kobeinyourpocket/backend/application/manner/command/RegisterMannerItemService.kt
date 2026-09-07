package com.kobeinyourpocket.backend.application.manner.command

import com.kobeinyourpocket.backend.application.media.MediaStorage
import com.kobeinyourpocket.backend.domain.manner.manneritem.model.MannerItem
import com.kobeinyourpocket.backend.domain.manner.manneritem.repository.MannerRepository
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIcon
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerIconUrl
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerKind
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerLocalizations
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.MannerScope
import com.kobeinyourpocket.backend.domain.manner.manneritem.vo.RelatedSpotId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * マナー項目の登録ユースケース（write）。
 *
 * **ID は運営に入力させず、英語タイトルから生成する。** ID の命名は運営の関心事ではなく、
 * 手入力にすると表記ゆれ・typo・重複が運用の負担になる（ジャンルマスタと同じ判断）。
 * 生成後は変更しない（Client の項目詳細の遷移先になるため）。
 *
 * **アイコン画像**: 先に [MediaStorage.commit] で確定させてから保存する（スポット登録と同じ順序）。
 * アップロードしただけの画像は staging として一定期間で自動削除されるため、確定を忘れると
 * 「登録できたのに画像が翌日消える」状態になる。逆に保存が失敗したときは
 * [MediaStorage.release] で staging に戻し、参照されない画像を残さない。
 */
@Service
class RegisterMannerItemService(
    private val mannerRepository: MannerRepository,
    private val mediaStorage: MediaStorage,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

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

        iconUrl?.let { mediaStorage.commit(it.value) }
        return try {
            mannerRepository.save(item)
        } catch (e: Exception) {
            iconUrl?.let { releaseQuietly(it.value) }
            throw e
        }
    }

    /**
     * 保存失敗時の巻き戻し。差し戻し自体が失敗しても元の例外を握りつぶさないよう、ログのみ残す
     * （残っても画像 1 件で、ストレージ側の突合で回収できる）。
     */
    private fun releaseQuietly(imageUrl: String) {
        runCatching { mediaStorage.release(imageUrl) }
            .onFailure { logger.error("failed to release media after save failure: {}", imageUrl, it) }
    }

    /**
     * 既存と衝突しない ID を決める。`no-littering` が埋まっていれば `no-littering-2`、以降 3, 4…。
     *
     * 衝突を 409 で弾かず採番するのは、運営から見ると「似た名前の別項目」を作ること自体は
     * 正当な操作で、ID の衝突は内部事情でしかないため（ジャンルマスタと同じ）。
     * 上限を設けているのは、想定外の状態で無限ループさせないため。
     *
     * **採番と保存は原子的ではない**（`RegisterGenreService` と同じ）。同じ英語タイトルの
     * 同時 POST が同じ ID を選ぶと、後続の保存が主キー制約で落ちて 500 になる。リトライを
     * 入れていないのは、運営アカウントが数名で「同一タイトルを同時刻に登録する」状況が
     * 現実的に起きないため。**起きたら運営が再送すれば済む**（採番し直される）。
     * 対処するならジャンル側と揃えて別途入れる。
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
