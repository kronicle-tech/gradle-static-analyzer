package tech.kronicle.plugins.gradle;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tech.kronicle.sdk.models.Software;
import tech.kronicle.sdk.models.SoftwareDependencyType;
import tech.kronicle.sdk.models.SoftwareRepository;
import tech.kronicle.sdk.models.SoftwareRepositoryScope;
import tech.kronicle.sdk.models.SoftwareRepositoryType;
import tech.kronicle.sdk.models.SoftwareType;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.kronicle.plugins.gradle.TestDependencyFactory.newGradleStaticAnalyzer;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.DEPENDENCY_CHECK_GRADLE_6_0_2_BUILDSCRIPT;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.DEPENDENCY_CHECK_PLUGIN;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.GRADLE_WRAPPER_6_7;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.GROOVY_PLUGIN;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.HIBERNATE_VALIDATOR_4_1_0_FINAL;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.HIBERNATE_VALIDATOR_6_1_6_FINAL;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.JAVA_PLUGIN;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.JTDS_1_3_1;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.LOMBOK_1_18_16;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.MICRONAUT_APPLICATION_PLUGIN_2_0_6;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.MICRONAUT_LIBRARY_PLUGIN_2_0_6;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.MICRONAUT_RUNTIME_3_0_0;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.MICRONAUT_RUNTIME_3_1_0;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.SPRING_BOOT_GRADLE_PLUGIN_2_3_4_RELEASE;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.SPRING_BOOT_PLUGIN_2_3_4_RELEASE;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.SPRING_BOOT_STARTER_ACTUATOR_2_3_4_RELEASE;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.SPRING_BOOT_STARTER_WEB_2_0_9_RELEASE;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE_SOURCES;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.SPRING_CLOUD_STARTER_ZIPKIN_2_2_5_RELEASE;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareItems.SPRING_DEPENDENCY_MANAGEMENT_PLUGIN_1_0_10_RELEASE;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareRepositories.GOOGLE_REPOSITORY;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareRepositories.GRADLE_PLUGIN_PORTAL_REPOSITORY;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareRepositories.JCENTER_REPOSITORY;
import static tech.kronicle.plugins.gradle.internal.testconstants.SoftwareRepositories.MAVEN_CENTRAL_REPOSITORY;

public class GradleStaticAnalyzerCodebaseTest extends BaseGradleStaticAnalyzerTest {

    private final GradleStaticAnalyzer underTest = newGradleStaticAnalyzer(this.getClass());
    private WireMockServer wireMockServer;

    @AfterEach
    public void afterEach() {
        if (nonNull(wireMockServer)) {
            wireMockServer.stop();
            wireMockServer = null;
        }
    }

