package fr.cnes.regards.modules.processing.domain.dto;

import lombok.*;

import java.util.UUID;

@Value @With
@AllArgsConstructor
@Builder(toBuilder = true)

public class PBatchResponse {

    UUID batchId;
    String correlationId;

}
