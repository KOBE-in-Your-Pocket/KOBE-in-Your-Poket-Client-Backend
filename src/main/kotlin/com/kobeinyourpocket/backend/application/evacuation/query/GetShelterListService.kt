package com.kobeinyourpocket.backend.application.evacuation.query

import com.kobeinyourpocket.backend.domain.common.localization.Language
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

/**
 * 避難所一覧 + データセット meta をまとめて取得するユースケース（read / #85）。
 *
 * [ListSheltersService] と [GetShelterDatasetMetadataService] を別トランザクションで
 * 呼ぶと、間に別の更新（トリガー経由の updated_at 更新を含む）が挟まった場合、
 * 古い一覧 data と新しい meta.updatedAt が混ざったレスポンスになりうる。
 * Client は updatedAt をその data のバージョンとしてローカル保存するため、
 * 混ざると更新を恒久的に取りこぼす（要件定義 §4.4 E-2 の前提が崩れる）。
 *
 * PostgreSQL の既定分離レベル（READ COMMITTED）は同一トランザクション内でも
 * 文ごとに新しいスナップショットを見るため、REPEATABLE_READ まで上げて
 * 両方を同一スナップショットから取得する。
 */
@Service
class GetShelterListService(
    private val listSheltersService: ListSheltersService,
    private val getShelterDatasetMetadataService: GetShelterDatasetMetadataService,
) {
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    fun getShelterList(language: Language): ShelterListView =
        ShelterListView(
            shelters = listSheltersService.listShelters(language),
            metadata = getShelterDatasetMetadataService.getMetadata(),
        )
}
