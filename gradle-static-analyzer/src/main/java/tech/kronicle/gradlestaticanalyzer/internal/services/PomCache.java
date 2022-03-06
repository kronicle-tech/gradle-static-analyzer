package tech.kronicle.gradlestaticanalyzer.internal.services;

import tech.kronicle.gradlestaticanalyzer.config.PomCacheConfig;
import tech.kronicle.utils.BaseFileCache;
import tech.kronicle.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class PomCache extends BaseFileCache {

    public PomCache(FileUtils fileUtils, PomCacheConfig config) throws IOException {
        super(fileUtils, Path.of(config.getDir()));
    }

    public Optional<String> get(String url) {
        return getFileContent(url);
    }

    public void put(String url, String jsonContent) {
        putFileContent(url, jsonContent);
    }
}
