package tech.kronicle.gradlestaticanalyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.SneakyThrows;
import tech.kronicle.gradlestaticanalyzer.config.GradleStaticAnalyzerConfig;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.BaseBuildFileVisitorDependencies;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.BuildGradleVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.SettingsGradleVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.BaseArtifactVisitorDependencies;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.BaseVisitorDependencies;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.BuildscriptVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.DependenciesVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.DependencyManagementImportsVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.DependencyManagementVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.ExtOuterVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.ExtVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.MavenRepositoryVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.MicronautVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.PlatformVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.PluginsVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.RepositoriesVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.services.ArtifactVersionResolver;
import tech.kronicle.gradlestaticanalyzer.internal.services.ArtifactVersionsFetcher;
import tech.kronicle.gradlestaticanalyzer.internal.services.BillOfMaterialsLogger;
import tech.kronicle.gradlestaticanalyzer.internal.services.BuildFileCache;
import tech.kronicle.gradlestaticanalyzer.internal.services.BuildFileLoader;
import tech.kronicle.gradlestaticanalyzer.internal.services.BuildFileProcessor;
import tech.kronicle.gradlestaticanalyzer.internal.services.CustomRepositoryRegistry;
import tech.kronicle.gradlestaticanalyzer.internal.services.DependencyVersionFetcher;
import tech.kronicle.gradlestaticanalyzer.internal.services.DownloadCache;
import tech.kronicle.gradlestaticanalyzer.internal.services.Downloader;
import tech.kronicle.gradlestaticanalyzer.internal.services.ExpressionEvaluator;
import tech.kronicle.gradlestaticanalyzer.internal.services.HttpRequestMaker;
import tech.kronicle.gradlestaticanalyzer.internal.services.ImportResolver;
import tech.kronicle.gradlestaticanalyzer.internal.services.MavenRepositoryFileDownloader;
import tech.kronicle.gradlestaticanalyzer.internal.services.PluginProcessor;
import tech.kronicle.gradlestaticanalyzer.internal.services.PomCache;
import tech.kronicle.gradlestaticanalyzer.internal.services.PomFetcher;
import tech.kronicle.gradlestaticanalyzer.internal.services.PropertyExpander;
import tech.kronicle.gradlestaticanalyzer.internal.services.PropertyRetriever;
import tech.kronicle.gradlestaticanalyzer.internal.services.RepositoryAuthHeadersRegistry;
import tech.kronicle.gradlestaticanalyzer.internal.services.SoftwareRepositoryFactory;
import tech.kronicle.gradlestaticanalyzer.internal.services.SoftwareRepositoryUrlSafetyChecker;
import tech.kronicle.gradlestaticanalyzer.internal.services.UrlExistsCache;
import tech.kronicle.gradlestaticanalyzer.internal.utils.ArtifactUtils;
import tech.kronicle.utils.FileUtils;

import java.time.Duration;

import static tech.kronicle.utils.FileUtilsFactory.createFileUtils;
import static tech.kronicle.utils.HttpClientFactory.createHttpClient;
import static tech.kronicle.utils.JsonMapperFactory.createJsonMapper;

public final class GradleStaticAnalyzerFactory {

    @SneakyThrows
    public static GradleStaticAnalyzer newGradleStaticAnalyzer(GradleStaticAnalyzerConfig config) {
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
