package fr.cnes.regards.modules.processing.storage;

import lombok.Value;

import java.nio.file.Path;

@Value
public class ExecutionLocalWorkdir {

    Path basePath;

    public Path inputFolder() {
        return basePath.resolve("input");
    }

    public Path outputFolder() {
        return basePath.resolve("output");
    }

}
