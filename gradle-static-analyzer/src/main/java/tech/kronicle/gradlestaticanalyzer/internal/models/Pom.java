package tech.kronicle.gradlestaticanalyzer.internal.models;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.Value;
import tech.kronicle.sdk.models.Software;

import java.util.Map;
import java.util.Set;

@Value
@Builder(toBuilder = true)
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class Pom {

    String artifactCoordinates;
    @Singular
    Map<String, String> properties;
    @Singular(value = "transitiveArtifactCoordinates")
    Set<String> transitiveArtifactCoordinates;
    @Singular
    Set<Software> dependencyManagementDependencies;
    @Singular
    Set<Software> dependencies;
}
