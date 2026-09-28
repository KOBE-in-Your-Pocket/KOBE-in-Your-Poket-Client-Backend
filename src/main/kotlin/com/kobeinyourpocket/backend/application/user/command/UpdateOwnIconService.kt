package com.kobeinyourpocket.backend.application.user.command

import com.kobeinyourpocket.backend.application.media.ImageNormalizer
import com.kobeinyourpocket.backend.application.media.MediaKeyPrefix
import com.kobeinyourpocket.backend.application.media.MediaStorage
import com.kobeinyourpocket.backend.application.media.command.UploadMediaService
import com.kobeinyourpocket.backend.domain.user.model.User
import com.kobeinyourpocket.backend.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.util.unit.DataSize
import java.util.UUID

/**
 * 本人がアイコン画像を差し替えるユースケース（#184）。
 *
 * 運営向けの [UploadMediaService]（`POST /api/v1/media/uploads`）と違い、
 * **アップロードと設定を 1 回で終える**。分けると「アップロードしたが設定されなかった」画像が
 * 残り、その清理を Client の振る舞いに依存することになる。ここで完結させれば、
 * 保存に失敗した画像だけが未確定のまま期限切れで消える。
 *
 * 更新対象は呼び出し側が JWT の subject から解決した [User.Id] で、ここでは本人判定をしない
 * （[UpdateOwnProfileService] と同じ理由）。
 *
 * **画像の確定**: 保存の前に新画像を確定し、トランザクションの決着後に不要になった方を
 * 清理対象へ戻す。コミットされたら旧画像、ロールバックしたら新画像。差し戻しをコミット前に
 * 行うと、その後ロールバックしたときに現役の画像を消してしまう。
 * （同じ手順をマナー項目の更新でも取っている。共通化していないのは、清理の対象が
 * ユースケースごとに違い、無理にまとめると「どちらを戻すか」が読めなくなるため）
 *
 * **同時実行**: 行ロックは取らない。同じ利用者が 2 枚同時に送った場合、後勝ちで片方の画像が
 * 参照されないまま確定済みで残る。アイコン 1 枚の取りこぼしであり、行ロックを増やすほどの
 * 不都合ではないと判断した。
 */
@Service
class UpdateOwnIconService(
    private val userRepository: UserRepository,
    private val updateOwnProfileService: UpdateOwnProfileService,
    private val mediaStorage: MediaStorage,
    private val imageNormalizer: ImageNormalizer,
    @param:Value("\${media.icon.max-file-size}") maxFileSize: DataSize,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /** アイコン 1 枚に許すアップロードサイズ。multipart の上限とは別に、より小さく絞る。 */
    private val maxBytes: Long = maxFileSize.toBytes()

    /**
     * @param bytes アップロードされた画像のバイト列
     * @throws UserNotFoundException プロフィール行が無い場合
     * @throws IllegalArgumentException 空・大きすぎる・画像として読めない場合
     */
    @Transactional
    fun execute(
        userId: User.Id,
        bytes: ByteArray,
    ): User {
        require(bytes.isNotEmpty()) { "empty file" }
        require(bytes.size.toLong() <= maxBytes) { "icon file too large (max $maxBytes bytes)" }

        // 画像を S3 へ置く前に対象を確かめる。存在しない利用者のために置いた画像は誰も消せない。
        // ここで読んだ現在のアイコンは、差し替え後に清理する旧画像の URL に使う。
        val current = userRepository.findById(userId) ?: throw UserNotFoundException(userId)

        val normalized = imageNormalizer.normalize(bytes, MAX_EDGE_PX)
        // 運営が上げた画像（uploads/）と混ざらないよう、利用者のアイコンは icons/ 配下に置く。
        val key = MediaKeyPrefix.ICONS.keyFor("${UUID.randomUUID()}.${normalized.extension}")
        val iconUrl =
            mediaStorage.store(
                key = key,
                bytes = normalized.bytes,
                contentType = normalized.contentType,
            )

        // 確定は保存の前。保存が失敗したら下の同期が新画像を清理対象へ戻す。
        mediaStorage.commit(iconUrl)
        // save より前に登録する（save が投げるとフックを張れなくなるため）。
        releaseAfterCompletion(
            onCommit = current.icon?.url?.takeIf { it != iconUrl },
            onRollback = iconUrl,
        )

        // プロフィール行への書き込みは UpdateOwnProfileService に通す（更新経路を 1 本に保つ）。
        return updateOwnProfileService.execute(
            userId = userId,
            icon = UpdateOwnProfileService.IconUpdate.Set(iconUrl),
        )
    }

    /**
     * トランザクションの決着後に、不要になった方の画像を staging へ戻す。
     *
     * @param onCommit コミット時に戻す URL（＝もう参照されない旧画像）。アイコン未設定なら null
     * @param onRollback ロールバック時に戻す URL（＝保存されなかった新画像）
     */
    private fun releaseAfterCompletion(
        onCommit: String?,
        onRollback: String?,
    ) {
        if (onCommit == null && onRollback == null) return

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCompletion(status: Int) {
                    val target =
                        when (status) {
                            TransactionSynchronization.STATUS_COMMITTED -> onCommit
                            TransactionSynchronization.STATUS_ROLLED_BACK -> onRollback
                            // 決着不明。どちらを戻しても現役を消す恐れがあるため何もしない。
                            else -> null
                        }
                    target?.let { releaseQuietly(it) }
                }
            },
        )
    }

    /**
     * 差し戻しの失敗でユースケースを壊さない（この時点でトランザクションは決着済み）。
     * 取りこぼしても画像 1 件で、ストレージ側の突合で回収できる。
     */
    private fun releaseQuietly(imageUrl: String) {
        runCatching { mediaStorage.release(imageUrl) }
            .onFailure { logger.error("failed to release media: {}", imageUrl, it) }
    }

    companion object {
        /**
         * 出力の長辺（px）。Client の表示は編集画面で 96pt・レビュー欄で 40pt なので、
         * Retina（3x）でも足りる大きさに収める。
         */
        internal const val MAX_EDGE_PX = 512
    }
}
