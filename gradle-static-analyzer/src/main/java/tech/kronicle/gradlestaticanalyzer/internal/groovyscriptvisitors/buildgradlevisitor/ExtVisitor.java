package tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor;

import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.slf4j.Logger;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.BaseVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.ExpressionVisitOutcome;
import tech.kronicle.gradlestaticanalyzer.internal.services.PropertyExpander;


@Slf4j
public class ExtVisitor extends BaseVisitor {

    private final PropertyExpander propertyExpander;

    public ExtVisitor(
            BaseVisitorDependencies dependencies,
            PropertyExpander propertyExpander
    ) {
        super(dependencies);
        this.propertyExpander = propertyExpander;
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected ExpressionVisitOutcome processMethodCallExpression(MethodCallExpression call) {
        if (call.getMethodAsString().equals("set")) {
            log.debug("Found ext.set");
            if (call.getArguments() instanceof ArgumentListExpression) {
                ArgumentListExpression arguments = (ArgumentListExpression) call.getArguments();
                if (arguments.getExpressions().size() == 2) {
                    visitorState().getProperties().put(arguments.getExpression(0).getText(), arguments.getExpression(1).getText());
                }
            }
            return ExpressionVisitOutcome.PROCESSED;
        }

        return ExpressionVisitOutcome.IGNORED;
    }

    @Override
    protected ExpressionVisitOutcome processBinaryExpression(BinaryExpression expression) {
        String rightExpressionText = expression.getRightExpression().getText();
        if (!rightExpressionText.isEmpty()) {
            rightExpressionText = propertyExpander.expandProperties(rightExpressionText,
                    "expression",
                    visitorState().getProperties(),
                    false);
        }
        visitorState().getProperties().put(expression.getLeftExpression().getText(), rightExpressionText);
        return ExpressionVisitOutcome.PROCESSED;
    }
}
