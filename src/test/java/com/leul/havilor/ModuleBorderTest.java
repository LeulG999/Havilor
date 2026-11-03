package com.leul.havilor;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

// The border rule (CLAUDE.md), checked on the compiled main classes by ArchUnit.
// A module is reached from outside only through its service/ and api/response/ packages:
// never its domain (entities, enums), repository, controllers or request DTOs.
@AnalyzeClasses(packages = "com.leul.havilor", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBorderTest {

    @ArchTest
    static final ArchRule nobodyOutsideAlertUsesItsDomainOrRepository = noClasses()
            .that().resideOutsideOfPackage("com.leul.havilor.alert..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.leul.havilor.alert.domain..", "com.leul.havilor.alert.repository..");

    @ArchTest
    static final ArchRule nobodyOutsidePlaybookUsesItsDomainOrRepository = noClasses()
            .that().resideOutsideOfPackage("com.leul.havilor.playbook..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.leul.havilor.playbook.domain..", "com.leul.havilor.playbook.repository..");

    @ArchTest
    static final ArchRule alertIsReachedOnlyThroughServiceAndResponses = onlyThroughServiceAndResponses("alert");

    @ArchTest
    static final ArchRule playbookIsReachedOnlyThroughServiceAndResponses = onlyThroughServiceAndResponses("playbook");

    private static ArchRule onlyThroughServiceAndResponses(String module) {
        String root = "com.leul.havilor." + module;
        return noClasses()
                .that().resideOutsideOfPackage(root + "..")
                .should().dependOnClassesThat(resideInAPackage(root + "..")
                        .and(not(resideInAnyPackage(root + ".service..", root + ".api.response.."))))
                .because("other modules may use only " + module + "'s service and api.response packages")
                .allowEmptyShould(true);
    }
}
