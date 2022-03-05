package tech.kronicle.plugins.gradle;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.SneakyThrows;
import tech.kronicle.plugins.gradle.config.GradleConfig;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.BaseBuildFileVisitorDependencies;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.BuildGradleVisitor;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.SettingsGradleVisitor;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor.BaseArtifactVisitorDependencies;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor.BaseVisitorDependencies;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor.BuildscriptVisitor;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor.DependenciesVisitor;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor.DependencyManagementImportsVisitor;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor.DependencyManagementVisitor;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor.ExtOuterVisitor;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor.ExtVisitor;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor.MavenRepositoryVisitor;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor.MicronautVisitor;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor.PlatformVisitor;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor.PluginsVisitor;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor.RepositoriesVisitor;
import tech.kronicle.plugins.gradle.internal.services.ArtifactVersionResolver;
import tech.kronicle.plugins.gradle.internal.services.ArtifactVersionsFetcher;
import tech.kronicle.plugins.gradle.internal.services.BillOfMaterialsLogger;
import tech.kronicle.plugins.gradle.internal.services.BuildFileCache;
import tech.kronicle.plugins.gradle.internal.services.BuildFileLoader;
import tech.kronicle.plugins.gradle.internal.services.BuildFileProcessor;
import tech.kronicle.plugins.gradle.internal.services.CustomRepositoryRegistry;
import tech.kronicle.plugins.gradle.internal.services.DependencyVersionFetcher;
import tech.kronicle.plugins.gradle.internal.services.DownloadCache;
import tech.kronicle.plugins.gradle.internal.services.Downloader;
import tech.kronicle.plugins.gradle.internal.services.ExpressionEvaluator;
import tech.kronicle.plugins.gradle.internal.services.HttpRequestMaker;
import tech.kronicle.plugins.gradle.internal.services.ImportResolver;
import tech.kronicle.plugins.gradle.internal.services.MavenRepositoryFileDownloader;
import tech.kronicle.plugins.gradle.internal.services.PluginProcessor;
import tech.kronicle.plugins.gradle.internal.services.PomCache;
import tech.kronicle.plugins.gradle.internal.services.PomFetcher;
import tech.kronicle.plugins.gradle.internal.services.PropertyExpander;
import tech.kronicle.plugins.gradle.internal.services.PropertyRetriever;
import tech.kronicle.plugins.gradle.internal.services.RepositoryAuthHeadersRegistry;
import tech.kronicle.plugins.gradle.internal.services.SoftwareRepositoryFactory;
import tech.kronicle.plugins.gradle.internal.services.SoftwareRepositoryUrlSafetyChecker;
import tech.kronicle.plugins.gradle.internal.services.UrlExistsCache;
import tech.kronicle.plugins.gradle.internal.utils.ArtifactUtils;
import tech.kronicle.pluginutils.FileUtils;

import java.time.Duration;

import static tech.kronicle.pluginutils.FileUtilsFactory.createFileUtils;
import static tech.kronicle.pluginutils.HttpClientFactory.createHttpClient;
import static tech.kronicle.pluginutils.JsonMapperFactory.createJsonMapper;

public final class GradleStaticAnalyzerFactory {

