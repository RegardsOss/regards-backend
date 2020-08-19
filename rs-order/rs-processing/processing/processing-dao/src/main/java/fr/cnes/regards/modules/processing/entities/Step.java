package fr.cnes.regards.modules.processing.entities;

import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import lombok.Value;

@Value
public class Step {

    ExecutionStatus status;
    Long epochTs;
    String message;

}
