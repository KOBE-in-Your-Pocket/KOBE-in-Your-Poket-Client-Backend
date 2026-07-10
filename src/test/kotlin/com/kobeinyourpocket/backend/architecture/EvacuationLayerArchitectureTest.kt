package com.kobeinyourpocket.backend.architecture

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * evacuation コンテキストの Onion 層依存ルール（docs/architecture.md §6 / #67）。
 *
 * [TourismLayerArchitectureTest] / [MannerLayerArchitectureTest] と同じ4ルールを evacuation 固有パッケージに適用する。
 * domain 純粋性の2ルールが共用する `domain.common`（tourism/evacuation/manner が共有する
 * 汎用ドメイン、例: `Language`）は tourism 側テストで既にカバー済みのため、ここでは
 * `..domain.evacuation..` のみを対象にして重複を避ける。
 *
 * プロトタイプ期の warn 運用は CI 側で段階導入する。ルール自体は本番同等に fail させる。
 */
@AnalyzeClasses(
    packages = ["com.kobeinyourpocket.backend"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class EvacuationLayerArchitectureTest {
    @ArchTest
    val domainShouldNotDependOnApplicationOrInfrastructure =
        noClasses()
            .that()
            .resideInAnyPackage("..domain.evacuation..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..application..", "..infrastructure..")
            .because("domain は純粋 Kotlin。外側レイヤへ依存しない（§2）")

    @ArchTest
    val domainShouldNotDependOnSpringOrJpa =
        noClasses()
            .that()
            .resideInAnyPackage("..domain.evacuation..")
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
            .resideInAPackage("..application.evacuation..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .because("application は domain port 経由のみ。adapter へ直接依存しない（§2）")

    @ArchTest
    val restShouldNotDependOnPersistenceOrQueryAdapters =
        noClasses()
            .that()
            .resideInAPackage("..infrastructure.rest.evacuation..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..infrastructure.persistence.evacuation..",
                "..infrastructure.query.evacuation..",
            ).because("REST は application 経由。persistence / query adapter を直叩きしない（§2）")
}
