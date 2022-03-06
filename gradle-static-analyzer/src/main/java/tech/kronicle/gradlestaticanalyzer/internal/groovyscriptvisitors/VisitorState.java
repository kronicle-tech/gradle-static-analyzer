package tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;
import tech.kronicle.gradlestaticanalyzer.internal.models.Import;
import tech.kronicle.gradlestaticanalyzer.internal.utils.InheritingHashMap;
import tech.kronicle.gradlestaticanalyzer.internal.utils.InheritingHashSet;
import tech.kronicle.sdk.models.Software;
import tech.kronicle.sdk.models.SoftwareRepository;

import java.nio.file.Path;
import java.util.Set;

@Value
@With
@RequiredArgsConstructor
@Builder
public class VisitorState {

    ProcessPhase processPhase;
    ProjectMode projectMode;
    Path codebaseDir;
    Path buildFile;
    Path applyFile;
    Set<Import> imports;
    InheritingHashSet<SoftwareRepository> buildscriptSoftwareRepositories;
    InheritingHashSet<SoftwareRepository> softwareRepositories;
    InheritingHashSet<Software> software;
    InheritingHashMap<String, String> properties;
    InheritingHashMap<String, Set<String>> dependencyVersions;
}
