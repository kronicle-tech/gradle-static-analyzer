package tech.kronicle.plugins.gradle;

import static tech.kronicle.plugins.gradle.config.GradleStaticAnalyzerConfigTestFactory.newGradleStaticAnalyzerConfig;

public final class TestDependencyFactory {

    public static GradleStaticAnalyzer newGradleStaticAnalyzer(Class<?> testClass) {
        return GradleStaticAnalyzerFactory.newGradleStaticAnalyzer(
                newGradleStaticAnalyzerConfig(testClass)
        );
    }

    private TestDependencyFactory() {
    }
}
