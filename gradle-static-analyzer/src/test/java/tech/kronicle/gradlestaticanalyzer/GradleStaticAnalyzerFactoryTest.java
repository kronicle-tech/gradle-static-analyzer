package tech.kronicle.gradlestaticanalyzer;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import tech.kronicle.gradlestaticanalyzer.GradleStaticAnalyzer;
import tech.kronicle.gradlestaticanalyzer.GradleStaticAnalyzerFactory;
import tech.kronicle.gradlestaticanalyzer.config.DownloadCacheConfig;
import tech.kronicle.gradlestaticanalyzer.config.DownloaderConfig;
import tech.kronicle.gradlestaticanalyzer.config.GradleConfig;
import tech.kronicle.gradlestaticanalyzer.config.PomCacheConfig;
import tech.kronicle.gradlestaticanalyzer.config.UrlExistsCacheConfig;

import java.nio.file.Files;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class GradleStaticAnalyzerFactoryTest {

    @Test
    public void newGradleStaticAnalyzerShouldCreateANewInstance() {
        // Given
        GradleConfig config = new GradleConfig(
                null,
                null,
                new DownloaderConfig(Duration.ofSeconds(60)),
                new DownloadCacheConfig(createTempDirectory()),
                new UrlExistsCacheConfig(createTempDirectory()),
                new PomCacheConfig(createTempDirectory())
        );

        // When
        GradleStaticAnalyzer returnValue = GradleStaticAnalyzerFactory.newGradleStaticAnalyzer(config);

        assertThat(returnValue).isNotNull();
    }

    @SneakyThrows
    private String createTempDirectory() {
        return Files.createTempDirectory(this.getClass().getName()).toString();
    }
}
