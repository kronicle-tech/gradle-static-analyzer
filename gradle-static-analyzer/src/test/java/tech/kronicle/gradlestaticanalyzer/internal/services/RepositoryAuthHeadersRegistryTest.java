package tech.kronicle.gradlestaticanalyzer.internal.services;

import org.junit.jupiter.api.Test;
import tech.kronicle.gradlestaticanalyzer.config.GradleStaticAnalyzerConfig;
import tech.kronicle.gradlestaticanalyzer.config.GradleCustomRepository;
import tech.kronicle.gradlestaticanalyzer.config.HttpHeaderConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryAuthHeadersRegistryTest {

    @Test
    void getSoftwareRepositoryAuthHeadersWhenCustomRepositoriesListIsNullShouldReturnNull() {
        // Given
        GradleStaticAnalyzerConfig config = new GradleStaticAnalyzerConfig(null, null, null, null, null, null);
        RepositoryAuthHeadersRegistry underTest = new RepositoryAuthHeadersRegistry(config);

        // When
        List<HttpHeaderConfig> returnValue = underTest.getRepositoryAuthHeaders("https://example.com/repo-2/test.group.id/test-artifact-id/test-artifact-id:test-version.pom");

        // Then
        assertThat(returnValue).isNull();
    }

    @Test
    void getSoftwareRepositoryAuthHeadersWhenCustomRepositoriesListIsEmptyShouldReturnNull() {
        // Given
        GradleStaticAnalyzerConfig config = new GradleStaticAnalyzerConfig(List.of(), null, null, null, null, null);
        RepositoryAuthHeadersRegistry underTest = new RepositoryAuthHeadersRegistry(config);

        // When
        List<HttpHeaderConfig> returnValue = underTest.getRepositoryAuthHeaders("https://example.com/repo-2/test.group.id/test-artifact-id/test-artifact-id:test-version.pom");

        // Then
        assertThat(returnValue).isNull();
    }

    @Test
    void getSoftwareRepositoryAuthHeadersWhenUrlDoesNotMatchACustomSoftwareRepositoryShouldReturnNull() {
        // Given
        List<GradleCustomRepository> customRepositories = List.of(
                new GradleCustomRepository("testCustomRepository1", "https://example.com/repo-1", List.of(
                        new HttpHeaderConfig("test-header-1", "test-value-1")
                ))
        );
        GradleStaticAnalyzerConfig config = new GradleStaticAnalyzerConfig(null, customRepositories, null, null, null, null);
        RepositoryAuthHeadersRegistry underTest = new RepositoryAuthHeadersRegistry(config);

        // When
        List<HttpHeaderConfig> returnValue = underTest.getRepositoryAuthHeaders("https://example.com/repo-2/test.group.id/test-artifact-id/test-artifact-id:test-version.pom");

        // Then
        assertThat(returnValue).isNull();
    }

    @Test
    void getSoftwareRepositoryAuthHeadersWhenUrlMatchesACustomSoftwareRepositoryShouldReturnTheAssociatedAuthHeaders() {
        // Given
        List<HttpHeaderConfig> httpHeaders = List.of(
                new HttpHeaderConfig("test-header-1", "test-value-1")
        );
        List<GradleCustomRepository> customRepositories = List.of(
                new GradleCustomRepository("testCustomRepository1", "https://example.com/repo-1", httpHeaders)
        );
        GradleStaticAnalyzerConfig config = new GradleStaticAnalyzerConfig(null, customRepositories, null, null, null, null);
        RepositoryAuthHeadersRegistry underTest = new RepositoryAuthHeadersRegistry(config);

        // When
        List<HttpHeaderConfig> returnValue = underTest.getRepositoryAuthHeaders("https://example.com/repo-1/test.group.id/test-artifact-id/test-artifact-id:test-version.pom");

        // Then
        assertThat(returnValue).containsExactlyElementsOf(httpHeaders);
    }

    @Test
    void getSoftwareRepositoryAuthHeadersWhenCustomRepositoryUrlIsMissingATrailingSlashAndDoesMatchWhenAddingATrailingSlashShouldReturnTheAssociatedAuthHeaders() {
        // Given
        List<HttpHeaderConfig> httpHeaders = List.of(
                new HttpHeaderConfig("test-header-1", "test-value-1")
        );
        List<GradleCustomRepository> customRepositories = List.of(
                new GradleCustomRepository("testCustomRepository1", "https://example.com/repo-1", httpHeaders)
        );
        GradleStaticAnalyzerConfig config = new GradleStaticAnalyzerConfig(null, customRepositories, null, null, null, null);
        RepositoryAuthHeadersRegistry underTest = new RepositoryAuthHeadersRegistry(config);

        // When
        List<HttpHeaderConfig> returnValue = underTest.getRepositoryAuthHeaders("https://example.com/repo-1/test.group.id/test-artifact-id/test-artifact-id:test-version.pom");

        // Then
        assertThat(returnValue).containsExactlyElementsOf(httpHeaders);
    }


    @Test
    void getSoftwareRepositoryAuthHeadersWhenCustomRepositoryUrlIsMissingATrailingSlashAndDoesNotMatchWhenAddingATrailingSlashShouldReturnNull() {
        // Given
        List<HttpHeaderConfig> httpHeaders = List.of(
                new HttpHeaderConfig("test-header-1", "test-value-1")
        );
        List<GradleCustomRepository> customRepositories = List.of(
                new GradleCustomRepository("testCustomRepository1", "https://example.com/repo-1", httpHeaders)
        );
        GradleStaticAnalyzerConfig config = new GradleStaticAnalyzerConfig(null, customRepositories, null, null, null, null);
        RepositoryAuthHeadersRegistry underTest = new RepositoryAuthHeadersRegistry(config);

        // When
        List<HttpHeaderConfig> returnValue = underTest.getRepositoryAuthHeaders("https://example.com/repo-11/test.group.id/test-artifact-id/test-artifact-id:test-version.pom");

        // Then
        assertThat(returnValue).isNull();
    }
}