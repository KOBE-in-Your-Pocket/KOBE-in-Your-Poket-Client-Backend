package com.kobeinyourpocket.backend.architecture

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * user コンテキストの Onion 層依存ルール（docs/architecture.md §6 / #88）。
 *
 * User は支援コンテキストで application 省略可だが、domain 純粋性と
 * REST → application 経由の依存方向は他 feature と同様に守る。
 */
@AnalyzeClasses(
    packages = ["com.kobeinyourpocket.backend"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class UserLayerArchitectureTest {
    @ArchTest
    val domainShouldNotDependOnApplicationOrInfrastructure =
        noClasses()
            .that()
            .resideInAnyPackage("..domain.user..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..application..", "..infrastructure..")
            .because("domain は純粋 Kotlin。外側レイヤへ依存しない（§2）")

    @ArchTest
    val domainShouldNotDependOnSpringOrJpa =
        noClasses()
            .that()
            .resideInAnyPackage("..domain.user..")
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
            .resideInAPackage("..application.user..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .because("application は domain port 経由のみ。adapter へ直接依存しない（§2）")

    @ArchTest
    val restShouldNotDependOnPersistenceOrQueryAdapters =
        noClasses()
            .that()
            .resideInAPackage("..infrastructure.rest.user..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..infrastructure.persistence.user..",
                "..infrastructure.query.user..",
            ).because("REST は application 経由。persistence / query adapter を直叩きしない（§2）")
}
