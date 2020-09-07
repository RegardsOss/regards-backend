package fr.cnes.regards.modules.processing.domain.parameters;

import lombok.Value;
import lombok.With;

@Value @With
public class ExecutionStringParameterValue {

    String name;

    String value;

}
