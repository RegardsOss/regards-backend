package fr.cnes.regards.modules.processing.domain.parameters;

import lombok.Value;
import lombok.With;

import java.net.URL;

@Value @With
public class ExecutionFileParameterValue {

    /** Parameter name in the dynamic execution parameters for this file. */
    String parameterName;

    /** Where to put the file once downloaded so that the execution finds it. */
    String localRelativePath;

    /** Optional content type of the file, may determine what to do with it. */
    String contentType;

    /** If the descriptor is a file, this is its location. */
    URL url;

    /** If the descriptor is a file, this is its */
    long bytes;


}
