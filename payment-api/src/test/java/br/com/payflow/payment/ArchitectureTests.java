package br.com.payflow.payment;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

// Guards the Vertical Slice + Clean Architecture rules described in docs/ARQUITETURA_BACKEND.md.
class ArchitectureTests {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("br.com.payflow.payment");

    @Test
    void featureSlicesDoNotDependOnEachOther() {
        slices().matching("br.com.payflow.payment.merchants.(provision|authenticate)..")
                .should().notDependOnEachOther()
                .check(CLASSES);
    }

    @Test
    void domainIsFrameworkFreeAndIndependentOfSlices() {
        noClasses().that().resideInAPackage("..merchants.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "com.mongodb..", "org.bson..",
                        "..merchants.provision..", "..merchants.authenticate..", "..merchants.infrastructure..")
                .check(CLASSES);
    }

    @Test
    void useCasesAndPortsAreFrameworkFree() {
        noClasses().that().resideInAnyPackage("..merchants.provision..", "..merchants.authenticate..")
                .and().haveNameNotMatching(".*(Slice|Controller|Properties)(\\$.*)?")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "com.mongodb..", "org.bson..")
                .check(CLASSES);
    }

    @Test
    void slicesNeverReachInfrastructureDirectly() {
        noClasses().that().resideInAnyPackage("..merchants.provision..", "..merchants.authenticate..")
                .should().dependOnClassesThat().resideInAPackage("..merchants.infrastructure..")
                .check(CLASSES);
    }
}
