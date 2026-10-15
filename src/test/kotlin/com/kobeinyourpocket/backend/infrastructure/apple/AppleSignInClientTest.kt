package com.kobeinyourpocket.backend.infrastructure.apple

import com.kobeinyourpocket.backend.application.user.auth.AppleTokenException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.URLDecoder
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import java.util.Date
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Sign in with Apple の REST API 呼び出し（#213）。Apple へは MockRestServiceServer で応答する。 */
class AppleSignInClientTest {
    private val keyPair =
        KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    private val privateKey = keyPair.private as ECPrivateKey
    private val publicKey = keyPair.public as ECPublicKey

    /** app.env 向けに改行を `\n` と書いた 1 行の PEM。 */
    private val escapedPem =
        "-----BEGIN PRIVATE KEY-----\\n" +
            Base64
                .getEncoder()
                .encodeToString(privateKey.encoded)
                .chunked(64)
                .joinToString("\\n") +
            "\\n-----END PRIVATE KEY-----"

    private val props =
        AppleSignInProperties(
            clientId = "com.kobeinyourpocket.client",
            teamId = "TEAM123456",
            keyId = "KEY1234567",
            privateKey = escapedPem,
        )

    private val now = Instant.parse("2026-10-15T00:00:00Z")

    private lateinit var server: MockRestServiceServer
    private lateinit var client: AppleSignInClient

    @BeforeTest
    fun setUp() {
        val builder = RestClient.builder().baseUrl("https://appleid.apple.com")
        server = MockRestServiceServer.bindTo(builder).build()
        client = AppleSignInClient(props, builder.build(), Clock.fixed(now, ZoneOffset.UTC))
    }

    private fun appleIdToken(subject: String): String =
        SignedJWT(JWSHeader(JWSAlgorithm.ES256), JWTClaimsSet.Builder().subject(subject).build())
            .apply { sign(ECDSASigner(privateKey)) }
            .serialize()

    private fun formOf(request: MockClientHttpRequest): Map<String, String> =
        request.bodyAsString.split('&').associate { pair ->
            val (key, value) = pair.split('=', limit = 2)
            URLDecoder.decode(key, Charsets.UTF_8) to URLDecoder.decode(value, Charsets.UTF_8)
        }

    @Test
    fun `authorizationCode を交換し、refresh token と ID トークンの sub を返す`() {
        var form = emptyMap<String, String>()
        server
            .expect(requestTo("https://appleid.apple.com/auth/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect { form = formOf(it as MockClientHttpRequest) }
            .andRespond(
                withSuccess(
                    """{"access_token":"at","token_type":"Bearer","expires_in":3600,""" +
                        """"refresh_token":"apple-refresh","id_token":"${appleIdToken("apple-sub")}"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val grant = client.exchangeAuthorizationCode("auth-code")

        server.verify()
        assertEquals("apple-refresh", grant.refreshToken)
        assertEquals("apple-sub", grant.subject)
        assertEquals("com.kobeinyourpocket.client", form["client_id"])
        assertEquals("auth-code", form["code"])
        assertEquals("authorization_code", form["grant_type"])
        assertClientSecret(form.getValue("client_secret"))
    }

    @Test
    fun `refresh token を失効させる`() {
        var form = emptyMap<String, String>()
        server
            .expect(requestTo("https://appleid.apple.com/auth/revoke"))
            .andExpect(method(HttpMethod.POST))
            .andExpect { form = formOf(it as MockClientHttpRequest) }
            .andRespond(withSuccess())

        client.revokeRefreshToken("apple-refresh")

        server.verify()
        assertEquals("com.kobeinyourpocket.client", form["client_id"])
        assertEquals("apple-refresh", form["token"])
        assertEquals("refresh_token", form["token_type_hint"])
        assertClientSecret(form.getValue("client_secret"))
    }

    @Test
    fun `Apple のエラー応答は AppleTokenException になる`() {
        server
            .expect(requestTo("https://appleid.apple.com/auth/token"))
            .andRespond(withBadRequest().body("""{"error":"invalid_grant"}""").contentType(MediaType.APPLICATION_JSON))

        val ex = assertFailsWith<AppleTokenException> { client.exchangeAuthorizationCode("expired-code") }

        assertContains(ex.message.orEmpty(), "status=400")
        assertContains(ex.message.orEmpty(), "invalid_grant")
    }

    @Test
    fun `refresh_token の無い応答は AppleTokenException になる`() {
        server
            .expect(requestTo("https://appleid.apple.com/auth/token"))
            .andRespond(withSuccess("""{"access_token":"at"}""", MediaType.APPLICATION_JSON))

        assertFailsWith<AppleTokenException> { client.exchangeAuthorizationCode("auth-code") }
    }

    @Test
    fun `どれかが未設定なら isConfigured は false で、呼び出すと AppleTokenException`() {
        val unconfigured = AppleSignInClient(props.copy(privateKey = ""), RestClient.create(), Clock.systemUTC())

        assertFalse(unconfigured.isConfigured)
        assertTrue(client.isConfigured)
        assertFailsWith<AppleTokenException> { unconfigured.exchangeAuthorizationCode("auth-code") }
    }

    @Test
    fun `秘密鍵の形式が不正なら AppleTokenException になる`() {
        val broken = AppleSignInClient(props.copy(privateKey = "not-a-key"), RestClient.create(), Clock.systemUTC())

        assertFailsWith<AppleTokenException> { broken.revokeRefreshToken("apple-refresh") }
    }

    @Test
    fun `秘密鍵はヘッダーを除いた Base64 本体でも読める`() {
        val raw = Base64.getEncoder().encodeToString(privateKey.encoded)

        assertEquals(privateKey, AppleSignInClient.parsePrivateKey(raw))
        assertEquals(privateKey, AppleSignInClient.parsePrivateKey(escapedPem))
    }

    @Test
    fun `ID トークンとして読めない値の sub は null`() {
        assertEquals("apple-sub", client.subjectOf(appleIdToken("apple-sub")))
        assertNull(client.subjectOf("not-a-jwt"))
    }

    /** Apple が求める client secret の形（ES256・kid・iss=Team ID・sub=Bundle ID・aud=Apple）。 */
    private fun assertClientSecret(clientSecret: String) {
        val jwt = SignedJWT.parse(clientSecret)
        assertTrue(jwt.verify(ECDSAVerifier(publicKey)))
        assertEquals(JWSAlgorithm.ES256, jwt.header.algorithm)
        assertEquals("KEY1234567", jwt.header.keyID)
        assertEquals("TEAM123456", jwt.jwtClaimsSet.issuer)
        assertEquals("com.kobeinyourpocket.client", jwt.jwtClaimsSet.subject)
        assertEquals(listOf("https://appleid.apple.com"), jwt.jwtClaimsSet.audience)
        assertEquals(Date.from(now), jwt.jwtClaimsSet.issueTime)
        assertTrue(jwt.jwtClaimsSet.expirationTime.after(Date.from(now)))
    }
}
