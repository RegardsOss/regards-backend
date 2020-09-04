package fr.cnes.regards.modules.processing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class ProcessingStorageConfiguration {

    @Value("${regards.processing.sharedStorage.basePath}")
    private String sharedStorageBasePath;
    @Value("${regards.processing.executionWorkdir.basePath}")
    private String executionWorkdirBasePath;

    @Bean(name = "executionWorkdirParentPath")
    public Path executionWorkdirParentPath() throws IOException {
        return Files.createDirectories(Paths.get(executionWorkdirBasePath));
    }

    @Bean(name = "sharedStorageBasePath")
    public Path sharedStorageBasePath() throws IOException {
        return Files.createDirectories(Paths.get(sharedStorageBasePath));
    }

}
