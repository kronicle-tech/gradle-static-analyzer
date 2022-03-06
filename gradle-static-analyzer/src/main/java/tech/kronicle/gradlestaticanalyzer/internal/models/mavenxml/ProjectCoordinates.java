package tech.kronicle.gradlestaticanalyzer.internal.models.mavenxml;

public interface ProjectCoordinates {

    String getGroupId();
    String getArtifactId();
    String getVersion();
    String getPackaging();
}
