package tech.kronicle.gradlestaticanalyzer.internal.services;


import java.util.Map;

public class PropertyRetriever {

    public String getPropertyValue(String propertyName, Map<String, String> properties) {
        return properties.get(propertyName);
    }
}
