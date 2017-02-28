package com.tngtech.archunit.lang.syntax.elements;

import java.util.Comparator;

import com.tngtech.archunit.core.ClassFileImporter;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.FailureReport;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ClassesShouldConjunctionTest {
    @DataProvider
    public static Object[][] ORed_conditions() {
        return $$(
                $(classes()
                        .should().beNamed(RightOne.class.getName())
                        .orShould(ArchConditions.beNamed(RightTwo.class.getName()))),
                $(classes()
                        .should().beNamed(RightOne.class.getName())
                        .orShould().beNamed(RightTwo.class.getName())));
    }

    @Test
    @UseDataProvider("ORed_conditions")
    public void orShould_ORs_conditions(ArchRule rule) {
        EvaluationResult result = rule
                .evaluate(importClasses(RightOne.class, RightTwo.class, Wrong.class));

        FailureReport report = result.getFailureReport();
        assertThat(report.toString())
                .contains(String.format(
                        "classes should be named '%s' or should be named '%s'",
                        RightOne.class.getName(), RightTwo.class.getName()));
        assertThat(report.getDetails()).containsOnly(
                String.format("class %s is not named '%s' and class %s is not named '%s'",
                        Wrong.class.getName(), RightOne.class.getName(),
                        Wrong.class.getName(), RightTwo.class.getName()));
    }

    @DataProvider
    public static Object[][] ORed_conditions_that() {
        return $$(
                $(noClasses()
                        .should().access().classesThat().areNamed(Wrong.class.getName())
                        .orShould(ArchConditions.beNamed(Wrong.class.getName()))),
                $(noClasses()
                        .should().access().classesThat().areNamed(Wrong.class.getName())
                        .orShould().beNamed(Wrong.class.getName())));
    }

    @Test
    @UseDataProvider("ORed_conditions_that")
    public void orShould_ORs_conditions_that(ArchRule rule) {
        EvaluationResult result = rule
                .evaluate(importClasses(RightOne.class, RightTwo.class, Wrong.class, OtherWrong.class));

        FailureReport report = result.getFailureReport();
        assertThat(report.toString())
                .contains(String.format(
                        "no classes should access classes that are named '%s' or should be named '%s'",
                        Wrong.class.getName(), Wrong.class.getName()));
        assertThat(report.getDetails()).usingElementComparator(matchesRegex()).contains(
                String.format("class %s is named '%s'",
                        quote(Wrong.class.getName()), quote(Wrong.class.getName())),
                String.format("Method <%s.call\\(\\)> calls constructor <%s.<init>\\(.*\\)>.*",
                        quote(OtherWrong.class.getName()), quote(Wrong.class.getName())));
    }

    private Comparator<String> matchesRegex() {
        return new Comparator<String>() {
            @Override
            public int compare(String element, String assertAgainst) {
                return element.matches(assertAgainst) ? 0 : element.compareTo(assertAgainst);
            }
        };
    }

    private JavaClasses importClasses(Class<?>... classes) {
        return new ClassFileImporter().importClasses(classes);
    }

    private static class RightOne {
    }

    private static class RightTwo {
    }

    private static class Wrong {
    }

    private static class OtherWrong {
        void call() {
            new Wrong();
        }
    }
}