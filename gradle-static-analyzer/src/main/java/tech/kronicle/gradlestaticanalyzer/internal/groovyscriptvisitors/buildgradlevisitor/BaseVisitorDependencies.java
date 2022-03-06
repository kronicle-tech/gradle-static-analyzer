package tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor;

import lombok.Value;
import tech.kronicle.gradlestaticanalyzer.internal.services.BuildFileLoader;
import tech.kronicle.gradlestaticanalyzer.internal.services.BuildFileProcessor;
import tech.kronicle.gradlestaticanalyzer.internal.services.ExpressionEvaluator;
import tech.kronicle.gradlestaticanalyzer.internal.services.SoftwareRepositoryFactory;

@Value
public class BaseVisitorDependencies {

    BuildFileLoader buildFileLoader;
    BuildFileProcessor buildFileProcessor;
    ExpressionEvaluator expressionEvaluator;
    SoftwareRepositoryFactory softwareRepositoryFactory;
}
