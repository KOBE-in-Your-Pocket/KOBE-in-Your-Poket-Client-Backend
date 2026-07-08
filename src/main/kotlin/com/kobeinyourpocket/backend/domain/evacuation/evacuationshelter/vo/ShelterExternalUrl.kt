package com.kobeinyourpocket.backend.domain.evacuation.evacuationshelter.vo

import java.net.URI
import java.net.URISyntaxException

/**
 * 外部参照 URL（値オブジェクト・言語非依存・任意）。
 *
 * Client `EvacuationShelter.externalUrl?` に対応する。
 * ブラウザ起動用途のため http / https の絶対 URL のみ許可する。
 */
@JvmInline
value class ShelterExternalUrl private constructor(
    val value: String,
) {
    init {
        requireValidHttpUrl(value)
    }

    override fun toString(): String = value

    companion object {
        private val ALLOWED_SCHEMES = setOf("http", "https")

        /**
         * 生文字列から [ShelterExternalUrl] を生成する。
         *
         * `null` は `null` を返す。空・空白のみは拒否する。
         */
        fun of(raw: String?): ShelterExternalUrl? {
            if (raw == null) return null
            val trimmed = raw.trim()
            require(trimmed.isNotBlank()) { "externalUrl must not be blank when provided" }
            return ShelterExternalUrl(trimmed)
        }

        private fun requireValidHttpUrl(value: String) {
            val scheme = value.substringBefore(':').lowercase()
            require(scheme in ALLOWED_SCHEMES) {
                "externalUrl must use http or https scheme, got $scheme"
            }
            val uri =
                try {
                    URI(value)
                } catch (e: URISyntaxException) {
                    throw IllegalArgumentException("externalUrl is not a valid URI: $value", e)
                }
            require(!uri.host.isNullOrBlank()) {
                "externalUrl must have a host"
            }
        }
    }
}
