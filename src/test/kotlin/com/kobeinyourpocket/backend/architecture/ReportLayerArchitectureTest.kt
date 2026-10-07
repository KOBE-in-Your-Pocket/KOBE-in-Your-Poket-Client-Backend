package com.kobeinyourpocket.backend.architecture

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * report コンテキストの Onion 層依存ルール（docs/architecture.md §6 / #147）。
 *
 * 通報対象の存在確認で application 層は tourism の domain port を使うが、
 * それは domain への依存なので以下のルールに反しない。
 */
@AnalyzeClasses(
    packages = ["com.kobeinyourpocket.backend"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class ReportLayerArchitectureTest {
    @ArchTest
    val domainShouldNotDependOnApplicationOrInfrastructure =
        noClasses()
            .that()
            .resideInAnyPackage("..domain.report..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..application..", "..infrastructure..")
            .because("domain は純粋 Kotlin。外側レイヤへ依存しない（§2）")

    @ArchTest
    val domainShouldNotDependOnSpringOrJpa =
        noClasses()
            .that()
            .resideInAnyPackage("..domain.report..")
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
            .resideInAPackage("..application.report..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .because("application は domain port 経由のみ。adapter へ直接依存しない（§2）")

    @ArchTest
    val restShouldNotDependOnPersistenceOrQueryAdapters =
        noClasses()
            .that()
            .resideInAPackage("..infrastructure.rest.report..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..infrastructure.persistence..",
                "..infrastructure.query..",
            ).because("REST は application 経由。persistence / query adapter を直叩きしない（§2）")
}
