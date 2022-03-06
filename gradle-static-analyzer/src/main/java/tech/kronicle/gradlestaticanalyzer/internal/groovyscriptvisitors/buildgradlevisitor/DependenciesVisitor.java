package tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor;

import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.slf4j.Logger;
import tech.kronicle.common.StringEscapeUtils;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.ProcessPhase;
import tech.kronicle.gradlestaticanalyzer.internal.models.PomOutcome;
import tech.kronicle.gradlestaticanalyzer.internal.services.ArtifactVersionResolver;
import tech.kronicle.gradlestaticanalyzer.internal.services.PomFetcher;
import tech.kronicle.gradlestaticanalyzer.internal.utils.InheritingHashSet;
import tech.kronicle.sdk.models.Software;
import tech.kronicle.sdk.models.SoftwareDependencyType;
import tech.kronicle.sdk.models.SoftwareScope;

import java.util.Objects;
import java.util.Set;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static tech.kronicle.sdk.models.SoftwareType.JVM;

@Slf4j
public class DependenciesVisitor extends BaseArtifactVisitor {

    private final ArtifactVersionResolver artifactVersionResolver;
    private final PomFetcher pomFetcher;
    private final PlatformVisitor platformVisitor;

    public DependenciesVisitor(
            BaseArtifactVisitorDependencies dependencies,
            ArtifactVersionResolver artifactVersionResolver,
            PomFetcher pomFetcher,
            PlatformVisitor platformVisitor) {
        super(dependencies);
        this.artifactVersionResolver = artifactVersionResolver;
        this.pomFetcher = pomFetcher;
        this.platformVisitor = platformVisitor;
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected void processPlatform(MethodCallExpression call) {
        log().debug("Found platform");
        visit(call, platformVisitor);
    }

    @Override
    protected void addArtifact(String groupId, String artifactId, String version, String packaging) {
        String name = artifactUtils().createName(groupId, artifactId);
        String versionSelector;
        Set<String> versions;

        if (nonNull(version)) {
            String newVersion = artifactVersionResolver.resolveArtifactVersion(groupId, artifactId, version, getSoftwareRepositories());
            versions = Set.of(newVersion);
            versionSelector = (!Objects.equals(version, newVersion)) ? version : null;
        } else {
            versions = visitorState().getDependencyVersions().get(name);
            if (isNull(versions) || versions.isEmpty()) {
                throw new IllegalArgumentException("Version could not be found for artifact \"" + StringEscapeUtils.escapeString(name) + "\"");
            }
            versionSelector = null;
        }

        SoftwareScope scope = (visitorState().getProcessPhase() == ProcessPhase.BUILDSCRIPT_DEPENDENCIES) ? SoftwareScope.BUILDSCRIPT : null;
        InheritingHashSet<Software> software = visitorState().getSoftware();

        versions.forEach(newVersion -> {
            software.add(new Software(null, JVM, SoftwareDependencyType.DIRECT, name, newVersion, versionSelector, packaging, scope));

            PomOutcome pomOutcome = pomFetcher.fetchPom(
                    artifactUtils().createArtifact(groupId, artifactId, newVersion),
                    getSoftwareRepositories());
            if (!pomOutcome.isJarOnly() && nonNull(pomOutcome.getPom().getDependencies())) {
                software.addAll(pomOutcome.getPom().getDependencies());
            }
        });
    }
}
