package com.kobeinyourpocket.backend.infrastructure.persistence.user

import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.model.UserBlock
import com.kobeinyourpocket.backend.domain.user.repository.UserBlockRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/** [UserBlockRepository] port の outbound adapter。 */
@Repository
class UserBlockRepositoryImpl(
    private val userBlockJpa: UserBlockJpaRepository,
) : UserBlockRepository {
    /**
     * 通常の重複は事前の存在確認で弾き、同時送信ですり抜けた分は主キー違反で弾く。
     *
     * このメソッド自体はトランザクションを張らない。張ると主キー違反の時点で
     * トランザクションが rollback-only になり、捕まえてもコミット時に例外になるため
     * （[com.kobeinyourpocket.backend.infrastructure.persistence.report.ReportRepositoryImpl] と同じ）。
     */
    override fun saveIfAbsent(block: UserBlock) {
        val entity = UserBlockEntity.fromDomain(block)
        if (userBlockJpa.existsById(entity.id)) return
        try {
            userBlockJpa.saveAndFlush(entity)
        } catch (e: DataIntegrityViolationException) {
            // 同時に同じ相手をブロックした。結果は同じなので成功として扱う。
        }
    }

    @Transactional
    override fun delete(
        blockerId: User.Id,
        blockedId: User.Id,
    ) {
        userBlockJpa.deleteById(UserBlockId(blockerUserId = blockerId.value, blockedUserId = blockedId.value))
    }

    @Transactional
    override fun deleteByUserId(userId: User.Id): Int = userBlockJpa.deleteByUserId(userId.value)
}