    @SneakyThrows
    public static GradleStaticAnalyzer newGradleStaticAnalyzer(GradleConfig config) {
        PropertyRetriever propertyRetriever = new PropertyRetriever();
        PropertyExpander propertyExpander = new PropertyExpander(propertyRetriever);
        FileUtils fileUtils = createFileUtils();
        BuildFileCache buildFileCache = new BuildFileCache();
        BuildFileLoader buildFileLoader = new BuildFileLoader(fileUtils, buildFileCache, propertyExpander);
        ImportResolver importResolver = new ImportResolver();
        SoftwareRepositoryUrlSafetyChecker urlSafetyChecker = new SoftwareRepositoryUrlSafetyChecker(config);
        BuildFileProcessor buildFileProcessor = new BuildFileProcessor();
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(
                propertyExpander,
                propertyRetriever,
                importResolver
        );
        SoftwareRepositoryFactory softwareRepositoryFactory = new SoftwareRepositoryFactory(urlSafetyChecker);
        BaseVisitorDependencies baseVisitorDependencies = new BaseVisitorDependencies(
                buildFileLoader,
                buildFileProcessor,
                expressionEvaluator,
                softwareRepositoryFactory
        );
        PluginProcessor pluginProcessor = new PluginProcessor();
        PluginsVisitor pluginsVisitor = new PluginsVisitor(baseVisitorDependencies, pluginProcessor);
        MavenRepositoryVisitor mavenRepositoryVisitor = new MavenRepositoryVisitor(baseVisitorDependencies);
        CustomRepositoryRegistry customRepositoryRegistry = new CustomRepositoryRegistry(config);
        RepositoriesVisitor repositoriesVisitor = new RepositoriesVisitor(
                baseVisitorDependencies,
                mavenRepositoryVisitor,
                customRepositoryRegistry
        );
        BaseBuildFileVisitorDependencies baseBuildFileVisitorDependencies = new BaseBuildFileVisitorDependencies(
                baseVisitorDependencies,
                pluginsVisitor,
                repositoriesVisitor,
                pluginProcessor
        );
        ArtifactUtils artifactUtils = new ArtifactUtils();
        DownloadCache downloadCache = new DownloadCache(fileUtils, config.getDownloadCache());
        RetryRegistry retryRegistry = RetryRegistry.custom()
                .addRetryConfig("http-request-maker", RetryConfig.custom()
                        .maxAttempts(5)
                        .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofSeconds(10), 2))
                        .build())
                .build();
        UrlExistsCache urlExistsCache = new UrlExistsCache(fileUtils, config.getUrlExistsCache());
        HttpRequestMaker httpRequestMaker = new HttpRequestMaker(retryRegistry);
        Downloader downloader = new Downloader(
                createHttpClient(config.getDownloader().getTimeout()),
                config.getDownloader(),
                downloadCache,
                urlExistsCache,
                httpRequestMaker
        );
        RepositoryAuthHeadersRegistry repositoryAuthHeadersRegistry = new RepositoryAuthHeadersRegistry(config);
        MavenRepositoryFileDownloader mavenRepositoryFileDownloader = new MavenRepositoryFileDownloader(
                artifactUtils,
                downloader,
                repositoryAuthHeadersRegistry
        );
        ObjectMapper objectMapper = createJsonMapper();
        PomCache pomCache = new PomCache(fileUtils, config.getPomCache());
        PomFetcher pomFetcher = new PomFetcher(
                mavenRepositoryFileDownloader,
                pomCache,
                propertyExpander,
                objectMapper,
                artifactUtils
        );
        DependencyVersionFetcher dependencyVersionFetcher = new DependencyVersionFetcher(pomFetcher, artifactUtils);
        BillOfMaterialsLogger billOfMaterialsLogger = new BillOfMaterialsLogger();
        ArtifactVersionsFetcher artifactVersionsFetcher = new ArtifactVersionsFetcher(
                mavenRepositoryFileDownloader,
                artifactUtils
        );
        ArtifactVersionResolver artifactVersionResolver = new ArtifactVersionResolver(
                artifactVersionsFetcher,
                artifactUtils
        );
        BaseArtifactVisitorDependencies baseArtifactVisitorDependencies = new BaseArtifactVisitorDependencies(
                baseVisitorDependencies,
                artifactUtils,
                dependencyVersionFetcher,
                billOfMaterialsLogger
        );
        PlatformVisitor platformVisitor = new PlatformVisitor(baseArtifactVisitorDependencies);
        DependenciesVisitor dependenciesVisitor = new DependenciesVisitor(
                baseArtifactVisitorDependencies,
                artifactVersionResolver,
                pomFetcher,
                platformVisitor
        );
        ExtVisitor extVisitor = new ExtVisitor(baseVisitorDependencies, propertyExpander);
        ExtOuterVisitor extOuterVisitor = new ExtOuterVisitor(baseVisitorDependencies, extVisitor);
        BuildscriptVisitor buildscriptVisitor = new BuildscriptVisitor(
                baseBuildFileVisitorDependencies,
                repositoriesVisitor,
                dependenciesVisitor,
                extOuterVisitor
        );
        DependencyManagementImportsVisitor dependencyManagementImportsVisitor = new DependencyManagementImportsVisitor(
                baseArtifactVisitorDependencies
        );
        MicronautVisitor micronautVisitor = new MicronautVisitor(baseBuildFileVisitorDependencies);
        DependencyManagementVisitor dependencyManagementVisitor = new DependencyManagementVisitor(
                baseVisitorDependencies,
                dependencyManagementImportsVisitor
        );
        BuildGradleVisitor buildGradleVisitor = new BuildGradleVisitor(
                baseBuildFileVisitorDependencies,
                buildscriptVisitor,
                dependencyManagementVisitor,
                dependenciesVisitor,
                extOuterVisitor,
                micronautVisitor,
                pluginProcessor
        );
        return new GradleStaticAnalyzer(
                new SettingsGradleVisitor(baseBuildFileVisitorDependencies),
                buildGradleVisitor,
                buildFileLoader,
                dependencyVersionFetcher,
                artifactUtils,
                pluginProcessor,
                softwareRepositoryFactory,
                buildFileProcessor,
                fileUtils
        );
    }

    private GradleStaticAnalyzerFactory() {
    }
}
