package tech.kronicle.gradlestaticanalyzer.internal.services;

import tech.kronicle.gradlestaticanalyzer.config.DownloadCacheConfig;
import tech.kronicle.utils.BaseFileCache;
import tech.kronicle.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class DownloadCache extends BaseFileCache {

    public DownloadCache(FileUtils fileUtils, DownloadCacheConfig config) throws IOException {
        super(fileUtils, Path.of(config.getDir()));
    }

    public Optional<String> getContent(String url) {
        return getFileContent(url);
    }

    public void putContent(String url, String content) {
        putFileContent(url, content);
    }
}
