package com.kobeinyourpocket.backend.infrastructure.apple

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.kobeinyourpocket.backend.application.user.auth.AppleTokenException
import com.kobeinyourpocket.backend.application.user.auth.AppleTokenGateway
import com.kobeinyourpocket.backend.application.user.auth.AppleTokenGrant
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.text.ParseException
import java.time.Clock
import java.time.Duration
import java.util.Base64
import java.util.Date

/**
 * Sign in with Apple の REST API クライアント（#213）。
 *
 * - `/auth/token`: Client から受け取った authorizationCode を refresh token に交換する
 * - `/auth/revoke`: 退会時に refresh token を失効させる
 *
 * どちらも client secret（Team ID / Key ID / .p8 で ES256 署名した JWT）が要る。
 * client secret は呼び出しのたびに短い有効期限で生成する（保管しない）。
 */
@Component
class AppleSignInClient internal constructor(
    private val props: AppleSignInProperties,
    private val restClient: RestClient,
    private val clock: Clock,
) : AppleTokenGateway {
    @Autowired
    constructor(props: AppleSignInProperties) : this(props, defaultRestClient(), Clock.systemUTC())

    private val privateKey: ECPrivateKey by lazy { parsePrivateKey(props.privateKey) }

    override val isConfigured: Boolean
        get() = listOf(props.clientId, props.teamId, props.keyId, props.privateKey).all { it.isNotBlank() }

    override fun exchangeAuthorizationCode(authorizationCode: String): AppleTokenGrant {
        val response =
            postForm(
                path = "/auth/token",
                form =
                    formOf(
                        "client_id" to props.clientId,
                        "client_secret" to clientSecret(),
                        "code" to authorizationCode,
                        "grant_type" to "authorization_code",
                    ),
            ) { it.body(AppleTokenResponse::class.java) }
                ?: throw AppleTokenException("Apple /auth/token returned an empty response")

        val refreshToken =
            response.refreshToken?.takeIf { it.isNotBlank() }
                ?: throw AppleTokenException("Apple /auth/token response has no refresh_token")
        return AppleTokenGrant(
            refreshToken = refreshToken,
            subject = response.idToken?.let(::subjectOf),
        )
    }

    override fun revokeRefreshToken(refreshToken: String) {
        postForm(
            path = "/auth/revoke",
            form =
                formOf(
                    "client_id" to props.clientId,
                    "client_secret" to clientSecret(),
                    "token" to refreshToken,
                    "token_type_hint" to "refresh_token",
                ),
        ) { it.toBodilessEntity() }
    }

    override fun subjectOf(idToken: String): String? =
        try {
            SignedJWT.parse(idToken).jwtClaimsSet.subject
        } catch (ex: ParseException) {
            null
        }

    /** Apple が要求する client secret。有効期限は最長 6 か月だが、都度生成するため短くする。 */
    internal fun clientSecret(): String {
        if (!isConfigured) throw AppleTokenException("Sign in with Apple の Team ID / Key ID / 秘密鍵が未設定")
        val now = clock.instant()
        val claims =
            JWTClaimsSet
                .Builder()
                .issuer(props.teamId)
                .subject(props.clientId)
                .audience(APPLE_ISSUER)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(CLIENT_SECRET_TTL)))
                .build()
        val header = JWSHeader.Builder(JWSAlgorithm.ES256).keyID(props.keyId).build()
        return try {
            SignedJWT(header, claims).apply { sign(ECDSASigner(privateKey)) }.serialize()
        } catch (ex: Exception) {
            // 鍵の形式不正（InvalidKeySpecException 等）や署名失敗は検査例外のため、呼び出し側が捕まえられる形にする。
            throw AppleTokenException("Apple の client secret を生成できなかった: ${ex.javaClass.simpleName}", ex)
        }
    }

    private fun <T> postForm(
        path: String,
        form: MultiValueMap<String, String>,
        read: (RestClient.ResponseSpec) -> T,
    ): T =
        try {
            read(
                restClient
                    .post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve(),
            )
        } catch (ex: RestClientResponseException) {
            // Apple のエラー本文は {"error":"invalid_grant"} 程度で token を含まない。
            throw AppleTokenException(
                "Apple $path failed: status=${ex.statusCode.value()}, body=${ex.responseBodyAsString.take(200)}",
                ex,
            )
        } catch (ex: RestClientException) {
            throw AppleTokenException("Apple $path failed: ${ex.javaClass.simpleName}", ex)
        }

    private fun formOf(vararg pairs: Pair<String, String>): MultiValueMap<String, String> =
        LinkedMultiValueMap<String, String>().apply { pairs.forEach { (key, value) -> add(key, value) } }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AppleTokenResponse(
        @JsonProperty("refresh_token") val refreshToken: String? = null,
        @JsonProperty("id_token") val idToken: String? = null,
    ) {
        override fun toString(): String = "AppleTokenResponse"
    }

    companion object {
        private const val APPLE_ISSUER = "https://appleid.apple.com"
        private val CLIENT_SECRET_TTL: Duration = Duration.ofMinutes(5)

        private fun defaultRestClient(): RestClient =
            RestClient
                .builder()
                .baseUrl(APPLE_ISSUER)
                .requestFactory(
                    SimpleClientHttpRequestFactory().apply {
                        setConnectTimeout(Duration.ofSeconds(5))
                        setReadTimeout(Duration.ofSeconds(10))
                    },
                ).build()

        /**
         * .p8（PKCS#8 の PEM）を読む。改行を `\n` と書いた 1 行の PEM と、ヘッダーを除いた Base64 本体を受け付ける。
         */
        internal fun parsePrivateKey(raw: String): ECPrivateKey {
            val base64 =
                raw
                    .replace("\\n", "\n")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .filterNot { it.isWhitespace() }
            val der = Base64.getDecoder().decode(base64)
            return KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(der)) as ECPrivateKey
        }
    }
}
