package com.kobeinyourpocket.backend.infrastructure.rest.tourism

import com.kobeinyourpocket.backend.application.tourism.command.DeleteGenreService
import com.kobeinyourpocket.backend.application.tourism.command.RegisterGenreService
import com.kobeinyourpocket.backend.application.tourism.command.UpdateGenreService
import com.kobeinyourpocket.backend.application.tourism.query.ListGenresService
import com.kobeinyourpocket.backend.domain.tourism.genre.vo.GenreCode
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * ジャンルマスタの REST inbound adapter（#153）。
 *
 * ジャンルはスポットの絞り込み区分。これまで `spot.genre` の文字列だけが存在し、表示名は
 * ADMIN と Client がそれぞれハードコードしていたため、運営が追加してもラベルが出せなかった。
 *
 * 一覧は Client のジャンルフィルタが使うため公開。書き込みは運営ロール限定
 * （ロール階層で ADMIN も通る）。
 */
@RestController
@RequestMapping("/api/v1/tourism/genres")
class GenreController(
    private val listGenresService: ListGenresService,
    private val registerGenreService: RegisterGenreService,
    private val updateGenreService: UpdateGenreService,
    private val deleteGenreService: DeleteGenreService,
) {
    /** 表示名は全言語まとめて返す（[GenreResponse] 参照）。並びは displayOrder 順。 */
    @GetMapping
    fun listGenres(): List<GenreResponse> = listGenresService.listGenres().map(GenreResponse::from)

    /** code は英語ラベルから自動生成する。運営には入力させない。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OPERATOR')")
    fun registerGenre(
        @Valid @RequestBody request: GenreRequest,
    ): GenreResponse =
        GenreResponse.from(
            registerGenreService.registerGenre(
                displayOrder = request.displayOrder,
                localizations = request.toLocalizations(),
            ),
        )

    /** 変更できるのは表示名と並び順のみ。code はパスの値のまま。 */
    @PutMapping("/{code}")
    @PreAuthorize("hasRole('OPERATOR')")
    fun updateGenre(
        @PathVariable code: String,
        @Valid @RequestBody request: GenreRequest,
    ): GenreResponse =
        GenreResponse.from(
            updateGenreService.updateGenre(
                code = GenreCode.of(code),
                displayOrder = request.displayOrder,
                localizations = request.toLocalizations(),
            ),
        )

    /** 使用中のジャンルは削除できない（409）。運営は先にスポットのジャンルを付け替える。 */
    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('OPERATOR')")
    fun deleteGenre(
        @PathVariable code: String,
    ): ResponseEntity<Void> {
        deleteGenreService.deleteGenre(GenreCode.of(code))
        return ResponseEntity.noContent().build()
    }
}
