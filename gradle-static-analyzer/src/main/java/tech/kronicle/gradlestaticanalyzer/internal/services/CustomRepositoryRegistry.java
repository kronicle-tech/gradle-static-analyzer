package tech.kronicle.gradlestaticanalyzer.internal.services;

import tech.kronicle.gradlestaticanalyzer.config.GradleStaticAnalyzerConfig;
import tech.kronicle.gradlestaticanalyzer.config.GradleCustomRepository;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CustomRepositoryRegistry {

    private final Map<String, String> customRepositoryUrls;

    public CustomRepositoryRegistry(GradleStaticAnalyzerConfig config) {
        customRepositoryUrls = Optional.ofNullable(config.getCustomRepositories())
                .map(items -> items.stream()
                        .collect(Collectors.toMap(
                                GradleCustomRepository::getName,
                                GradleCustomRepository::getUrl)))
                .orElse(Map.of());
    }

    public String getCustomRepositoryUrl(String name) {
        return customRepositoryUrls.get(name);
    }
}
