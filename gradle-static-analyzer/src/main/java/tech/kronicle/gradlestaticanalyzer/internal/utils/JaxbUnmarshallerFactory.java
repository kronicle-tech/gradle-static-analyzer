package tech.kronicle.gradlestaticanalyzer.internal.utils;

import lombok.SneakyThrows;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

public final class JaxbUnmarshallerFactory {

    @SneakyThrows
    public static Unmarshaller createJaxbUnmarshaller(Class<?> type) {
        return JAXBContext.newInstance(type).createUnmarshaller();
    }

    private JaxbUnmarshallerFactory() {
    }
}
