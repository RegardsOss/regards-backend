package fr.cnes.regards.modules.processing.domain.size;

import lombok.Value;

@Value
public class FileSetStatistics {

    String originDataset;
    int fileNumber;
    long totalBytes;

}
