package tech.kronicle.gradlestaticanalyzer.internal.models.mavenxml.project;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;
import tech.kronicle.gradlestaticanalyzer.internal.models.mavenxml.DependenciesContainer;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class DependencyManagement implements DependenciesContainer {

    @XmlElement
    private Dependencies dependencies;
}
