package fr.cnes.regards.modules.processing.entities;

import fr.cnes.regards.modules.processing.domain.parameters.ExecutionStringParameterValue;
import lombok.*;

import java.util.List;

@Data @With
@AllArgsConstructor @NoArgsConstructor
@Builder(toBuilder = true)

public class ParamValues {

    private List<ExecutionStringParameterValue> values;

}
