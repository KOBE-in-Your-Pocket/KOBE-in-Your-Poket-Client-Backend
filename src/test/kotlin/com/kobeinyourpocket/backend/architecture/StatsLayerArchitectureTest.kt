package com.kobeinyourpocket.backend.architecture

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * stats コンテキストの Onion 層依存ルール（docs/architecture.md §6 / #169）。
 *
 * stats は集計の読み取り専用コンテキストで domain を持たない（不変条件を守る対象が無い）。
 * そのぶん「application は port 経由のみ」「REST は adapter を直接触らない」を機械的に守る。
 */
@AnalyzeClasses(
    packages = ["com.kobeinyourpocket.backend"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class StatsLayerArchitectureTest {
    @ArchTest
    val applicationShouldNotDependOnInfrastructure =
        noClasses()
            .that()
            .resideInAPackage("..application.stats..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .because("application は port 経由のみ。adapter へ直接依存しない（§2）")

    @ArchTest
    val restShouldNotDependOnPersistenceOrQueryAdapters =
        noClasses()
            .that()
            .resideInAPackage("..infrastructure.rest.stats..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..infrastructure.persistence..", "..infrastructure.query..")
            .because("REST は application 経由で読む。query adapter へ直接依存しない（§2）")

    @ArchTest
    val applicationShouldNotDependOnOtherContextsDomain =
        noClasses()
            .that()
            .resideInAPackage("..application.stats..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..domain.tourism..", "..domain.user..", "..domain.evacuation..", "..domain.manner..")
            .because("stats は集計の read 専用。他コンテキストの集約に依存しない（CQRS-lite）")
}
