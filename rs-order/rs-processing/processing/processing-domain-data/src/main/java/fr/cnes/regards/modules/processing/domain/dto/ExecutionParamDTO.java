package fr.cnes.regards.modules.processing.domain.dto;

import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterDescriptor;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.With;

@Data @With
@AllArgsConstructor
@Builder(toBuilder = true)

public class ExecutionParamDTO {

    private final String name;

    private final ExecutionParameterType type;

    private final String desc;

    public static ExecutionParamDTO fromProcessParam(ExecutionParameterDescriptor p) {
        return builder().desc(p.getDesc()).name(p.getName()).build();
    }
}
