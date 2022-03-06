package tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors;

import lombok.Value;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.BaseVisitorDependencies;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.PluginsVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.RepositoriesVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.services.PluginProcessor;

@Value
public class BaseBuildFileVisitorDependencies {

    BaseVisitorDependencies baseDependencies;
    PluginsVisitor pluginsVisitor;
    RepositoriesVisitor repositoriesVisitor;
    PluginProcessor pluginProcessor;
}
