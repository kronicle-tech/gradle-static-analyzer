package tech.kronicle.plugins.gradle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.ast.ASTNode;
import tech.kronicle.plugins.gradle.internal.constants.ArtifactNames;
import tech.kronicle.plugins.gradle.internal.constants.GradlePlugins;
import tech.kronicle.plugins.gradle.internal.constants.GradlePropertyNames;
import tech.kronicle.plugins.gradle.internal.constants.SoftwareRepositoryUrls;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.BaseVisitor;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.BuildGradleVisitor;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.ProcessPhase;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.ProjectMode;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.SettingsGradleVisitor;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.VisitorState;
import tech.kronicle.plugins.gradle.internal.models.Import;
import tech.kronicle.plugins.gradle.internal.services.BuildFileLoader;
import tech.kronicle.plugins.gradle.internal.services.BuildFileProcessor;
import tech.kronicle.plugins.gradle.internal.services.DependencyVersionFetcher;
import tech.kronicle.plugins.gradle.internal.services.PluginProcessor;
import tech.kronicle.plugins.gradle.internal.services.SoftwareRepositoryFactory;
import tech.kronicle.plugins.gradle.internal.utils.ArtifactUtils;
import tech.kronicle.plugins.gradle.internal.utils.InheritingHashMap;
import tech.kronicle.plugins.gradle.internal.utils.InheritingHashSet;
import tech.kronicle.utils.Comparators;
import tech.kronicle.utils.FileUtils;
import tech.kronicle.sdk.models.Software;
import tech.kronicle.sdk.models.SoftwareDependencyType;
import tech.kronicle.sdk.models.SoftwareRepository;
import tech.kronicle.sdk.models.SoftwareRepositoryScope;
import tech.kronicle.sdk.models.SoftwareType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static tech.kronicle.common.StringEscapeUtils.escapeString;
import static tech.kronicle.plugins.gradle.internal.constants.GradleFileNames.BUILD_GRADLE;
import static tech.kronicle.plugins.gradle.internal.constants.GradleFileNames.GRADLE_PROPERTIES;
import static tech.kronicle.plugins.gradle.internal.constants.GradleFileNames.GRADLE_WRAPPER_PROPERTIES;
import static tech.kronicle.plugins.gradle.internal.constants.GradleFileNames.SETTINGS_GRADLE;
import static tech.kronicle.plugins.gradle.internal.constants.GradleWrapperPropertyNames.DISTRIBUTION_URL;
import static tech.kronicle.plugins.gradle.internal.constants.ToolNames.GRADLE_WRAPPER;

@RequiredArgsConstructor
@Slf4j
public class GradleStaticAnalyzer {

    private static final Pattern GRADLE_WRAPPER_VERSION_EXTRACTION_PATTERN = Pattern.compile("/gradle-([0-9]+\\.[0-9]+(\\.[0-9]+)?)-");
    private static final List<ProcessPhase> PROCESS_PHASES = List.of(
            ProcessPhase.INITIALIZE,
            ProcessPhase.PROPERTIES,
            ProcessPhase.PLUGINS,
            ProcessPhase.BUILDSCRIPT_REPOSITORIES,
            ProcessPhase.BUILDSCRIPT_DEPENDENCIES,
            ProcessPhase.APPLY_PLUGINS,
            ProcessPhase.REPOSITORIES,
            ProcessPhase.DEPENDENCY_MANAGEMENT,
            ProcessPhase.DEPENDENCIES,
            ProcessPhase.FINALIZE);
    private final SettingsGradleVisitor settingsGradleVisitor;
    private final BuildGradleVisitor buildGradleVisitor;
    private final BuildFileLoader buildFileLoader;
    private final DependencyVersionFetcher dependencyVersionFetcher;
    private final ArtifactUtils artifactUtils;
    private final PluginProcessor pluginProcessor;
    private final SoftwareRepositoryFactory softwareRepositoryFactory;
    private final BuildFileProcessor buildFileProcessor;
    private final FileUtils fileUtils;

