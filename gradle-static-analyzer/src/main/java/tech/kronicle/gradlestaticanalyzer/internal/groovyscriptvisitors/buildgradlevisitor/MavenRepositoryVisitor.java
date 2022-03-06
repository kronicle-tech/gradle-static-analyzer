package tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor;

import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.slf4j.Logger;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.BaseVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.ExpressionVisitOutcome;


@Slf4j
public class MavenRepositoryVisitor extends BaseVisitor {

    public MavenRepositoryVisitor(BaseVisitorDependencies dependencies) {
        super(dependencies);
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected ExpressionVisitOutcome processMethodCallExpression(MethodCallExpression call) {
        if (call.getMethodAsString().equals("url")) {
            ArgumentListExpression arguments = (ArgumentListExpression) call.getArguments();
            addSoftwareRepository(evaluateExpression(arguments.getExpression(0)));
            return ExpressionVisitOutcome.PROCESSED;
        }

        return ExpressionVisitOutcome.IGNORED;
    }

    @Override
    protected ExpressionVisitOutcome processBinaryExpression(BinaryExpression expression) {
        if (expression.getLeftExpression().getText().equals("url")) {
            addSoftwareRepository(evaluateExpression(expression.getRightExpression()));
            return ExpressionVisitOutcome.PROCESSED;
        }

        return ExpressionVisitOutcome.IGNORED;
    }
}
