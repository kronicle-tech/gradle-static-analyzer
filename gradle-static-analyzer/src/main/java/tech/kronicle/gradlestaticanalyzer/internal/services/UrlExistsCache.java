package tech.kronicle.gradlestaticanalyzer.internal.services;

import tech.kronicle.gradlestaticanalyzer.config.UrlExistsCacheConfig;
import tech.kronicle.utils.BaseFileCache;
import tech.kronicle.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class UrlExistsCache extends BaseFileCache {

    public UrlExistsCache(FileUtils fileUtils, UrlExistsCacheConfig config) throws IOException {
        super(fileUtils, Path.of(config.getDir()));
    }

    public Optional<Boolean> getExists(String url) {
        return getFileContent(url)
                .map(Boolean::parseBoolean);
    }

    public void putExists(String url, boolean exists) {
        putFileContent(url, Boolean.toString(exists));
    }
}
