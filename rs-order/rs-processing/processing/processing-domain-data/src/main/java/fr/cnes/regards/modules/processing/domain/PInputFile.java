package fr.cnes.regards.modules.processing.domain;

import lombok.Value;
import lombok.With;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.net.URL;

@Value @With
public class PInputFile {

    /** Parameter name in the dynamic execution parameters for this file. */
    @Nullable String parameterName;

    /** Where to put the file once downloaded so that the execution finds it.
     * The path is relative to the 'input' workdir folder. */
    @NonNull String localRelativePath;

    /** Optional content type of the file, may determine what to do with it. */
    @Nullable String contentType;

    /** If the descriptor is a file, this is its location. */
    @NonNull URL url;

    /** File content length in bytes */
    @NonNull Long bytes;

    /** The file checksum */
    @NonNull String checksum;

    /** True if the file is accessible from the storageClient, false if accessible from en external ressource */
    @NonNull Boolean internal;

    /** Allows to provide some correlationId for this input file. Output files can refer to this correlationId. */
    @Nullable String inputCorrelationId;

}
