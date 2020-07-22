package fr.cnes.regards.modules.processing.dto;

import fr.cnes.regards.modules.processing.domain.PProcess;
import io.vavr.collection.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.With;

@Data @With
@AllArgsConstructor
@Builder(toBuilder = true)

public class PProcessDTO {

    private final String name;

    private final List<String> tenants;

    private final List<String> userRoles;

    private final List<String> datasets;

    private final List<ExecutionParamDTO> params;

    public static PProcessDTO fromProcess(PProcess p) {
        return builder()
                .name(p.getProcessName())
                .tenants(p.getAllowedTenants().values().toList())
                .userRoles(p.getAllowedUsersRoles().values().toList())
                .datasets(p.getAllowedDatasets().values().toList())
                .params(p.getParameters().map(ExecutionParamDTO::fromProcessParam).toList())
                .build();
    }

}
