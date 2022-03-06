package tech.kronicle.gradlestaticanalyzer;

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
import static tech.kronicle.gradlestaticanalyzer.TestDependencyFactory.newGradleStaticAnalyzer;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.DEPENDENCY_CHECK_GRADLE_6_0_2_BUILDSCRIPT;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.DEPENDENCY_CHECK_PLUGIN;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.GRADLE_WRAPPER_6_7;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.GROOVY_PLUGIN;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.HIBERNATE_VALIDATOR_4_1_0_FINAL;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.HIBERNATE_VALIDATOR_6_1_6_FINAL;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.JAVA_PLUGIN;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.JTDS_1_3_1;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.LOMBOK_1_18_16;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.MICRONAUT_APPLICATION_PLUGIN_2_0_6;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.MICRONAUT_LIBRARY_PLUGIN_2_0_6;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.MICRONAUT_RUNTIME_3_0_0;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.MICRONAUT_RUNTIME_3_1_0;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.SPRING_BOOT_GRADLE_PLUGIN_2_3_4_RELEASE;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.SPRING_BOOT_PLUGIN_2_3_4_RELEASE;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.SPRING_BOOT_STARTER_ACTUATOR_2_3_4_RELEASE;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.SPRING_BOOT_STARTER_WEB_2_0_9_RELEASE;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.SPRING_BOOT_STARTER_WEB_2_3_4_RELEASE_SOURCES;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.SPRING_CLOUD_STARTER_ZIPKIN_2_2_5_RELEASE;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareItems.SPRING_DEPENDENCY_MANAGEMENT_PLUGIN_1_0_10_RELEASE;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareRepositories.GOOGLE_REPOSITORY;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareRepositories.GRADLE_PLUGIN_PORTAL_REPOSITORY;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareRepositories.JCENTER_REPOSITORY;
import static tech.kronicle.gradlestaticanalyzer.internal.testconstants.SoftwareRepositories.MAVEN_CENTRAL_REPOSITORY;

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
        Path codebaseDir = getResourcesDir("None");

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
        Path codebaseDir = getResourcesDir("Empty");

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
        Path codebaseDir = getResourcesDir("RootProjectBuiltInProperties");

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
        Path codebaseDir = getResourcesDir("ProjectBuiltInProperties");

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
        Path codebaseDir = getResourcesDir("DependencyClass");

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
        Path codebaseDir = getResourcesDir("Dependency");

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
        Path codebaseDir = getResourcesDir("DependencyPackaging");

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
        Path codebaseDir = getResourcesDir("DependencyDynamicVersion");

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
        Path codebaseDir = getResourcesDir("DependencyPomXmlWithoutNamespace");

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
        Path codebaseDir = getResourcesDir("DependencyFollowRedirect");

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
        Path codebaseDir = getResourcesDir("DependencyList");

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
        Path codebaseDir = getResourcesDir("ApplyPlugin");

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
        Path codebaseDir = getResourcesDir("ApplyPluginClass");

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
        Path codebaseDir = getResourcesDir("ApplyPluginImportedClass");

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
        Path codebaseDir = getResourcesDir("PluginProperty");

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
        Path codebaseDir = getResourcesDir("PluginDefinedInSettingsFileWithApplyFalse");

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
        Path codebaseDir = getResourcesDir("PluginDefinedInSettingsFileWithApplyFalseAndAppliedInBuildFile");

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
        Path codebaseDir = getResourcesDir("PluginDefinedInSettingsFileWithApplyFalseAndAppliedInBuildFile");

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
        Path codebaseDir = getResourcesDir("PluginDefinedInSettingsFileWithApplyTrue");

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
        Path codebaseDir = getResourcesDir("SpringBootPlugin");

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
        Path codebaseDir = getResourcesDir("SpringBootPluginApplyPlugin");

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
        Path codebaseDir = getResourcesDir("SpringBootPluginPluginsAndApplyPlugin");

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
        Path codebaseDir = getResourcesDir("GradleWrapper");

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
        Path codebaseDir = getResourcesDir("DependencyNamedParts");

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
        Path codebaseDir = getResourcesDir("DependencyNamedPartsWithClassifierAndExt");

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
        Path codebaseDir = getResourcesDir("DependencyMultipleInOneCall");

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
        Path codebaseDir = getResourcesDir("DependencyMultipleWithNamedPartsInOneCall");

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
        Path codebaseDir = getResourcesDir("DependencyStringConcatenation");

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
        Path codebaseDir = getResourcesDir("DependencyProject");

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
        Path codebaseDir = getResourcesDir("DependencyLocalGroovy");

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
        Path codebaseDir = getResourcesDir("DependencyGradleApi");

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
        Path codebaseDir = getResourcesDir("DependencyFileTree");

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
        Path codebaseDir = getResourcesDir("DependencyFiles");

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
        Path codebaseDir = getResourcesDir("DependencyVariable");

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
        Path codebaseDir = getResourcesDir("DependencyVariableInDependencies");

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
        Path codebaseDir = getResourcesDir("DependencyDuplicates");

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
        Path codebaseDir = getResourcesDir("DependencyExclusion");

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
        Path codebaseDir = getResourcesDir("ProjectProperties");

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
        Path codebaseDir = getResourcesDir("ProjectPropertyAssignment");

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
        Path codebaseDir = getResourcesDir("ProjectPropertySetMethod");

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
        Path codebaseDir = getResourcesDir("BuildscriptProjectProperty");

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
        Path codebaseDir = getResourcesDir("BuildscriptProjectPropertyAssignment");

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
        Path codebaseDir = getResourcesDir("BuildscriptDependencyWithNoBuildscriptRepository");

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
        Path codebaseDir = getResourcesDir("GradleProperties");

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
        Path codebaseDir = getResourcesDir("GradlePropertiesAndProjectProperties");

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
        Path codebaseDir = getResourcesDir("MultiProject");

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
        Path codebaseDir = getResourcesDir("MultiProjectNestedGradleProperties");

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
        Path codebaseDir = getResourcesDir("MultiProjectMissingBuildFile");

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
        Path codebaseDir = getResourcesDir("MultiProjectSpringBootPlugin");

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
        Path codebaseDir = getResourcesDir("MultiProjectSpringBootPluginApplyFalse");

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
        Path codebaseDir = getResourcesDir("MultiProjectInheritedProjectProperties");

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
        Path codebaseDir = getResourcesDir("MultiProjectOverriddenProjectProperties");

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
        Path codebaseDir = getResourcesDir("MultiProjectAllprojects");

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
        Path codebaseDir = getResourcesDir("MultiProjectSubprojects");

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
        Path codebaseDir = getResourcesDir("MultiProjectSubprojectsRepositories");

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
        Path codebaseDir = getResourcesDir("PlatformDependency");

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
        Path codebaseDir = getResourcesDir("ApplyFrom");

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
        Path codebaseDir = getResourcesDir("ApplyFromRootDir");

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
        Path codebaseDir = getResourcesDir("ApplyFromRootProjectProjectDir");

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
        Path codebaseDir = getResourcesDir("ApplyFromProjectDir");

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
        Path codebaseDir = getResourcesDir("ApplyFromProjectDirParent");

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
        Path codebaseDir = getResourcesDir("ApplyFromProjectRelativePath");

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
        Path codebaseDir = getResourcesDir("ApplyFromTo");

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
        Path codebaseDir = getResourcesDir("SpringDependencyManagementPlugin");

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
        Path codebaseDir = getResourcesDir("SpringDependencyManagementPluginStringConcatenation");

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
        Path codebaseDir = getResourcesDir("Import");

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
        Path codebaseDir = getResourcesDir("MicronautApplicationPluginWithGradleProperty");

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
        Path codebaseDir = getResourcesDir("MicronautApplicationPluginWithGradlePropertyAndMicronautBlock");

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
        Path codebaseDir = getResourcesDir("MicronautApplicationPluginWithMicronautBlock");

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
        Path codebaseDir = getResourcesDir("MicronautLibraryPluginWithGradleProperty");

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
        Path codebaseDir = getResourcesDir("MicronautLibraryPluginWithGradlePropertyAndMicronautBlock");

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
        Path codebaseDir = getResourcesDir("MicronautLibraryPluginWithMicronautBlock");

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
        Path codebaseDir = getResourcesDir("RepositoryMavenUrlMethodCall");

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
        Path codebaseDir = getResourcesDir("RepositoryMavenUrlMethodCallGString");

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
        Path codebaseDir = getResourcesDir("RepositoryMavenUrlProperty");

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
        Path codebaseDir = getResourcesDir("RepositoryMavenUrlPropertyGString");

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
        Path codebaseDir = getResourcesDir("RepositoryMavenCentral");

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
        Path codebaseDir = getResourcesDir("RepositoryJCenter");

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
        Path codebaseDir = getResourcesDir("RepositoryGoogle");

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
        Path codebaseDir = getResourcesDir("RepositoryCustom");

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
        Path codebaseDir = getResourcesDir("RepositoryCustomWithAuthenticationHeader");

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
