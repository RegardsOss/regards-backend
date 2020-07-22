package fr.cnes.regards.modules.processing.dto;

import fr.cnes.regards.modules.processing.domain.size.FileSetStatistics;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.With;

@Data @With
@AllArgsConstructor
@Builder(toBuilder = true)

public class PBatchRequest {

    private final String correlationId;
    private final String processName;
    private final String tenant;
    private final String user;
    private final String userRole;

    private final Map<String, String> parameters;
    private final Map<String, FileSetStatistics> filesetsByDataset;

}
