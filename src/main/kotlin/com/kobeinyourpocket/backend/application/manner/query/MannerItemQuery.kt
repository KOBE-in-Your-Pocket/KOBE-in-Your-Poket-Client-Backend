package com.kobeinyourpocket.backend.application.manner.query

import com.kobeinyourpocket.backend.domain.common.localization.Language

/** read 専用 port。application が定義し infrastructure.query が実装する。 */
interface MannerItemQuery {
    fun findAllResolved(language: Language): List<MannerItemView>
}
