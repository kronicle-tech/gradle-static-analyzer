package tech.kronicle.gradlestaticanalyzer.internal.models;

import lombok.Value;

@Value
public class Import {

    String className;
    String aliasName;
}
