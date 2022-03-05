package tech.kronicle.plugins.gradle;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import tech.kronicle.plugins.gradle.config.DownloadCacheConfig;
import tech.kronicle.plugins.gradle.config.DownloaderConfig;
import tech.kronicle.plugins.gradle.config.GradleConfig;
import tech.kronicle.plugins.gradle.config.PomCacheConfig;
import tech.kronicle.plugins.gradle.config.UrlExistsCacheConfig;

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
