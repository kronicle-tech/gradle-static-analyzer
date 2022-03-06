package tech.kronicle.gradlestaticanalyzer;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;
import static tech.kronicle.gradlestaticanalyzer.TestDependencyFactory.newGradleStaticAnalyzer;

public class GradleStaticAnalyzerErrorTest extends BaseGradleStaticAnalyzerTest {

    private final GradleStaticAnalyzer underTest = newGradleStaticAnalyzer(this.getClass());

    @Test
    public void shouldScanBuildscriptMissingRepositoryBuild() {
        // Given
        Path codebaseDir = getResourcesDir("BuildscriptMissingRepository");

        // When
        Exception exception = catchException(() -> underTest.analyzeCodebase(codebaseDir));

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).hasMessageMatching("^Failed to process build file \\\"[^\\\"]+/src/test/resources/tech/kronicle/gradlestaticanalyzer/GradleStaticAnalyzerErrorTest/BuildscriptMissingRepository/build.gradle\\\" for THIS_PROJECT project mode and BUILDSCRIPT_DEPENDENCIES process phase$");
        assertThat(exception.getCause()).hasMessage("Failed to create software item for artifact");
        assertThat(exception.getCause().getCause()).hasMessage("No safe repositories configured");
    }

    @Test
    public void shouldScanBuildscriptMissingRepositoryWithEmptyPluginsBuild() {
        // Given
        Path codebaseDir = getResourcesDir("BuildscriptMissingRepositoryWithEmptyPlugins");

        // When
        Exception exception = catchException(() -> underTest.analyzeCodebase(codebaseDir));

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).hasMessageMatching("^Failed to process build file \\\"[^\\\"]+/src/test/resources/tech/kronicle/gradlestaticanalyzer/GradleStaticAnalyzerErrorTest/BuildscriptMissingRepositoryWithEmptyPlugins/build.gradle\\\" for THIS_PROJECT project mode and BUILDSCRIPT_DEPENDENCIES process phase$");
        assertThat(exception.getCause()).hasMessage("Failed to create software item for artifact");
        assertThat(exception.getCause().getCause()).hasMessage("No safe repositories configured");
    }
}