    public GradleAnalysis analyzeCodebase(Path codebaseDir) {
        Set<SoftwareRepository> allSoftwareRepositories = new HashSet<>();
        Set<Software> allSoftware = new HashSet<>();

        Path gradleWrapperPropertiesFile = codebaseDir.resolve("gradle").resolve("wrapper").resolve(GRADLE_WRAPPER_PROPERTIES);

        if (Files.exists(gradleWrapperPropertiesFile)) {
            Properties gradleWrapperProperties = fileUtils.loadProperties(gradleWrapperPropertiesFile);
            String distributionUrl = requireNonNull(gradleWrapperProperties.getProperty(DISTRIBUTION_URL),
                    "distributionUrl in " + GRADLE_WRAPPER_PROPERTIES + " file does not contain \"" + DISTRIBUTION_URL +  "\" property");
            allSoftware.add(Software.builder()
                    .type(SoftwareType.TOOL)
                    .dependencyType(SoftwareDependencyType.DIRECT)
                    .name(GRADLE_WRAPPER)
                    .version(extractGradleWrapperVersionFromDistributionUrl(distributionUrl))
                    .build());
        }

        HashMap<String, String> rootProperties = new HashMap<>();
        rootProperties.put("rootDir", codebaseDir.toString());
        rootProperties.put("rootProject.projectDir", codebaseDir.toString());
        rootProperties.put("rootProject.name", codebaseDir.getFileName().toString());

        long buildFileCount = fileUtils.findFiles(codebaseDir, this::matchBuildFile)
            .peek(buildFile -> {
                log.debug("Found build file \"" + escapeString(buildFile.toString()) + "\"");
                List<Path> buildFileChain = getBuildFileChain(codebaseDir, buildFile);
                List<InheritingHashMap<String, String>> properties = new ArrayList<>();
                List<InheritingHashSet<SoftwareRepository>> buildscriptSoftwareRepositories = new ArrayList<>();
                List<InheritingHashSet<SoftwareRepository>> softwareRepositories = new ArrayList<>();
                List<InheritingHashSet<Software>> software = new ArrayList<>();
                List<InheritingHashMap<String, Set<String>>> dependencyVersions = new ArrayList<>();

                PROCESS_PHASES.forEach(processPhase -> {
                    log.debug("Beginning {} phase", processPhase);
                    for (int index = 0, count = buildFileChain.size(); index < count; index++) {
                        ProjectMode projectMode = getProjectMode(buildFileChain, index);

                        InheritingHashMap<String, String> currentProperties = getInheritingItem(properties, index, processPhase,
                                () -> cloneValues(rootProperties), InheritingHashMap::new);
                        InheritingHashSet<SoftwareRepository> currentBuildscriptSoftwareRepositories = getInheritingItem(buildscriptSoftwareRepositories,
                                index, processPhase, InheritingHashSet::new, InheritingHashSet::new);
                        InheritingHashSet<SoftwareRepository> currentSoftwareRepositories = getInheritingItem(softwareRepositories, index, processPhase,
                                InheritingHashSet::new, InheritingHashSet::new);
                        InheritingHashSet<Software> currentSoftware = getInheritingItem(software, index, processPhase,
                                InheritingHashSet::new, InheritingHashSet::new);
                        InheritingHashMap<String, Set<String>> currentDependencyVersions = getInheritingItem(dependencyVersions, index, processPhase,
                                InheritingHashMap::new, InheritingHashMap::new);

                        Path currentBuildFile = buildFileChain.get(index);
                        log.debug("Processing build file \"" + escapeString(currentBuildFile.toString()) + "\"");

                        if (processPhase == ProcessPhase.INITIALIZE) {
                            if (projectMode != ProjectMode.SETTINGS) {
                                Path projectDir = currentBuildFile.getParent();
                                currentProperties.put("project.name", projectDir.getFileName().toString());
                                String propertyName = "projectDir";

                                while (projectDir.startsWith(codebaseDir)) {
                                    currentProperties.put(propertyName, projectDir.toString());

                                    projectDir = projectDir.getParent();
                                    propertyName += ".parent";
                                }
                            }

                            Path gradlePropertiesFile = currentBuildFile.getParent().resolve(GRADLE_PROPERTIES);
                            if (Files.exists(gradlePropertiesFile)) {
                                Properties gradleProperties = fileUtils.loadProperties(gradlePropertiesFile);
                                addPropertiesToPropertyMap(gradleProperties, currentProperties);
                            }
                        } else if (processPhase == ProcessPhase.FINALIZE) {
                            if (isLastBuildFileInChain(buildFileChain, index)) {
                                allSoftwareRepositories.addAll(currentBuildscriptSoftwareRepositories);
                                allSoftwareRepositories.addAll(currentSoftwareRepositories);
                                allSoftware.addAll(currentSoftware);
                            }
                        } else {
                            if (processPhase == ProcessPhase.DEPENDENCIES && projectMode != ProjectMode.SETTINGS) {
                                emulateSpringBootPlugin(currentSoftwareRepositories, currentSoftware, currentDependencyVersions);
                                emulateMicronautApplicationPlugin(currentProperties, currentSoftwareRepositories, currentSoftware, currentDependencyVersions);
                                emulateMicronautLibraryPlugin(currentProperties, currentSoftwareRepositories, currentSoftware, currentDependencyVersions);
                            }

                            if (Files.exists(currentBuildFile)) {
                                List<ASTNode> nodes = buildFileLoader.loadBuildFile(currentBuildFile, codebaseDir);

                                try {
                                    Set<Import> imports = buildFileProcessor.getImports(nodes);

                                    VisitorState visitorState = new VisitorState(
                                            processPhase,
                                            projectMode,
                                            codebaseDir,
                                            currentBuildFile,
                                            null,
                                            imports,
                                            currentBuildscriptSoftwareRepositories,
                                            currentSoftwareRepositories,
                                            currentSoftware,
                                            currentProperties,
                                            currentDependencyVersions
                                    );
                                    BaseVisitor visitor = (projectMode == ProjectMode.SETTINGS) ? settingsGradleVisitor : buildGradleVisitor;
                                    visitor.setVisitorState(visitorState, visitorState.getProperties());

                                    buildFileProcessor.visitNodes(nodes, visitor);
                                } catch (Exception e) {
                                    throw new RuntimeException(String.format("Failed to process build file \"%s\" for %s project mode and %s process phase",
                                            escapeString(currentBuildFile.toString()), projectMode, processPhase), e);
                                }
                            }

                            if (processPhase == ProcessPhase.BUILDSCRIPT_REPOSITORIES && projectMode != ProjectMode.SETTINGS) {
                                if (pluginProcessor.getPluginCount(currentSoftware) > 0) {
                                    if (currentBuildscriptSoftwareRepositories.isEmpty()) {
                                        currentBuildscriptSoftwareRepositories.add(
                                                softwareRepositoryFactory.createSoftwareRepository(
                                                        SoftwareRepositoryUrls.GRADLE_PLUGIN_PORTAL,
                                                        SoftwareRepositoryScope.BUILDSCRIPT
                                                )
                                        );
                                    }
                                }
                            }
                        }
                    }
                });
            })
            .count();

        boolean gradleIsUsed = buildFileCount > 0;
        List<SoftwareRepository> allSoftwareRepositoriesList = allSoftwareRepositories.stream()
                .sorted(Comparators.SOFTWARE_REPOSITORIES)
                .collect(Collectors.toList());
        List<Software> allSoftwareList = allSoftware.stream()
                .sorted(Comparators.SOFTWARE)
                .collect(Collectors.toList());
        return new GradleAnalysis(gradleIsUsed, allSoftwareRepositoriesList, allSoftwareList);
    }

