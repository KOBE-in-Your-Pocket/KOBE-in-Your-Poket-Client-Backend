package com.kobeinyourpocket.backend.infrastructure.rest.stats

import com.kobeinyourpocket.backend.application.stats.query.GetDashboardStatsService
import com.kobeinyourpocket.backend.application.tourism.query.ListSpotViewCountsService
import com.kobeinyourpocket.backend.infrastructure.rest.common.LanguageResolver
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 管理画面ダッシュボード向けの統計 REST inbound adapter（#169）。
 *
 * 画面と 1 対 1 の集計を 1 リクエストで返す。運営が見る値のみで、
 * Client アプリからは使わないため公開しない。
 *
 * 認可は運営ロール限定。閲覧系は SecurityConfig で permitAll のため、守るのはメソッドセキュリティ側になる。
 * **全ユーザー数・投稿者名を含む**ので、レビュー横断一覧（`/api/v1/tourism/reviews`）と同じ扱いにする。
 * ロール階層で ADMIN も通る。
 */
@RestController
@RequestMapping("/api/v1/stats")
class StatsController(
    private val getDashboardStatsService: GetDashboardStatsService,
    private val listSpotViewCountsService: ListSpotViewCountsService,
) {
    /** `?lang=` は**スポット名の解決にのみ**効く。投稿者名は投稿時の言語のまま返る。 */
    @GetMapping
    @PreAuthorize("hasRole('OPERATOR')")
    fun getStats(
        @RequestParam(name = "lang", required = false) lang: String?,
        @RequestHeader(name = "Accept-Language", required = false) acceptLanguage: String?,
    ): DashboardStatsResponse {
        val language = LanguageResolver.resolve(lang, acceptLanguage)
        return DashboardStatsResponse.from(getDashboardStatsService.getStats(language))
    }

    /**
     * スポット別の閲覧数を多い順に返す（#241 / docs/spot-view.md）。
     * 閲覧が 1 件も無いスポットは含めない。`?lang=` はスポット名の解決にのみ効く。
     */
    @GetMapping("/spot-views")
    @PreAuthorize("hasRole('OPERATOR')")
    fun listSpotViews(
        @RequestParam(name = "lang", required = false) lang: String?,
        @RequestHeader(name = "Accept-Language", required = false) acceptLanguage: String?,
    ): List<SpotViewCountResponse> {
        val language = LanguageResolver.resolve(lang, acceptLanguage)
        return listSpotViewCountsService.list(language).map(SpotViewCountResponse::from)
    }
}