    @Test
    public void shouldHandleNone() {
        // Given
        Path codebaseDir = getCodebaseDir("None");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isFalse();
        assertThat(gradleAnalysis.getSoftwareRepositories()).isEmpty();
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleEmpty() {
        // Given
        Path codebaseDir = getCodebaseDir("Empty");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).isEmpty();
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleRootProjectBuiltInProperties() {
        // Given
        Path codebaseDir = getCodebaseDir("RootProjectBuiltInProperties");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).isEmpty();
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleProjectBuiltInProperties() {
        // Given
        Path codebaseDir = getCodebaseDir("ProjectBuiltInProperties");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).isEmpty();
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyClass() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyClass");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependency() {
        // Given
        Path codebaseDir = getCodebaseDir("Dependency");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyPackaging() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyPackaging");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                JAVA_PLUGIN,
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE_SOURCES);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyDynamicVersion() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyDynamicVersion");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_0_9_RELEASE.withVersionSelector("2.0+"),
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(6);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyPomXmlWithoutNamespace() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyPomXmlWithoutNamespace");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                JTDS_1_3_1,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(1);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyFollowRedirect() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyFollowRedirect");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                GRADLE_PLUGIN_PORTAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyList() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyList");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_ACTUATOR_2_3_4_RELEASE,
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(7);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleApplyPlugin() {
        // Given
        Path codebaseDir = getCodebaseDir("ApplyPlugin");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                MAVEN_CENTRAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_GRADLE_PLUGIN_2_3_4_RELEASE,
                JAVA_PLUGIN,
                SPRING_BOOT_PLUGIN_2_3_4_RELEASE);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(29);
    }

    @Test
    public void shouldHandleApplyPluginClass() {
        // Given
        Path codebaseDir = getCodebaseDir("ApplyPluginClass");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                MAVEN_CENTRAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT));
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                DEPENDENCY_CHECK_GRADLE_6_0_2_BUILDSCRIPT,
                DEPENDENCY_CHECK_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(4);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleApplyPluginImportedClass() {
        // Given
        Path codebaseDir = getCodebaseDir("ApplyPluginImportedClass");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                MAVEN_CENTRAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT));
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                DEPENDENCY_CHECK_GRADLE_6_0_2_BUILDSCRIPT,
                DEPENDENCY_CHECK_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(4);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandlePluginProperty() {
        // Given
        Path codebaseDir = getCodebaseDir("PluginProperty");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_PLUGIN_2_3_4_RELEASE);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(29);
    }

    @Test
    public void shouldHandlePluginDefinedInSettingsFileWithApplyFalse() {
        // Given
        Path codebaseDir = getCodebaseDir("PluginDefinedInSettingsFileWithApplyFalse");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                MAVEN_CENTRAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT));
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_PLUGIN_2_3_4_RELEASE.withType(SoftwareType.GRADLE_PLUGIN_VERSION));
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandlePluginDefinedInSettingsFileWithApplyFalseAndAppliedInBuildFile() {
        // Given
        Path codebaseDir = getCodebaseDir("PluginDefinedInSettingsFileWithApplyFalseAndAppliedInBuildFile");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                MAVEN_CENTRAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_PLUGIN_2_3_4_RELEASE.withType(SoftwareType.GRADLE_PLUGIN_VERSION),
                SPRING_BOOT_PLUGIN_2_3_4_RELEASE);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(29);
    }

    @Test
    public void shouldHandlePluginDefinedInSettingsFileWithApplyFalseAndDefinedAgainInBuildFile() {
        // Given
        Path codebaseDir = getCodebaseDir("PluginDefinedInSettingsFileWithApplyFalseAndAppliedInBuildFile");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                MAVEN_CENTRAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_PLUGIN_2_3_4_RELEASE.withType(SoftwareType.GRADLE_PLUGIN_VERSION),
                SPRING_BOOT_PLUGIN_2_3_4_RELEASE);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(29);
    }

    @Test
    public void shouldHandlePluginDefinedInSettingsFileWithApplyTrue() {
        // Given
        Path codebaseDir = getCodebaseDir("PluginDefinedInSettingsFileWithApplyTrue");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                MAVEN_CENTRAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_PLUGIN_2_3_4_RELEASE);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(29);
    }

    @Test
    public void shouldHandleSpringBootPlugin() {
        // Given
        Path codebaseDir = getCodebaseDir("SpringBootPlugin");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN,
                SPRING_BOOT_PLUGIN_2_3_4_RELEASE);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(29);
    }

    @Test
    public void shouldHandleSpringBootPluginApplyPlugin() {
        // Given
        Path codebaseDir = getCodebaseDir("SpringBootPluginApplyPlugin");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                MAVEN_CENTRAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_GRADLE_PLUGIN_2_3_4_RELEASE,
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN,
                SPRING_BOOT_PLUGIN_2_3_4_RELEASE);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(10);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(29);
    }

    @Test
    public void shouldHandleSpringBootPluginPluginsAndApplyPlugin() {
        // Given
        Path codebaseDir = getCodebaseDir("SpringBootPluginPluginsAndApplyPlugin");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN,
                SPRING_BOOT_PLUGIN_2_3_4_RELEASE);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(29);
    }

    @Test
    public void shouldHandleGradleWrapper() {
        // Given
        Path codebaseDir = getCodebaseDir("GradleWrapper");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                GRADLE_WRAPPER_6_7,
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyNamedParts() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyNamedParts");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyNamedPartsWithClassifierAndExt() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyNamedPartsWithClassifierAndExt");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyMultipleInOneCall() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyMultipleInOneCall");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_ACTUATOR_2_3_4_RELEASE,
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(7);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyMultipleWithNamedPartsInOneCall() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyMultipleWithNamedPartsInOneCall");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_ACTUATOR_2_3_4_RELEASE,
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(7);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyStringConcatenation() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyStringConcatenation");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyProject() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyProject");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT));
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyLocalGroovy() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyLocalGroovy");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT));
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                GROOVY_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyGradleApi() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyGradleApi");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT));
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                GROOVY_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyFileTree() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyFileTree");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT));
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                GROOVY_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyFiles() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyFiles");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT));
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                GROOVY_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyVariable() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyVariable");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                HIBERNATE_VALIDATOR_6_1_6_FINAL,
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(28);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyVariableInDependencies() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyVariableInDependencies");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                HIBERNATE_VALIDATOR_6_1_6_FINAL,
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(28);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyDuplicates() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyDuplicates");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                LOMBOK_1_18_16);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleDependencyExclusion() {
        // Given
        Path codebaseDir = getCodebaseDir("DependencyExclusion");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_ACTUATOR_2_3_4_RELEASE,
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(7);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleProjectProperties() {
        // Given
        Path codebaseDir = getCodebaseDir("ProjectProperties");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                HIBERNATE_VALIDATOR_6_1_6_FINAL,
                LOMBOK_1_18_16,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(23);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleProjectPropertyAssignment() {
        // Given
        Path codebaseDir = getCodebaseDir("ProjectPropertyAssignment");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                HIBERNATE_VALIDATOR_6_1_6_FINAL,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(23);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleProjectPropertySetMethod() {
        // Given
        Path codebaseDir = getCodebaseDir("ProjectPropertySetMethod");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                HIBERNATE_VALIDATOR_6_1_6_FINAL,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(23);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleBuildscriptProjectProperty() {
        // Given
        Path codebaseDir = getCodebaseDir("BuildscriptProjectProperty");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                HIBERNATE_VALIDATOR_6_1_6_FINAL,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(23);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleBuildscriptProjectPropertyAssignment() {
        // Given
        Path codebaseDir = getCodebaseDir("BuildscriptProjectPropertyAssignment");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                MAVEN_CENTRAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT));
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_GRADLE_PLUGIN_2_3_4_RELEASE);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleBuildscriptDependencyWithNoBuildscriptRepository() {
        // Given
        Path codebaseDir = getCodebaseDir("BuildscriptDependencyWithNoBuildscriptRepository");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT));
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_GRADLE_PLUGIN_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleGradleProperties() {
        // Given
        Path codebaseDir = getCodebaseDir("GradleProperties");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                HIBERNATE_VALIDATOR_6_1_6_FINAL,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(23);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleGradlePropertiesAndProjectProperties() {
        // Given
        Path codebaseDir = getCodebaseDir("GradlePropertiesAndProjectProperties");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                HIBERNATE_VALIDATOR_4_1_0_FINAL,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(20);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleMultiProject() {
        // Given
        Path codebaseDir = getCodebaseDir("MultiProject");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                HIBERNATE_VALIDATOR_4_1_0_FINAL,
                LOMBOK_1_18_16,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(20);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleMultiProjectNestedGradleProperties() {
        // Given
        Path codebaseDir = getCodebaseDir("MultiProjectNestedGradleProperties");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                HIBERNATE_VALIDATOR_4_1_0_FINAL,
                LOMBOK_1_18_16,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(20);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleMultiProjectMissingBuildFile() {
        // Given
        Path codebaseDir = getCodebaseDir("MultiProjectMissingBuildFile");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                LOMBOK_1_18_16,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleMultiProjectSpringBootPlugin() {
        // Given
        Path codebaseDir = getCodebaseDir("MultiProjectSpringBootPlugin");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN,
                SPRING_BOOT_PLUGIN_2_3_4_RELEASE);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(29);
    }

    @Test
    public void shouldHandleMultiProjectSpringBootPluginApplyFalse() {
        // Given
        Path codebaseDir = getCodebaseDir("MultiProjectSpringBootPluginApplyFalse");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN,
                SPRING_BOOT_PLUGIN_2_3_4_RELEASE.withType(SoftwareType.GRADLE_PLUGIN_VERSION),
                SPRING_BOOT_PLUGIN_2_3_4_RELEASE);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(29);
    }

    @Test
    public void shouldHandleMultiProjectInheritedProjectProperties() {
        // Given
        Path codebaseDir = getCodebaseDir("MultiProjectInheritedProjectProperties");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                HIBERNATE_VALIDATOR_4_1_0_FINAL,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(20);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleMultiProjectOverriddenProjectProperties() {
        // Given
        Path codebaseDir = getCodebaseDir("MultiProjectOverriddenProjectProperties");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                HIBERNATE_VALIDATOR_4_1_0_FINAL,
                HIBERNATE_VALIDATOR_6_1_6_FINAL,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(36);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleMultiProjectAllprojects() {
        // Given
        Path codebaseDir = getCodebaseDir("MultiProjectAllprojects");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                HIBERNATE_VALIDATOR_4_1_0_FINAL,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(20);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleMultiProjectSubprojects() {
        // Given
        Path codebaseDir = getCodebaseDir("MultiProjectSubprojects");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                HIBERNATE_VALIDATOR_4_1_0_FINAL,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(20);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleMultiProjectSubprojectsRepositories() {
        // Given
        Path codebaseDir = getCodebaseDir("MultiProjectSubprojectsRepositories");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN,
                SPRING_BOOT_PLUGIN_2_3_4_RELEASE);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(29);
    }

    @Test
    public void shouldHandlePlatformDependency() {
        // Given
        Path codebaseDir = getCodebaseDir("PlatformDependency");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                JAVA_PLUGIN,
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(29);
    }

    @Test
    public void shouldHandleApplyFrom() {
        // Given
        Path codebaseDir = getCodebaseDir("ApplyFrom");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleApplyFromRootDir() {
        // Given
        Path codebaseDir = getCodebaseDir("ApplyFromRootDir");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleApplyFromRootProjectProjectDir() {
        // Given
        Path codebaseDir = getCodebaseDir("ApplyFromRootProjectProjectDir");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleApplyFromProjectDir() {
        // Given
        Path codebaseDir = getCodebaseDir("ApplyFromProjectDir");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleApplyFromProjectDirParent() {
        // Given
        Path codebaseDir = getCodebaseDir("ApplyFromProjectDirParent");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleApplyFromProjectRelativePath() {
        // Given
        Path codebaseDir = getCodebaseDir("ApplyFromProjectRelativePath");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(5);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleApplyFromTo() {
        // Given
        Path codebaseDir = getCodebaseDir("ApplyFromTo");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                MAVEN_CENTRAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT));
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleSpringDependencyManagementPlugin() {
        // Given
        Path codebaseDir = getCodebaseDir("SpringDependencyManagementPlugin");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_CLOUD_STARTER_ZIPKIN_2_2_5_RELEASE,
                SPRING_DEPENDENCY_MANAGEMENT_PLUGIN_1_0_10_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(7);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(95);
    }

    @Test
    public void shouldHandleSpringDependencyManagementPluginStringConcatenation() {
        // Given
        Path codebaseDir = getCodebaseDir("SpringDependencyManagementPluginStringConcatenation");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                SPRING_CLOUD_STARTER_ZIPKIN_2_2_5_RELEASE,
                SPRING_DEPENDENCY_MANAGEMENT_PLUGIN_1_0_10_RELEASE,
                JAVA_PLUGIN);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(7);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(95);
    }

    @Test
    public void shouldHandleImport() {
        // Given
        Path codebaseDir = getCodebaseDir("Import");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).isEmpty();
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleMicronautApplicationPluginWithGradleProperty() {
        // Given
        Path codebaseDir = getCodebaseDir("MicronautApplicationPluginWithGradleProperty");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                MICRONAUT_APPLICATION_PLUGIN_2_0_6,
                MICRONAUT_RUNTIME_3_1_0);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(9);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(28);
    }

    @Test
    public void shouldHandleMicronautApplicationPluginWithGradlePropertyAndMicronautBlock() {
        // Given
        Path codebaseDir = getCodebaseDir("MicronautApplicationPluginWithGradlePropertyAndMicronautBlock");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                MICRONAUT_APPLICATION_PLUGIN_2_0_6,
                MICRONAUT_RUNTIME_3_0_0);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(11);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(28);
    }

    @Test
    public void shouldHandleMicronautApplicationPluginWithMicronautBlock() {
        // Given
        Path codebaseDir = getCodebaseDir("MicronautApplicationPluginWithMicronautBlock");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                MICRONAUT_APPLICATION_PLUGIN_2_0_6,
                MICRONAUT_RUNTIME_3_1_0);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(9);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(28);
    }

    @Test
    public void shouldHandleMicronautLibraryPluginWithGradleProperty() {
        // Given
        Path codebaseDir = getCodebaseDir("MicronautLibraryPluginWithGradleProperty");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                MICRONAUT_LIBRARY_PLUGIN_2_0_6,
                MICRONAUT_RUNTIME_3_1_0);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(9);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(28);
    }

    @Test
    public void shouldHandleMicronautLibraryPluginWithGradlePropertyAndMicronautBlock() {
        // Given
        Path codebaseDir = getCodebaseDir("MicronautLibraryPluginWithGradlePropertyAndMicronautBlock");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                MICRONAUT_LIBRARY_PLUGIN_2_0_6,
                MICRONAUT_RUNTIME_3_0_0);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(11);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(28);
    }

    @Test
    public void shouldHandleMicronautLibraryPluginWithMicronautBlock() {
        // Given
        Path codebaseDir = getCodebaseDir("MicronautLibraryPluginWithMicronautBlock");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactlyInAnyOrder(
                MICRONAUT_LIBRARY_PLUGIN_2_0_6,
                MICRONAUT_RUNTIME_3_1_0);
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).hasSize(9);
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).hasSize(28);
    }

    @Test
    public void shouldHandleRepositoryMavenUrlMethodCall() {
        // Given
        Path codebaseDir = getCodebaseDir("RepositoryMavenUrlMethodCall");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GOOGLE_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleRepositoryMavenUrlMethodCallGString() {
        // Given
        Path codebaseDir = getCodebaseDir("RepositoryMavenUrlMethodCallGString");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GOOGLE_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleRepositoryMavenUrlProperty() {
        // Given
        Path codebaseDir = getCodebaseDir("RepositoryMavenUrlProperty");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GOOGLE_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleRepositoryMavenUrlPropertyGString() {
        // Given
        Path codebaseDir = getCodebaseDir("RepositoryMavenUrlPropertyGString");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GOOGLE_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleRepositoryMavenCentral() {
        // Given
        Path codebaseDir = getCodebaseDir("RepositoryMavenCentral");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                MAVEN_CENTRAL_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleRepositoryJCenter() {
        // Given
        Path codebaseDir = getCodebaseDir("RepositoryJCenter");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                JCENTER_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleRepositoryGoogle() {
        // Given
        Path codebaseDir = getCodebaseDir("RepositoryGoogle");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GOOGLE_REPOSITORY);
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleRepositoryCustom() {
        // Given
        Path codebaseDir = getCodebaseDir("RepositoryCustom");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                SoftwareRepository
                        .builder()
                        .type(SoftwareRepositoryType.MAVEN)
                        .url("https://example.com/repo/")
                        .safe(false)
                        .build());
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }

    @Test
    public void shouldHandleRepositoryCustomWithAuthenticationHeader() {
        // Given
        wireMockServer = new MavenRepositoryWireMockFactory().create();
        Path codebaseDir = getCodebaseDir("RepositoryCustomWithAuthenticationHeader");

        // When
        GradleAnalysis gradleAnalysis = underTest.analyzeCodebase(codebaseDir);

        // Then
        assertThat(gradleAnalysis.getGradleIsUsed()).isTrue();
        assertThat(gradleAnalysis.getSoftwareRepositories()).containsExactlyInAnyOrder(
                GRADLE_PLUGIN_PORTAL_REPOSITORY.withScope(SoftwareRepositoryScope.BUILDSCRIPT),
                SoftwareRepository
                        .builder()
                        .type(SoftwareRepositoryType.MAVEN)
                        .url("http://localhost:36211/repo-with-authentication/")
                        .safe(true)
                        .build());
        Map<SoftwareGroup, List<Software>> softwareGroups = getSoftwareGroups(gradleAnalysis.getSoftware());
        assertThat(softwareGroups.get(SoftwareGroup.DIRECT)).containsExactly(
                JAVA_PLUGIN,
                Software.builder()
                        .type(SoftwareType.JVM)
                        .dependencyType(SoftwareDependencyType.DIRECT)
                        .name("test.group.id:test-artifact-id")
                        .version("test-version")
                        .build()
        );
        assertThat(softwareGroups.get(SoftwareGroup.TRANSITIVE)).isNull();
        assertThat(softwareGroups.get(SoftwareGroup.BOM)).isNull();
    }
}
