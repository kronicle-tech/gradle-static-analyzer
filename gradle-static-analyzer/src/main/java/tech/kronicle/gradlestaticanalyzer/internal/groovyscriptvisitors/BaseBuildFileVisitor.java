package tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors;

import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.PluginsVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.RepositoriesVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.services.PluginProcessor;

public abstract class BaseBuildFileVisitor extends BaseVisitor {

    private final PluginsVisitor pluginsVisitor;
    private final RepositoriesVisitor repositoriesVisitor;
    private final PluginProcessor pluginProcessor;

    public BaseBuildFileVisitor(BaseBuildFileVisitorDependencies dependencies) {
        super(dependencies.getBaseDependencies());
        pluginsVisitor = dependencies.getPluginsVisitor();
        repositoriesVisitor = dependencies.getRepositoriesVisitor();
        pluginProcessor = dependencies.getPluginProcessor();
    }

    @Override
    protected ExpressionVisitOutcome processMethodCallExpression(MethodCallExpression call) {
        if (call.getMethodAsString().equals("import")) {
            log().debug("Found import");
            return ExpressionVisitOutcome.PROCESSED;
        } else if (call.getMethodAsString().equals("plugins")) {
            if (visitorState().getProcessPhase() == ProcessPhase.PLUGINS) {
                log().debug("Found plugins");
                int count = getPluginCount();
                visit(call.getArguments(), pluginsVisitor);
                log().debug("Found {} plugins", getPluginCount() - count);
                return ExpressionVisitOutcome.PROCESSED;
            } else {
                return ExpressionVisitOutcome.IGNORED_NO_WARNING;
            }
        } else if (call.getMethodAsString().equals("repositories")) {
            if (visitorState().getProcessPhase() == getRepositoriesProcessPhase()) {
                log().debug("Found repositories");
                int count = getSoftwareRepositories().size();
                visit(call.getArguments(), repositoriesVisitor);
                log().debug("Found {} repositories", getSoftwareRepositories().size() - count);
                return ExpressionVisitOutcome.PROCESSED;
            } else {
                return ExpressionVisitOutcome.IGNORED_NO_WARNING;
            }
        }

        return super.processMethodCallExpression(call);
    }

    protected ProcessPhase getRepositoriesProcessPhase() {
        return ProcessPhase.REPOSITORIES;
    }

    @Override
    protected ExpressionVisitOutcome processBinaryExpression(BinaryExpression expression) {
        if (expression.getLeftExpression() instanceof PropertyExpression) {
            if (visitorState().getProcessPhase() == ProcessPhase.PROPERTIES) {
                log().debug("Found property");
                Expression object = ((PropertyExpression) expression.getLeftExpression()).getObjectExpression();

                if (object instanceof VariableExpression && ((VariableExpression) object).getName().equals("ext")) {
                    return processAssignment(expression);
                } else {
                    return ExpressionVisitOutcome.IGNORED;
                }
            } else {
                return ExpressionVisitOutcome.IGNORED_NO_WARNING;
            }
        }

        return super.processBinaryExpression(expression);
    }

    protected int getPluginCount() {
        return pluginProcessor.getPluginCount(visitorState().getSoftware());
    }
}
