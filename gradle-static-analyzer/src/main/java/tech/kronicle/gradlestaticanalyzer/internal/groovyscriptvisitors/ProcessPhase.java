package tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors;

public enum ProcessPhase {

    INITIALIZE,
    PROPERTIES,
    PLUGINS,
    BUILDSCRIPT_REPOSITORIES,
    BUILDSCRIPT_DEPENDENCIES,
    APPLY_PLUGINS,
    REPOSITORIES,
    DEPENDENCY_MANAGEMENT,
    DEPENDENCIES,
    FINALIZE
}
