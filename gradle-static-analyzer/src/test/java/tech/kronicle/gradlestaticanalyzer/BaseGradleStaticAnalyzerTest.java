package tech.kronicle.gradlestaticanalyzer;

import tech.kronicle.gradlestaticanalyzer.internal.constants.MavenPackagings;
import tech.kronicle.sdk.models.Software;
import tech.kronicle.sdk.models.SoftwareDependencyType;
import tech.kronicle.testutils.BaseTest;
import tech.kronicle.utils.Comparators;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class BaseGradleStaticAnalyzerTest extends BaseTest {

    protected Map<SoftwareGroup, List<Software>> getSoftwareGroups(List<Software> software) {
        Map<SoftwareGroup, List<Software>> softwareGroups = software.stream()
                .sorted(Comparators.SOFTWARE)
                .collect(Collectors.groupingBy(this::softwareClassifier));

        List<Software> bomSoftware = softwareGroups.get(SoftwareGroup.BOM);

        if (nonNull(bomSoftware)) {
            bomSoftware.forEach(this::assertSoftwareFieldsAreValid);
        }

        List<Software> transitiveSoftware = softwareGroups.get(SoftwareGroup.TRANSITIVE);

        if (nonNull(transitiveSoftware)) {
            transitiveSoftware.forEach(this::assertSoftwareFieldsAreValid);
        }

        return softwareGroups;
    }

    private SoftwareGroup softwareClassifier(Software software) {
        if (Objects.equals(software.getPackaging(), MavenPackagings.BOM)) {
            return SoftwareGroup.BOM;
        } else if (Objects.equals(software.getDependencyType(), SoftwareDependencyType.DIRECT)) {
            return SoftwareGroup.DIRECT;
        } else if (Objects.equals(software.getDependencyType(), SoftwareDependencyType.TRANSITIVE)) {
            return SoftwareGroup.TRANSITIVE;
        } else {
            throw new RuntimeException("Unexpected software dependency type " + software.getDependencyType());
        }
    }

    private void assertSoftwareFieldsAreValid(Software item) {
        assertThat(item.getScannerId()).isNull();
        assertThat(item.getType()).isNotNull();
        assertThat(item.getDependencyType()).isNotNull();
        assertThat(item.getName()).isNotEmpty();
        assertThat(item.getVersion()).isNotEmpty();
        assertThat(item.getVersion()).doesNotContain("$");
    }
}
