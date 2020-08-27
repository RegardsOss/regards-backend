package fr.cnes.regards.modules.processing.dto;

import fr.cnes.regards.modules.processing.domain.PProcess;
import io.vavr.collection.List;
import lombok.*;

import java.util.UUID;

@Value
public class PProcessDTO {

    UUID businessId;

    String name;

    boolean active;

    String tenant;

    String userRoles;

    List<Long> datasets;

    List<ExecutionParamDTO> params;

    public static PProcessDTO fromProcess(PProcess p) {
        return new PProcessDTO(
                p.getBusinessId(),
                p.getProcessName(),
                p.isActive(),
                p.getTenant(),
                p.getUserRole(),
                p.getDatasets().toList(),
                p.getParameters().map(ExecutionParamDTO::fromProcessParam).toList()
        );
    }

}
