package tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor;

import lombok.Value;
import tech.kronicle.gradlestaticanalyzer.internal.services.BillOfMaterialsLogger;
import tech.kronicle.gradlestaticanalyzer.internal.services.DependencyVersionFetcher;
import tech.kronicle.gradlestaticanalyzer.internal.utils.ArtifactUtils;

@Value
public class BaseArtifactVisitorDependencies {

    BaseVisitorDependencies baseDependencies;
    ArtifactUtils artifactUtils;
    DependencyVersionFetcher dependencyVersionFetcher;
    BillOfMaterialsLogger billOfMaterialsLogger;
}
