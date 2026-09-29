package com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.model

import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.PetAcceptance
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterCoordinates
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterLocalizations
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterSiting
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterSuitabilities
import com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo.ShelterType

/**
 * 避難所エンティティ（集約ルート）。
 *
 * 「避難所 1 件」を表す。同一性は [Id] が担い、内容が更新されても同じ EvacuationShelter として続く。
 * 全言語のローカライズを [localizations] として所有し、集約の整合境界に含める。
 *
 * 属性は神戸市オープンデータ「指定緊急避難場所・指定避難所」が持つ項目に揃えている（#180）。
 * 以前あった施設種別・画像・収容人数・バリアフリー・外部リンクは元データに無いため持たない。
 * 持っていない情報を空で返すより、項目ごと無い方が利用者にも Client にも誠実である。
 */
data class EvacuationShelter(
    val id: Id,
    val coordinates: ShelterCoordinates,
    val type: ShelterType,
    /** 屋内 / 屋外。 */
    val siting: ShelterSiting,
    /** 災害種別ごとの適否。全種別について値を持つ。 */
    val suitabilities: ShelterSuitabilities,
    /** ペット同行避難の可否（調整中を含む 3 値）。 */
    val petAcceptance: PetAcceptance,
    val localizations: ShelterLocalizations,
    /**
     * 施設の電話番号。屋外の緊急避難場所は元データが空。
     *
     * **単純な番号 1 つとは限らない。** 元データには `078-803-5921(昼)、078-803-5777(夜)` の
     * ように注記付きで 2 つ並ぶ値が 6 件ある。表示用の文字列として扱い、tel: リンクへ
     * そのまま流せる前提を置かない。
     */
    val phoneNumber: String? = null,
) {
    /**
     * 避難所の識別子（値オブジェクト）。
     *
     * エンティティの同一性そのものであり、エンティティを構成するため本ファイルに同居させる。
     * [Companion.of] が生成入口。
     */
    @JvmInline
    value class Id private constructor(
        val value: String,
    ) {
        init {
            require(value.isNotBlank()) { "EvacuationShelter.Id must not be blank" }
            require(value.length <= MAX_LENGTH) {
                "EvacuationShelter.Id must be at most $MAX_LENGTH characters, got ${value.length}"
            }
        }

        override fun toString(): String = value

        companion object {
            private const val MAX_LENGTH = 128

            fun of(value: String): Id = Id(value.trim())
        }
    }

    companion object {
        fun create(
            id: Id,
            coordinates: ShelterCoordinates,
            type: ShelterType,
            siting: ShelterSiting,
            suitabilities: ShelterSuitabilities,
            petAcceptance: PetAcceptance,
            localizations: ShelterLocalizations,
            phoneNumber: String? = null,
        ): EvacuationShelter =
            EvacuationShelter(
                id = id,
                coordinates = coordinates,
                type = type,
                siting = siting,
                suitabilities = suitabilities,
                petAcceptance = petAcceptance,
                localizations = localizations,
                // 空文字は「電話番号が無い」として null に寄せる（元データの屋外 88 件）。
                phoneNumber = phoneNumber?.trim()?.ifBlank { null },
            )
    }
}