    private void emulateSpringBootPlugin(
            InheritingHashSet<SoftwareRepository> softwareRepositories,
            InheritingHashSet<Software> software,
            InheritingHashMap<String, Set<String>> dependencyVersions
    ) {
        emulateBomPlugin(
                GradlePlugins.SPRING_BOOT,
                ArtifactNames.SPRING_BOOT_DEPENDENCIES,
                Software::getVersion,
                softwareRepositories,
                software,
                dependencyVersions);
    }

    private void emulateMicronautApplicationPlugin(
            InheritingHashMap<String, String> properties,
            InheritingHashSet<SoftwareRepository> softwareRepositories,
            InheritingHashSet<Software> software,
            InheritingHashMap<String, Set<String>> dependencyVersions
    ) {
        emulateBomPlugin(
                GradlePlugins.MICRONAUT_APPLICATION,
                ArtifactNames.MICRONAUT_BOM,
                ignored -> getMicronautVersion(properties),
                softwareRepositories,
                software,
                dependencyVersions);
    }

    private void emulateMicronautLibraryPlugin(
            InheritingHashMap<String, String> properties,
            InheritingHashSet<SoftwareRepository> softwareRepositories,
            InheritingHashSet<Software> software,
            InheritingHashMap<String, Set<String>> dependencyVersions
    ) {
        emulateBomPlugin(
                GradlePlugins.MICRONAUT_LIBRARY,
                ArtifactNames.MICRONAUT_BOM,
                ignored -> getMicronautVersion(properties),
                softwareRepositories,
                software,
                dependencyVersions);
    }

