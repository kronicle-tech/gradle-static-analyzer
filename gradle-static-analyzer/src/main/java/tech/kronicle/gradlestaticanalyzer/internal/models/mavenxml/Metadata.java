package tech.kronicle.gradlestaticanalyzer.internal.models.mavenxml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import tech.kronicle.gradlestaticanalyzer.internal.models.mavenxml.metadata.Versioning;

@Data
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Metadata {

    @XmlElement
    String groupId;
    @XmlElement
    String artifactId;
    @XmlElement
    String version;
    @XmlElement
    Versioning versioning;
}