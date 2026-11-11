package com.kobeinyourpocket.backend.domain.weather

/**
 * Weather（天気） — bounded context マーカー（#226）。
 *
 * 配置: domain/weather · application/weather · infrastructure/weather
 * 外部の気象情報 API（Yahoo! 気象情報API）から取得した降水強度を扱う。
 * 取得は application 層の [com.kobeinyourpocket.backend.application.weather.WeatherGateway] を通し、
 * ドメイン層は API 固有の形（Yahoo! の JSON）を知らない。
 *
 * @see docs/weather.md
 */
internal object WeatherContext
