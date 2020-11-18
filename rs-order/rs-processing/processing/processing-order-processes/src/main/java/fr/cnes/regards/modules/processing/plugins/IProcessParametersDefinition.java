package fr.cnes.regards.modules.processing.plugins;

import fr.cnes.regards.modules.processing.domain.engine.IOutputToInputMapper;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterDescriptor;
import io.vavr.collection.Seq;

public interface IProcessParametersDefinition {

    Seq<ExecutionParameterDescriptor> parameters();

    default IOutputToInputMapper inputOutputMapper() { return IOutputToInputMapper.noMapping(); }

}
