package tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor;

import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.slf4j.Logger;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.BaseBuildFileVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.BaseBuildFileVisitorDependencies;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.ExpressionVisitOutcome;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.ProcessPhase;


@Slf4j
public class BuildscriptVisitor extends BaseBuildFileVisitor {

    private final RepositoriesVisitor repositoriesVisitor;
    private final DependenciesVisitor dependenciesVisitor;
    private final ExtOuterVisitor extOuterVisitor;

    public BuildscriptVisitor(
            BaseBuildFileVisitorDependencies dependencies,
            RepositoriesVisitor repositoriesVisitor,
            DependenciesVisitor dependenciesVisitor,
            ExtOuterVisitor extOuterVisitor
    ) {
        super(dependencies);
        this.repositoriesVisitor = repositoriesVisitor;
        this.dependenciesVisitor = dependenciesVisitor;
        this.extOuterVisitor = extOuterVisitor;
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected ExpressionVisitOutcome processMethodCallExpression(MethodCallExpression call) {
        if (call.getMethodAsString().equals("repositories")) {
            if (visitorState().getProcessPhase() == ProcessPhase.BUILDSCRIPT_REPOSITORIES) {
                log.debug("Found buildscript repositories");
                int count = getSoftwareRepositories().size();
                visit(call.getArguments(), repositoriesVisitor);
                log.debug("Found {} repositories", getSoftwareRepositories().size() - count);
                return ExpressionVisitOutcome.PROCESSED;
            } else {
                return ExpressionVisitOutcome.IGNORED_NO_WARNING;
            }
        } else if (call.getMethodAsString().equals("dependencies")) {
            if (visitorState().getProcessPhase() == ProcessPhase.BUILDSCRIPT_DEPENDENCIES) {
                log.debug("Found buildscript dependencies");
                int count = visitorState().getSoftware().size();
                visit(call.getArguments(), dependenciesVisitor);
                log.debug("Found {} dependencies", visitorState().getSoftware().size() - count);
                return ExpressionVisitOutcome.PROCESSED;
            } else {
                return ExpressionVisitOutcome.IGNORED_NO_WARNING;
            }
        } else if (call.getMethodAsString().equals("ext")) {
            if (visitorState().getProcessPhase() == ProcessPhase.PROPERTIES) {
                log.debug("Found buildscript ext");
                int count = visitorState().getProperties().size();
                visit(call, extOuterVisitor);
                log.debug("Found {} project properties", visitorState().getProperties().size() - count);
                return ExpressionVisitOutcome.PROCESSED;
            } else {
                return ExpressionVisitOutcome.IGNORED_NO_WARNING;
            }
        }

        return ExpressionVisitOutcome.IGNORED;
    }
}
