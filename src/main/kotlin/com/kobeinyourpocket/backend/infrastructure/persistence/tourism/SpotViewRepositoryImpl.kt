package com.kobeinyourpocket.backend.infrastructure.persistence.tourism

import com.kobeinyourpocket.backend.domain.tourism.view.model.SpotView
import com.kobeinyourpocket.backend.domain.tourism.view.repository.SpotViewRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/** [SpotViewRepository] port の outbound adapter。 */
@Repository
class SpotViewRepositoryImpl(
    private val spotViewJpa: SpotViewJpaRepository,
) : SpotViewRepository {
    /**
     * 毎回新しい行を INSERT するだけなので、同時に記録されても数がずれない（#240）。
     * 識別子は毎回新しく採番するため、`save` が既存行の更新（merge）になることもない。
     */
    @Transactional
    override fun record(view: SpotView) {
        spotViewJpa.saveAndFlush(SpotViewEntity.fromDomain(view))
    }
}
