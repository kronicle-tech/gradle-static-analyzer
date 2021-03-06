package tech.kronicle.gradlestaticanalyzer.internal.services;

import lombok.RequiredArgsConstructor;
import tech.kronicle.common.StringEscapeUtils;
import tech.kronicle.gradlestaticanalyzer.internal.models.mavenxml.Metadata;
import tech.kronicle.gradlestaticanalyzer.internal.models.mavenxml.metadata.Versioning;
import tech.kronicle.gradlestaticanalyzer.internal.models.mavenxml.metadata.Versions;
import tech.kronicle.gradlestaticanalyzer.internal.utils.ArtifactUtils;
import tech.kronicle.sdk.models.SoftwareRepository;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static tech.kronicle.gradlestaticanalyzer.internal.utils.JaxbUnmarshallerFactory.createJaxbUnmarshaller;
import static tech.kronicle.gradlestaticanalyzer.internal.utils.XmlInputFactoryFactory.createXmlInputFactory;

@RequiredArgsConstructor
public class ArtifactVersionsFetcher {

    private final MavenRepositoryFileDownloader mavenRepositoryFileDownloader;
    private final ArtifactUtils artifactUtils;
    private final Unmarshaller unmarshaller = createJaxbUnmarshaller(Metadata.class);
    private final XMLInputFactory xmlInputFactory = createXmlInputFactory();

    public List<String> fetchArtifactVersions(String groupId, String artifactId, Set<SoftwareRepository> softwareRepositories) {
        MavenRepositoryFileDownloader.MavenFileRequestOutcome<String> xmlContent =
                mavenRepositoryFileDownloader.downloadMetadata(groupId, artifactId, softwareRepositories);

        if (xmlContent.isFailure()) {
            throw new IllegalArgumentException("Could not retrieve maven metadata file for artifact coordinates "
                    + "\"" + StringEscapeUtils.escapeString(artifactUtils.createName(groupId, artifactId))
                    + "\" from safe subset of configured repositories");
        }

        Metadata metadata = readMetadataXml(xmlContent.getOutput());
        return Optional.of(metadata).map(Metadata::getVersioning).map(Versioning::getVersions).map(Versions::getVersions)
                .orElseGet(List::of);
    }

    private Metadata readMetadataXml(String content) {
        try {
            return (Metadata) unmarshaller.unmarshal(xmlInputFactory.createXMLStreamReader(new StringReader(content)));
        } catch (JAXBException | XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }
}
