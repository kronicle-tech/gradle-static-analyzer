package tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor;

import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.slf4j.Logger;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.BaseVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.ExpressionVisitOutcome;

import java.util.Objects;

@Slf4j
public class ExtOuterVisitor extends BaseVisitor {

    private final ExtVisitor extVisitor;

    public ExtOuterVisitor(
            BaseVisitorDependencies dependencies,
            ExtVisitor extVisitor
    ) {
        super(dependencies);
        this.extVisitor = extVisitor;
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected ExpressionVisitOutcome processMethodCallExpression(MethodCallExpression call) {
        Expression objectExpression = call.getObjectExpression();

        if (objectExpression instanceof VariableExpression) {
            String objectName = ((VariableExpression) objectExpression).getName();

            if (Objects.equals(objectName, "this") || Objects.equals(objectName, "project")) {
                log.debug("Found {}.ext", objectName);
                visit(call.getArguments(), extVisitor);
                return ExpressionVisitOutcome.PROCESSED;
            }
        }

        return ExpressionVisitOutcome.IGNORED;
    }
}
