package tech.kronicle.gradlestaticanalyzer.internal.services;

import tech.kronicle.gradlestaticanalyzer.internal.models.Import;

import java.util.Objects;
import java.util.Set;

public class ImportResolver {

    public Import importResolver(String value, Set<Import> imports) {
        return imports.stream()
                .filter(item -> Objects.equals(item.getAliasName(), value))
                .findFirst().orElse(null);
    }
}
