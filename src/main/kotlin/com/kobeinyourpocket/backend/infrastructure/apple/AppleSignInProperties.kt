package com.kobeinyourpocket.backend.infrastructure.apple

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Sign in with Apple の REST API 呼び出し用（#213）。
 *
 * client secret（JWT）は [teamId] / [keyId] / [privateKey] から都度生成する。
 * いずれかが空なら [AppleSignInClient.isConfigured] が false になり、
 * サインイン時の refresh token 保存と退会時の失効を行わない（起動は妨げない）。
 */
@ConfigurationProperties(prefix = "apple.sign-in")
data class AppleSignInProperties(
    /** ネイティブアプリの Bundle ID。id_token の `aud` と同じ値（Supabase の Client IDs と一致させる）。 */
    val clientId: String = "",
    /** Apple Developer の Team ID。 */
    val teamId: String = "",
    /** Sign in with Apple を有効にした Key の ID。 */
    val keyId: String = "",
    /**
     * 上記 Key の秘密鍵（.p8）の中身。
     * env_file は複数行を扱えないため、改行を `\n` と書いた 1 行の PEM か、ヘッダーを除いた Base64 本体を受け付ける。
     */
    val privateKey: String = "",
) {
    // 秘密鍵をログへ出さない。
    override fun toString(): String = "AppleSignInProperties(clientId=$clientId, teamId=$teamId, keyId=$keyId)"
}
