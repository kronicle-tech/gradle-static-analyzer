package tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor;

import lombok.Value;
import tech.kronicle.plugins.gradle.internal.services.BillOfMaterialsLogger;
import tech.kronicle.plugins.gradle.internal.services.DependencyVersionFetcher;
import tech.kronicle.plugins.gradle.internal.utils.ArtifactUtils;

@Value
public class BaseArtifactVisitorDependencies {

    BaseVisitorDependencies baseDependencies;
    ArtifactUtils artifactUtils;
    DependencyVersionFetcher dependencyVersionFetcher;
    BillOfMaterialsLogger billOfMaterialsLogger;
}
