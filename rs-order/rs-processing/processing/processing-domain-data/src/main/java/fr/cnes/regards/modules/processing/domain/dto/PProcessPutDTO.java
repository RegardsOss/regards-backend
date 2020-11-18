package fr.cnes.regards.modules.processing.domain.dto;

import io.vavr.collection.Seq;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.With;

@Data @With
@AllArgsConstructor
@Builder(toBuilder = true)

public class PProcessPutDTO {

    private final String processName;

    private final Seq<String> tenants;

    private final Seq<String> userRoles;

    private final Seq<String> datasets;

    private final Seq<ExecutionParamDTO> parameters;

}
