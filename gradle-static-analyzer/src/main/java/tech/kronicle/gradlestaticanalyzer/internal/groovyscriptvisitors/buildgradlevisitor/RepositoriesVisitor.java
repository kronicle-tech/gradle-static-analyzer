package tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor;

import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.slf4j.Logger;
import tech.kronicle.gradlestaticanalyzer.internal.constants.SoftwareRepositoryUrls;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.BaseVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.ExpressionVisitOutcome;
import tech.kronicle.gradlestaticanalyzer.internal.services.CustomRepositoryRegistry;

import static java.util.Objects.nonNull;

@Slf4j
public class RepositoriesVisitor extends BaseVisitor {

    private final MavenRepositoryVisitor mavenRepositoryVisitor;
    private final CustomRepositoryRegistry customRepositoryRegistry;

    public RepositoriesVisitor(
            BaseVisitorDependencies dependencies,
            MavenRepositoryVisitor mavenRepositoryVisitor,
            CustomRepositoryRegistry customRepositoryRegistry
    ) {
        super(dependencies);
        this.mavenRepositoryVisitor = mavenRepositoryVisitor;
        this.customRepositoryRegistry = customRepositoryRegistry;
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected ExpressionVisitOutcome processMethodCallExpression(MethodCallExpression call) {
        String methodName = call.getMethodAsString();

        if (methodName.equals("maven")) {
            visit(call.getArguments(), mavenRepositoryVisitor);
            return ExpressionVisitOutcome.PROCESSED;
        }

        String url = getRepositoryUrl(methodName);

        if (nonNull(url)) {
            addSoftwareRepository(url);
            return ExpressionVisitOutcome.PROCESSED;
        }

        return ExpressionVisitOutcome.IGNORED;
    }

    private String getRepositoryUrl(String methodName) {
        if (methodName.equals("gradlePluginPortal")) {
            return SoftwareRepositoryUrls.GRADLE_PLUGIN_PORTAL;
        } else if (methodName.equals("mavenCentral")) {
            return SoftwareRepositoryUrls.MAVEN_CENTRAL;
        } else if (methodName.equals("jcenter")) {
            return SoftwareRepositoryUrls.JCENTER;
        } else if (methodName.equals("google")) {
            return SoftwareRepositoryUrls.GOOGLE;
        } else {
            return customRepositoryRegistry.getCustomRepositoryUrl(methodName);
        }
    }
}
