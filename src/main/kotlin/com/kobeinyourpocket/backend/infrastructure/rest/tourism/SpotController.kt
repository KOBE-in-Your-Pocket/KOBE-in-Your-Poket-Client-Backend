package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.command.RegisterSpotService
import com.kobeinyourpocket.backend.application.tourism.query.GetSpotService
import com.kobeinyourpocket.backend.application.tourism.query.ListSpotsService
import com.kobeinyourpocket.backend.domain.tourism.localization.Language
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Coordinates
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.Genre
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotId
import com.kobeinyourpocket.backend.domain.tourism.spot.vo.SpotMedia
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Spot の REST inbound adapter（§8）。application 経由のみ（persistence 直叩き禁止 / §2）。
 */
@RestController
@RequestMapping("/api/v1/tourism/spots")
class SpotController(
    private val listSpotsService: ListSpotsService,
    private val getSpotService: GetSpotService,
    private val registerSpotService: RegisterSpotService,
) {
    @GetMapping
    fun listSpots(
        @RequestParam(name = "lang", required = false) lang: String?,
        @RequestHeader(name = "Accept-Language", required = false) acceptLanguage: String?,
    ): List<SpotResponse> {
        val language = resolveLanguage(lang, acceptLanguage)
        return listSpotsService.listSpots(language).map(SpotResponse::from)
    }

    @GetMapping("/{id}")
    fun getSpot(
        @PathVariable id: String,
        @RequestParam(name = "lang", required = false) lang: String?,
        @RequestHeader(name = "Accept-Language", required = false) acceptLanguage: String?,
    ): SpotResponse {
        val language = resolveLanguage(lang, acceptLanguage)
        return SpotResponse.from(getSpotService.getSpot(SpotId.of(id), language))
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun registerSpot(
        @Valid @RequestBody request: RegisterSpotRequest,
    ): SpotResponse {
        val saved =
            registerSpotService.registerSpot(
                genre = Genre.of(request.genre),
                coordinates = Coordinates.of(request.coordinates.latitude, request.coordinates.longitude),
                media = SpotMedia(imageUrl = request.imageUrl),
                localizations = request.toLocalizations(),
            )
        return SpotResponse.fromRegistered(saved)
    }

    private fun resolveLanguage(
        lang: String?,
        acceptLanguage: String?,
    ): Language =
        lang?.let { Language.of(it) }
            ?: acceptLanguage?.let(::parseAcceptLanguage)
            ?: Language.DEFAULT

    private fun parseAcceptLanguage(header: String): Language? =
        header
            .split(',')
            .map { it.substringBefore(';').substringBefore('-').trim() }
            .firstNotNullOfOrNull { Language.of(it) }
}
