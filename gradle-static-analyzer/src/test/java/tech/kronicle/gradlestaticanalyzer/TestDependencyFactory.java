package tech.kronicle.gradlestaticanalyzer;

import static tech.kronicle.gradlestaticanalyzer.config.GradleStaticAnalyzerConfigTestFactory.newGradleStaticAnalyzerConfig;

public final class TestDependencyFactory {

    public static GradleStaticAnalyzer newGradleStaticAnalyzer(Class<?> testClass) {
        return GradleStaticAnalyzerFactory.newGradleStaticAnalyzer(
                newGradleStaticAnalyzerConfig(testClass)
        );
    }

    private TestDependencyFactory() {
    }
}
