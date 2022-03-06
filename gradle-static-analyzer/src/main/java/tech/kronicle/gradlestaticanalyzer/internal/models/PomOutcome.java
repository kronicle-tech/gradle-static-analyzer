package tech.kronicle.gradlestaticanalyzer.internal.models;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PomOutcome {

    boolean jarOnly;
    Pom pom;
}
