package tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors;

import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.slf4j.Logger;
import tech.kronicle.gradlestaticanalyzer.internal.constants.GradlePlugins;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.BuildscriptVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.DependenciesVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.DependencyManagementVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.ExtOuterVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.MicronautVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.services.PluginProcessor;
import tech.kronicle.sdk.models.Software;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class BuildGradleVisitor extends BaseBuildFileVisitor {

    private final BuildscriptVisitor buildscriptVisitor;
    private final DependencyManagementVisitor dependencyManagementVisitor;
    private final DependenciesVisitor dependenciesVisitor;
    private final ExtOuterVisitor extOuterVisitor;
    private final MicronautVisitor micronautVisitor;
    private final PluginProcessor pluginProcessor;

    public BuildGradleVisitor(
            BaseBuildFileVisitorDependencies dependencies,
            BuildscriptVisitor buildscriptVisitor,
            DependencyManagementVisitor dependencyManagementVisitor,
            DependenciesVisitor dependenciesVisitor,
            ExtOuterVisitor extOuterVisitor,
            MicronautVisitor micronautVisitor,
            PluginProcessor pluginProcessor
    ) {
        super(dependencies);
        this.buildscriptVisitor = buildscriptVisitor;
        this.dependencyManagementVisitor = dependencyManagementVisitor;
        this.dependenciesVisitor = dependenciesVisitor;
        this.extOuterVisitor = extOuterVisitor;
        this.micronautVisitor = micronautVisitor;
        this.pluginProcessor = pluginProcessor;
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected ExpressionVisitOutcome processMethodCallExpression(MethodCallExpression call) {
        if (call.getMethodAsString().equals("allprojects")) {
            log.debug("Found allprojects");
            return ExpressionVisitOutcome.CONTINUE;
        } else if (call.getMethodAsString().equals("subprojects")) {
            log.debug("Found subprojects");
            return visitorState().getProjectMode() == ProjectMode.SUBPROJECT
                    ? ExpressionVisitOutcome.CONTINUE
                    : ExpressionVisitOutcome.IGNORED;
        } else if (call.getMethodAsString().equals("buildscript")) {
            if (visitorState().getProcessPhase() == ProcessPhase.PROPERTIES
                || visitorState().getProcessPhase() == ProcessPhase.BUILDSCRIPT_REPOSITORIES
                || visitorState().getProcessPhase() == ProcessPhase.BUILDSCRIPT_DEPENDENCIES) {
                log.debug("Found buildscript");
                visit(call.getArguments(), buildscriptVisitor);
                return ExpressionVisitOutcome.PROCESSED;
            } else {
                return ExpressionVisitOutcome.IGNORED_NO_WARNING;
            }
        } else if (call.getMethodAsString().equals("ext")) {
            if (visitorState().getProcessPhase() == ProcessPhase.PROPERTIES) {
                log.debug("Found ext");
                int count = visitorState().getProperties().size();
                visit(call, extOuterVisitor);
                log.debug("Found {} project properties", visitorState().getProperties().size() - count);
                return ExpressionVisitOutcome.PROCESSED;
            } else {
                return ExpressionVisitOutcome.IGNORED_NO_WARNING;
            }
        } else if (call.getMethodAsString().equals("dependencies")) {
            if (visitorState().getProcessPhase() == ProcessPhase.DEPENDENCIES) {
                log.debug("Found dependencies");
                int count = visitorState().getSoftware().size();
                visit(call.getArguments(), dependenciesVisitor);
                log.debug("Found {} dependencies", visitorState().getSoftware().size() - count);
                return ExpressionVisitOutcome.PROCESSED;
            } else {
                return ExpressionVisitOutcome.IGNORED_NO_WARNING;
            }
        } else if (call.getMethodAsString().equals("dependencyManagement")) {
            if (visitorState().getProcessPhase() == ProcessPhase.DEPENDENCY_MANAGEMENT) {
                log.debug("Found dependencyManagement");
                visit(call.getArguments(), dependencyManagementVisitor);
                return ExpressionVisitOutcome.PROCESSED;
            } else {
                return ExpressionVisitOutcome.IGNORED_NO_WARNING;
            }
        } else if (call.getMethodAsString().equals("micronaut")) {
            if (visitorState().getProcessPhase() == ProcessPhase.DEPENDENCY_MANAGEMENT) {
                log.debug("Found micronaut");
                visit(call.getArguments(), micronautVisitor);
                return ExpressionVisitOutcome.PROCESSED;
            } else {
                return ExpressionVisitOutcome.IGNORED_NO_WARNING;
            }
        }

        return super.processMethodCallExpression(call);
    }

    @Override
    protected void processApplyPlugin(Map<String, String> values) {
        log.debug("Process apply plugin");
        int count = getPluginCount();
        String name = values.get("plugin");

        pluginProcessor.processPlugin(name, getPluginVersion(name), true, visitorState().getSoftware());

        log.debug("Found {} plugins", getPluginCount() - count);
    }

    private String getPluginVersion(String name) {
        if (Objects.equals(name, "org.springframework.boot")) {
            Optional<Software> springBootPlugin = pluginProcessor.getPlugin(GradlePlugins.SPRING_BOOT, visitorState().getSoftware());

            if (springBootPlugin.isPresent()) {
                return springBootPlugin.get().getVersion();
            }

            Optional<Software> springBootPluginDependency = pluginProcessor.getSpringBootPluginDependency(visitorState().getSoftware());

            if (springBootPluginDependency.isPresent()) {
                return springBootPluginDependency.get().getVersion();
            }
        }

        return null;
    }
}
