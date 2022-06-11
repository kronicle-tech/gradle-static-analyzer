package tech.kronicle.gradlestaticanalyzer.internal.models.mavenxml;

import lombok.Data;
import tech.kronicle.gradlestaticanalyzer.internal.models.mavenxml.metadata.Versioning;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
