package fr.cnes.regards.modules.processing.domain;

import fr.cnes.regards.modules.processing.domain.constraints.ConstraintChecker;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.engine.IWorkloadEngine;
import fr.cnes.regards.modules.processing.domain.forecast.IResultSizeForecast;
import fr.cnes.regards.modules.processing.domain.forecast.IRunningDurationForecast;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterDescriptor;
import io.vavr.collection.Seq;
import lombok.Value;

import java.util.UUID;

@Value

public class PProcess {

    UUID businessId;

    String processName;

    boolean active;

    String tenant;

    String userRole;

    Seq<Long> datasets;

    ConstraintChecker<PBatch> batchChecker;

    ConstraintChecker<PExecution> executionChecker;

    Seq<ExecutionParameterDescriptor> parameters;

    IResultSizeForecast resultSizeForecast;

    IRunningDurationForecast runningDurationForecast;

    IWorkloadEngine engine;

    IExecutable executable;
}
