package tech.kronicle.gradlestaticanalyzer;

import lombok.Value;
import tech.kronicle.sdk.models.Software;
import tech.kronicle.sdk.models.SoftwareRepository;

import java.util.List;

import static tech.kronicle.sdk.utils.ListUtils.createUnmodifiableList;

@Value
public class GradleAnalysis {

    Boolean gradleIsUsed;
    List<SoftwareRepository> softwareRepositories;
    List<Software> software;

    public GradleAnalysis(
            Boolean gradleIsUsed,
            List<SoftwareRepository> softwareRepositories,
            List<Software> software
    ) {
        this.gradleIsUsed = gradleIsUsed;
        this.softwareRepositories = createUnmodifiableList(softwareRepositories);
        this.software = createUnmodifiableList(software);
    }
}