    private void emulateBomPlugin(
            String pluginName,
            String bomCoordinates,
            Function<Software, String> versionGetter,
            Set<SoftwareRepository> softwareRepositories,
            Set<Software> software,
            Map<String, Set<String>> dependencyVersions
    ) {
        pluginProcessor.getPlugin(pluginName, software).ifPresent(plugin ->
                dependencyVersionFetcher.findDependencyVersions(
                        artifactUtils.createArtifactFromNameAndVersion(bomCoordinates, versionGetter.apply(plugin)),
                        softwareRepositories,
                        dependencyVersions,
                        software));
    }

    private String getMicronautVersion(InheritingHashMap<String, String> currentProperties) {
        String micronautVersion = currentProperties.get(GradlePropertyNames.MICRONAUT_VERSION);
        if (isNull(micronautVersion)) {
            throw new RuntimeException("Micronaut version not set. Use micronaut { version '..'} or 'micronautVersion' in gradle.properties to set the version");
        }
        return micronautVersion;
    }

    private ProjectMode getProjectMode(List<Path> buildFileChain, int index) {
        ProjectMode projectMode;

        if (index == 0 && isSettingsGradleFile(buildFileChain.get(index))) {
            projectMode = ProjectMode.SETTINGS;
        } else {
            projectMode = isLastBuildFileInChain(buildFileChain, index)
                    ? ProjectMode.THIS_PROJECT
                    : ProjectMode.SUBPROJECT;
        }
        return projectMode;
    }

    private boolean isSettingsGradleFile(Path file) {
        return Objects.equals(file.getFileName().toString(), SETTINGS_GRADLE);
    }

    private <T> T getInheritingItem(List<T> list, int index, ProcessPhase processPhase, Supplier<T> initializeRoot, UnaryOperator<T> initializeChild) {
        if (processPhase == ProcessPhase.INITIALIZE) {
            T item = (index == 0) ? initializeRoot.get() : initializeChild.apply(getParentItem(list, index));
            list.add(item);
            return item;
        } else {
            return list.get(index);
        }
    }

    private <T> T getParentItem(List<T> list, int index) {
        return list.get(index - 1);
    }

    protected InheritingHashMap<String, String> cloneValues(HashMap<String, String> values) {
        InheritingHashMap<String, String> newValues = new InheritingHashMap<>();
        newValues.putAll(values);
        return newValues;
    }

    private boolean isLastBuildFileInChain(List<Path> buildFileChain, int index) {
        return index == buildFileChain.size() - 1;
    }

    private List<Path> getBuildFileChain(Path codebaseDir, Path buildFile) {
        List<Path> buildFileChain = new ArrayList<>();
        Path currentBuildFile = buildFile;

        do {
            buildFileChain.add(0, currentBuildFile);
            currentBuildFile = currentBuildFile.getParent().getParent().resolve(BUILD_GRADLE);
        } while (currentBuildFile.startsWith(codebaseDir));

        currentBuildFile = codebaseDir.resolve(SETTINGS_GRADLE);
        buildFileChain.add(0, currentBuildFile);

        return buildFileChain;
    }

    private void addPropertiesToPropertyMap(Properties gradleProperties, HashMap<String, String> properties) {
        gradleProperties.forEach((name, value) -> properties.put((String) name, (String) value));
    }

    private String extractGradleWrapperVersionFromDistributionUrl(String distributionUrl) {
        Matcher matcher = GRADLE_WRAPPER_VERSION_EXTRACTION_PATTERN.matcher(distributionUrl);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Could not extract Gradle Wrapper version from distribution URL \"" + escapeString(distributionUrl) + "\"");
        }

        return matcher.group(1);
    }

    private boolean matchBuildFile(Path path, BasicFileAttributes basicFileAttributes) {
        return basicFileAttributes.isRegularFile() && Objects.equals(path.getFileName().toString(), BUILD_GRADLE);
    }
}
