package fr.cnes.regards.modules.processing.domain.size;

import lombok.Value;

@Value
public class FileSetStatistics {

    /** The dataset ID this file set comes from*/
    String dataset;
    /** The total number of executions to be launched */
    int executionCount;
    /** The total number of bytes to be treated */
    long totalBytes;

}
