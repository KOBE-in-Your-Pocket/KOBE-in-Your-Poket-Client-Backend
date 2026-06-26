package com.kobeinyourpocket.backend.domain.contentsubmission.model

/**
 * 集約ルート・値オブジェクト・不変条件。純粋 Kotlin（Spring / JPA 禁止）。
 *
 * 型設計は Client Mock API スキーマを正とする（未整備なら Client domain 型追加後に追随）。
 *
 * @see docs/architecture.md §7.0
 * @see KOBE-in-Your-Poket-Client `src/features/contentsubmission/infrastructure/api/mock-*.ts`
 * @see KOBE-in-Your-Poket-Client `src/features/contentsubmission/domain/`
 */
internal object DomainModelLayer
