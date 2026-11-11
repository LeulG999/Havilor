package com.leul.havilor;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

// The border rule (CLAUDE.md), checked on the compiled main classes by ArchUnit.
// A module is reached from outside only through its service/ and api/response/ packages:
// never its domain (entities, enums), repository, controllers or request DTOs.
// And no two modules may depend on each other in a circle.
@AnalyzeClasses(packages = "com.leul.havilor", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBorderTest {

    @ArchTest
    static final ArchRule alert = onlyThroughServiceAndResponses("alert");

    @ArchTest
    static final ArchRule playbook = onlyThroughServiceAndResponses("playbook");

    @ArchTest
    static final ArchRule user = onlyThroughServiceAndResponses("user");

    @ArchTest
    static final ArchRule finding = onlyThroughServiceAndResponses("finding");

    @ArchTest
    static final ArchRule escalation = onlyThroughServiceAndResponses("escalation");

    @ArchTest
    static final ArchRule audit = onlyThroughServiceAndResponses("audit");

    @ArchTest
    static final ArchRule standup = onlyThroughServiceAndResponses("standup");

    @ArchTest
    static final ArchRule knowledge = onlyThroughServiceAndResponses("knowledge");

    // Each top-level package (alert, playbook, user, ..., common) is one slice; slices must not form a cycle.
    @ArchTest
    static final ArchRule noCyclesBetweenModules = slices().matching("com.leul.havilor.(*)..")
            .should().beFreeOfCycles();

    // common/ is shared code: it must not know any module.
    @ArchTest
    static final ArchRule commonKnowsNoModule = noClasses()
            .that().resideInAPackage("com.leul.havilor.common..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.leul.havilor.alert..", "com.leul.havilor.playbook..", "com.leul.havilor.user..",
                    "com.leul.havilor.finding..", "com.leul.havilor.escalation..", "com.leul.havilor.audit..",
                    "com.leul.havilor.standup..", "com.leul.havilor.knowledge..");

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
