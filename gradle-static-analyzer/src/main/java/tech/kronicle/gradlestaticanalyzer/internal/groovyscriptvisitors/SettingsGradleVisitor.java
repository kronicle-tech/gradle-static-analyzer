package tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors;

import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.slf4j.Logger;

import java.util.Map;

@Slf4j
public class SettingsGradleVisitor extends BaseBuildFileVisitor {

    public SettingsGradleVisitor(BaseBuildFileVisitorDependencies dependencies) {
        super(dependencies);
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected ExpressionVisitOutcome processMethodCallExpression(MethodCallExpression call) {
        if (call.getMethodAsString().equals("pluginManagement")) {
            log.debug("Found pluginManagement");
            return ExpressionVisitOutcome.CONTINUE;
        }

        return super.processMethodCallExpression(call);
    }

    @Override
    protected ProcessPhase getRepositoriesProcessPhase() {
        return ProcessPhase.BUILDSCRIPT_REPOSITORIES;
    }

    @Override
    protected void processApplyPlugin(Map<String, String> values) {
    }
}
