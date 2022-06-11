package tech.kronicle.gradlestaticanalyzer.internal.models.mavenxml.project;

import lombok.Data;
import tech.kronicle.gradlestaticanalyzer.internal.models.mavenxml.DependenciesContainer;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class DependencyManagement implements DependenciesContainer {

    @XmlElement
    private Dependencies dependencies;
}
