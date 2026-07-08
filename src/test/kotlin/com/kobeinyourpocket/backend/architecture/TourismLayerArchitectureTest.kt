package com.kobeinyourpocket.backend.architecture

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * tourism コンテキストの Onion 層依存ルール（docs/architecture.md §6 / #25）。
 *
 * domain 純粋性の2ルールは `domain.common`（tourism/evacuation/manner が共用する
 * 汎用ドメイン、例: `Language` / #74）にも適用する。application/infrastructure 側の
 * 2ルールは tourism 固有のため対象外。
 *
 * プロトタイプ期の warn 運用は CI 側で段階導入する。ルール自体は本番同等に fail させる。
 */
@AnalyzeClasses(
    packages = ["com.kobeinyourpocket.backend"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class TourismLayerArchitectureTest {
    @ArchTest
    val domainShouldNotDependOnApplicationOrInfrastructure =
        noClasses()
            .that()
            .resideInAnyPackage("..domain.tourism..", "..domain.common..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..application..", "..infrastructure..")
            .because("domain は純粋 Kotlin。外側レイヤへ依存しない（§2）")

    @ArchTest
    val domainShouldNotDependOnSpringOrJpa =
        noClasses()
            .that()
            .resideInAnyPackage("..domain.tourism..", "..domain.common..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "javax.persistence..",
                "org.hibernate..",
            ).because("domain は Spring / JPA を知らない（§2）")

    @ArchTest
    val applicationShouldNotDependOnInfrastructure =
        noClasses()
            .that()
            .resideInAPackage("..application.tourism..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .because("application は domain port 経由のみ。adapter へ直接依存しない（§2）")

    @ArchTest
    val restShouldNotDependOnPersistenceOrQueryAdapters =
        noClasses()
            .that()
            .resideInAPackage("..infrastructure.rest.tourism..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..infrastructure.persistence.tourism..",
                "..infrastructure.query.tourism..",
            ).because("REST は application 経由。persistence / query adapter を直叩きしない（§2）")
}
