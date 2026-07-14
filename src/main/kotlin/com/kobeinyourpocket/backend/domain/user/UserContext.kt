package com.kobeinyourpocket.backend.domain.user

/**
 * User（認証・ロール） — bounded context マーカー。
 *
 * 配置: domain/user · application/user · infrastructure/{persistence,rest}/user
 * 認証・ロールの正は Supabase Auth / JWT。本コンテキストはプロフィール集約を担う。
 *
 * @see docs/architecture.md §3
 */
internal object UserContext
