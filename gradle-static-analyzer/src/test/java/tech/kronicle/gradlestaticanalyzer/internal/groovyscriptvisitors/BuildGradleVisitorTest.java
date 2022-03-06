package tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.codehaus.groovy.ast.ASTNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.BaseVisitorDependencies;
import tech.kronicle.gradlestaticanalyzer.internal.groovyscriptvisitors.buildgradlevisitor.PluginsVisitor;
import tech.kronicle.gradlestaticanalyzer.internal.services.ExpressionEvaluator;
import tech.kronicle.gradlestaticanalyzer.internal.services.ImportResolver;
import tech.kronicle.gradlestaticanalyzer.internal.services.PluginProcessor;
import tech.kronicle.gradlestaticanalyzer.internal.services.PropertyExpander;
import tech.kronicle.gradlestaticanalyzer.internal.services.PropertyRetriever;
import tech.kronicle.gradlestaticanalyzer.internal.utils.InheritingHashMap;
import tech.kronicle.gradlestaticanalyzer.internal.utils.InheritingHashSet;
import tech.kronicle.testutils.LogCaptor;
import tech.kronicle.utils.Comparators;
import tech.kronicle.sdk.models.Software;
import tech.kronicle.sdk.models.SoftwareType;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BuildGradleVisitorTest {

    private final BuildGradleVisitor underTest = newBuildGradleVisitor();

    private BuildGradleVisitor newBuildGradleVisitor() {
        PropertyRetriever propertyRetriever = new PropertyRetriever();
        BaseVisitorDependencies baseVisitorDependencies = new BaseVisitorDependencies(
                null,
                null,
                new ExpressionEvaluator(
                        new PropertyExpander(propertyRetriever),
                        propertyRetriever,
                        new ImportResolver()
                ),
                null
        );
        PluginProcessor pluginProcessor = new PluginProcessor();
        return new BuildGradleVisitor(
                new BaseBuildFileVisitorDependencies(
                        baseVisitorDependencies,
                        new PluginsVisitor(baseVisitorDependencies, pluginProcessor),
                        null,
                        pluginProcessor
                ),
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private final GroovyParser groovyParser = new GroovyParser();
    private LogCaptor logCaptor;

    @BeforeEach
    public void beforeEach() {
        logCaptor = new LogCaptor(underTest.getClass());
    }

    @AfterEach
    public void afterEach() {
        logCaptor.close();
    }

    @Test
    public void getPluginCountShouldHandleNullsInExistingSoftware() {
        // Given
        InheritingHashSet<Software> softwareSet = new InheritingHashSet<>();
        softwareSet.add(new Software(null, null, null, null, null, null, null, null));
        VisitorState visitorState = new VisitorState(ProcessPhase.PLUGINS, null, null, null, null, null, new InheritingHashSet<>(), null, softwareSet, new InheritingHashMap<>(), null);
        underTest.setVisitorState(visitorState, null);
        List<ASTNode> nodes = groovyParser.parseGroovy(
                "plugins {\n"
                        + "  id \"test\"\n"
                        + "}\n");

        // When
        nodes.forEach(node -> node.visit(underTest));

        // Then
        List<Software> softwareList = getSoftware(softwareSet);
        assertThat(softwareList).hasSize(2);
        Software software;
        software = softwareList.get(0);
        assertThat(software.getType()).isEqualTo(SoftwareType.GRADLE_PLUGIN);
        assertThat(software.getName()).isEqualTo("test");
        assertThat(software.getVersion()).isNull();
        assertThat(software.getPackaging()).isNull();
        assertThat(software.getScope()).isNull();
        software = softwareList.get(1);
        assertThat(software.getType()).isNull();
        assertThat(software.getName()).isNull();
        assertThat(software.getVersion()).isNull();
        assertThat(software.getPackaging()).isNull();
        assertThat(software.getScope()).isNull();

        List<ILoggingEvent> events = logCaptor.getEvents();
        assertThat(events).hasSize(2);
        ILoggingEvent event;
        event = events.get(0);
        assertThat(event.getFormattedMessage()).isEqualTo("Found plugins");
        event = events.get(1);
        assertThat(event.getFormattedMessage()).isEqualTo("Found 1 plugins");
    }

    private List<Software> getSoftware(InheritingHashSet<Software> software) {
        ArrayList<Software> list = new ArrayList<>(software);
        list.sort(Comparators.SOFTWARE);
        return list;
    }
}
