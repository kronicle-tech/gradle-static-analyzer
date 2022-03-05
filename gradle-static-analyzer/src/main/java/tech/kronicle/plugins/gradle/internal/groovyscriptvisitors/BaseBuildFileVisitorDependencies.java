package tech.kronicle.plugins.gradle.internal.groovyscriptvisitors;

import lombok.Value;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor.BaseVisitorDependencies;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor.PluginsVisitor;
import tech.kronicle.plugins.gradle.internal.groovyscriptvisitors.buildgradlevisitor.RepositoriesVisitor;
import tech.kronicle.plugins.gradle.internal.services.PluginProcessor;

@Value
public class BaseBuildFileVisitorDependencies {

    BaseVisitorDependencies baseDependencies;
    PluginsVisitor pluginsVisitor;
    RepositoriesVisitor repositoriesVisitor;
    PluginProcessor pluginProcessor;
}
