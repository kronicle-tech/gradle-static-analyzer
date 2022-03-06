package tech.kronicle.gradlestaticanalyzer.config;

import lombok.Value;

@Value
public class HttpHeaderConfig {

    String name;
    String value;
}
