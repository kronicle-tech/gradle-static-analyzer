package tech.kronicle.gradlestaticanalyzer.internal.testconstants;

import tech.kronicle.sdk.models.SoftwareRepository;
import tech.kronicle.sdk.models.SoftwareRepositoryType;

public final class SoftwareRepositories {

    public static final SoftwareRepository GRADLE_PLUGIN_PORTAL_REPOSITORY = SoftwareRepository
            .builder()
            .type(SoftwareRepositoryType.MAVEN)
            .url("https://plugins.gradle.org/m2/")
            .safe(true)
            .build();
    public static final SoftwareRepository JCENTER_REPOSITORY = SoftwareRepository
            .builder()
            .type(SoftwareRepositoryType.MAVEN)
            .url("https://jcenter.bintray.com/")
            .safe(true)
            .build();
    public static final SoftwareRepository GOOGLE_REPOSITORY = SoftwareRepository
            .builder()
            .type(SoftwareRepositoryType.MAVEN)
            .url("https://dl.google.com/dl/android/maven2/")
            .safe(false)
            .build();
    public static final SoftwareRepository MAVEN_CENTRAL_REPOSITORY = SoftwareRepository
            .builder()
            .type(SoftwareRepositoryType.MAVEN)
            .url("https://repo.maven.apache.org/maven2/")
            .safe(true)
            .build();

    private SoftwareRepositories() {
    }
}
