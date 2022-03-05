package tech.kronicle.plugins.gradle.internal.services;

import lombok.RequiredArgsConstructor;
import tech.kronicle.sdk.models.SoftwareRepository;
import tech.kronicle.sdk.models.SoftwareRepositoryScope;
import tech.kronicle.sdk.models.SoftwareRepositoryType;

@RequiredArgsConstructor
public class SoftwareRepositoryFactory {

    private final SoftwareRepositoryUrlSafetyChecker urlSafetyChecker;

    public SoftwareRepository createSoftwareRepository(String url, SoftwareRepositoryScope scope) {
        return SoftwareRepository.builder()
                .type(SoftwareRepositoryType.MAVEN)
                .url(url)
                .safe(urlSafetyChecker.isSoftwareRepositoryUrlSafe(url))
                .scope(scope)
                .build();
    }
}
