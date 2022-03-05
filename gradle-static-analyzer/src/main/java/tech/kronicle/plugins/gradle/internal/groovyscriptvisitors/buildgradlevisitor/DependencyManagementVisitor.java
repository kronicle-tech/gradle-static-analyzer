package tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor;

import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.slf4j.Logger;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.BaseVisitor;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.ExpressionVisitOutcome;


@Slf4j
public class DependencyManagementVisitor extends BaseVisitor {

    private final DependencyManagementImportsVisitor dependencyManagementImportsVisitor;

    public DependencyManagementVisitor(
            BaseVisitorDependencies dependencies,
            DependencyManagementImportsVisitor dependencyManagementImportsVisitor
    ) {
        super(dependencies);
        this.dependencyManagementImportsVisitor = dependencyManagementImportsVisitor;
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected ExpressionVisitOutcome processMethodCallExpression(MethodCallExpression call) {
        if (call.getMethodAsString().equals("imports")) {
            log.debug("Found imports");
            visit(call.getArguments(), dependencyManagementImportsVisitor);
            return ExpressionVisitOutcome.PROCESSED;
        }

        return ExpressionVisitOutcome.IGNORED;
    }
}
