package tech.kronicle.gradlestaticanalyzer.internal.models.mavenxml.project;

import lombok.Data;
import tech.kronicle.gradlestaticanalyzer.internal.models.mavenxml.ProjectCoordinates;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Parent implements ProjectCoordinates {

    @XmlElement
    private String groupId;
    @XmlElement
    private String artifactId;
    @XmlElement
    private String version;
    @XmlElement
    private String packaging;
}
