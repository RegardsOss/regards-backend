package fr.cnes.regards.modules.processing.entities;

import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import lombok.Value;

import java.util.List;

@Value
public class FileParameters {

    List<ExecutionFileParameterValue> values;

}
