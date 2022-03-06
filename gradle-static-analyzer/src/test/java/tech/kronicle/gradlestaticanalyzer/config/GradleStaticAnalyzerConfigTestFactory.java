package tech.kronicle.gradlestaticanalyzer.config;

import java.time.Duration;
import java.util.List;

public final class GradleStaticAnalyzerConfigTestFactory {

    public static GradleStaticAnalyzerConfig newGradleStaticAnalyzerConfig(Class<?> testClass) {
        String testDataDir = "build/test-data/" + testClass.getName();
        return new GradleStaticAnalyzerConfig(
                List.of("http://localhost:36211/repo-with-authentication/"),
                List.of(
                        new GradleCustomRepository("someCustomRepository", "https://example.com/repo/", List.of()),
                        new GradleCustomRepository("someCustomRepositoryWithAuthentication", "http://localhost:36211/repo-with-authentication/", List.of(
                                new HttpHeaderConfig("test-header-1", "test-value-1"),
                                new HttpHeaderConfig("test-header-2", "test-value-2")
                        ))
                ),
                new DownloaderConfig(Duration.ofSeconds(60)),
                new DownloadCacheConfig(testDataDir + "/download-cache"),
                new UrlExistsCacheConfig(testDataDir + "/url-exists-cache"),
                new PomCacheConfig(testDataDir + "/gradle/pom-cache")
        );
    }

    private GradleStaticAnalyzerConfigTestFactory() {
    }
}
