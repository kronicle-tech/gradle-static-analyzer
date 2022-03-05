package tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor;

import lombok.Value;
import tech.kronicle.plugins.gradle.internal.services.BuildFileLoader;
import tech.kronicle.plugins.gradle.internal.services.BuildFileProcessor;
import tech.kronicle.plugins.gradle.internal.services.ExpressionEvaluator;
import tech.kronicle.plugins.gradle.internal.services.SoftwareRepositoryFactory;

@Value
public class BaseVisitorDependencies {

    BuildFileLoader buildFileLoader;
    BuildFileProcessor buildFileProcessor;
    ExpressionEvaluator expressionEvaluator;
    SoftwareRepositoryFactory softwareRepositoryFactory;
}
