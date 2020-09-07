package fr.cnes.regards.modules.processing.domain.parameters;

import lombok.Value;
import lombok.With;

@Value @With
public class ExecutionParameterDescriptor {

    /** Name of the parameter. */
    String name;

    /** Which type of parameter value is expected. */
    ExecutionParameterType type;

    /** Short description of the parameter. */
    String desc;

    /** True if the parameter can be ommitted. */
    boolean optional;

    /** True if several values can be given for this parameter name */
    boolean repeatable;

    /** True if the end-user must provide the value for this parameter,
     *  meaning that the actual value has to be defined at the creation of the batch.
     * (False if it can be inferred from the context of execution.) */
    boolean userDefined;

}
