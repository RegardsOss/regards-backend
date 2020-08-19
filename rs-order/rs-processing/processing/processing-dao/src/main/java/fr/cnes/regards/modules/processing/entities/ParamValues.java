package fr.cnes.regards.modules.processing.entities;

import fr.cnes.regards.modules.processing.domain.parameters.ExecutionStringParameterValue;
import lombok.*;

import java.util.List;

@Value

public class ParamValues {

    List<ExecutionStringParameterValue> values;

}
