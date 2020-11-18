package fr.cnes.regards.modules.processing.domain.dto;

import fr.cnes.regards.modules.processing.domain.PProcess;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import lombok.*;

import java.util.UUID;

@Value
public class PProcessDTO {

    UUID processId;

    String processName;

    boolean active;

    Map<String,String> processInfo;

    List<ExecutionParamDTO> params;

    public static PProcessDTO fromProcess(PProcess p) {
        return new PProcessDTO(
                p.getProcessId(),
                p.getProcessName(),
                p.isActive(),
                p.getProcessInfo(),
                p.getParameters().map(ExecutionParamDTO::fromProcessParam).toList()
        );
    }

}
