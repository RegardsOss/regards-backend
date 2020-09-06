package fr.cnes.regards.modules.processing.dto;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import lombok.Value;

import java.net.URL;
import java.util.UUID;

@Value
public class POutputFileDTO {

    UUID id;

    UUID execId;

    URL url;

    String name;

    long size;

    String checksumMethod;
    String checksumValue;

    public static POutputFileDTO toDto(POutputFile outFile) {
        return new POutputFileDTO(
            outFile.getId(),
            outFile.getExecId(),
            outFile.getUrl(),
            outFile.getName(),
            outFile.getSize(),
            outFile.getChecksum().getMethod(),
            outFile.getChecksum().getValue()
        );
    }

}
