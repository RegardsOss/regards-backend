package fr.cnes.regards.modules.processing.domain;

import fr.cnes.regards.modules.processing.domain.constraints.ConstraintChecker;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.engine.IOutputToInputMapper;
import fr.cnes.regards.modules.processing.domain.engine.IWorkloadEngine;
import fr.cnes.regards.modules.processing.domain.forecast.IResultSizeForecast;
import fr.cnes.regards.modules.processing.domain.forecast.IRunningDurationForecast;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterDescriptor;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import lombok.Value;

import java.util.UUID;

public interface PProcess {

    UUID getProcessId();

    String getProcessName();

    Map<String, String> getProcessInfo();

    boolean isActive();

    ConstraintChecker<PBatch> getBatchChecker();

    ConstraintChecker<PExecution> getExecutionChecker();

    Seq<ExecutionParameterDescriptor> getParameters();

    IResultSizeForecast getResultSizeForecast();

    IRunningDurationForecast getRunningDurationForecast();

    IWorkloadEngine getEngine();

    IExecutable getExecutable();

    IOutputToInputMapper getMapper();

    @Value
    class ConcretePProcess implements PProcess {
        UUID processId;
        String processName;
        Map<String, String> processInfo;
        boolean active;
        ConstraintChecker<PBatch> batchChecker;
        ConstraintChecker<PExecution> executionChecker;
        Seq<ExecutionParameterDescriptor> parameters;
        IResultSizeForecast resultSizeForecast;
        IRunningDurationForecast runningDurationForecast;
        IWorkloadEngine engine;
        IExecutable executable;
        IOutputToInputMapper mapper;
    }

}
