package com.kobeinyourpocket.backend.application.evacuation.command

import com.kobeinyourpocket.backend.application.media.MediaStorage
import com.kobeinyourpocket.backend.domain.common.localization.Language
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model.EvacuationShelter
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.repository.ShelterRepository
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCapacity
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCoordinates
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalizations
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterMedia
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterType
import com.kobeinyourpocket.backend.domain.evacuation.shelterfacilitycategory.model.ShelterFacilityCategory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * 避難所登録ユースケース（write / 運営ロール限定）。
 *
 * 神戸市オープンデータ（V9 seed）に無い避難所を運営が追加するための経路。
 *
 * **全対応言語（ja/en/zh/ko）を必須**とする。[ShelterLocalizations] の不変条件は
 * フォールバック言語（en）の存在だけで、「全言語そろっていること」は登録時ポリシーとして
 * application 層で持つと同 VO の KDoc が定めているため、ここで検証する。
 * 避難所は防災情報であり、外国人利用者にフォールバック（英語）で読ませる前提にしない。
 *
 * 画像は先に [MediaStorage.commit] で確定させてから保存する。確定に失敗したら登録自体を
 * 失敗させ、画像は staging のまま期限切れで消える（＝「登録済みなのに画像が翌日消える」状態を
 * 作らない）。逆に保存が失敗したときは [MediaStorage.release] で staging に戻す
 * （[RegisterSpotService][com.kobeinyourpocket.backend.application.tourism.command.RegisterSpotService] と同方針）。
 */
@Service
class RegisterShelterService(
    private val shelterRepository: ShelterRepository,
    private val mediaStorage: MediaStorage,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun registerShelter(
        coordinates: ShelterCoordinates,
        type: ShelterType,
        facilityCategory: ShelterFacilityCategory,
        media: ShelterMedia,
        accessible: Boolean,
        localizations: ShelterLocalizations,
        capacity: ShelterCapacity? = null,
        externalUrl: String? = null,
    ): EvacuationShelter {
        requireAllLanguages(localizations)

        val shelter =
            EvacuationShelter.create(
                // seed（V9）は意味のある slug を使うが、登録経路では衝突と採番の責任を持たないため UUID にする
                // （RegisterSpotService と同方針）。
                id = EvacuationShelter.Id.of(UUID.randomUUID().toString()),
                coordinates = coordinates,
                type = type,
                facilityCategory = facilityCategory,
                media = media,
                accessible = accessible,
                localizations = localizations,
                capacity = capacity,
                externalUrl = externalUrl,
            )

        mediaStorage.commit(media.imageUrl)
        return try {
            shelterRepository.save(shelter)
        } catch (e: Exception) {
            releaseQuietly(media)
            throw e
        }
    }

    private fun requireAllLanguages(localizations: ShelterLocalizations) {
        val required = Language.entries.toSet()
        require(localizations.languages == required) {
            "localizations must contain exactly the supported languages " +
                "${required.map { it.code }.sorted()}, got ${localizations.languages.map { it.code }.sorted()}"
        }
    }

    /**
     * 保存失敗時の巻き戻し。差し戻し自体が失敗しても元の例外を握りつぶさないよう、ログのみ残す
     * （残っても画像 1 件で、ストレージ側の突合で回収できる）。
     */
    private fun releaseQuietly(media: ShelterMedia) {
        runCatching { mediaStorage.release(media.imageUrl) }
            .onFailure { logger.error("failed to release media after save failure: {}", media.imageUrl, it) }
    }
}
