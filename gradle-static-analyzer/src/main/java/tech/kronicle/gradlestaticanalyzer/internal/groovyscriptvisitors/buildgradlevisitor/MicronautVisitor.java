package tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor;

import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.slf4j.Logger;
import tech.kronicle.gradlestaticanalyzer.internal.constants.GradlePropertyNames;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.BaseBuildFileVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.BaseBuildFileVisitorDependencies;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.ExpressionVisitOutcome;


@Slf4j
public class MicronautVisitor extends BaseBuildFileVisitor {

    public MicronautVisitor(BaseBuildFileVisitorDependencies dependencies) {
        super(dependencies);
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected ExpressionVisitOutcome processMethodCallExpression(MethodCallExpression call) {
        if (call.getMethodAsString().equals("version")) {
            log.debug("Found micronaut version");
            if (call.getArguments() instanceof ArgumentListExpression) {
                ArgumentListExpression arguments = (ArgumentListExpression) call.getArguments();
                if (arguments.getExpressions().size() == 1) {
                    visitorState().getProperties().put(GradlePropertyNames.MICRONAUT_VERSION, arguments.getExpression(0).getText());
                    return ExpressionVisitOutcome.PROCESSED;
                }
            }
            throw new RuntimeException("Unexpected format of version in micronaut block");
        }

        return ExpressionVisitOutcome.IGNORED;
    }
}
