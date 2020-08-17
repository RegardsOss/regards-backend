package fr.cnes.regards.modules.processing.domain;

import fr.cnes.regards.modules.processing.domain.constraints.ExecutionQuota;
import fr.cnes.regards.modules.processing.domain.constraints.ExecutionRights;
import fr.cnes.regards.modules.processing.domain.duration.IRunningDurationForecast;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.engine.IWorkloadEngine;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterDescriptor;
import fr.cnes.regards.modules.processing.domain.size.IResultSizeForecast;
import io.vavr.collection.Seq;
import lombok.Value;

@Value

public class PProcess {

    String processName;

    boolean active;

    ExecutionQuota<Integer> maxParallelExecutionsForUser;

    ExecutionQuota<Long> maxBytesInCache;

    ExecutionRights allowedUsersRoles;

    ExecutionRights allowedDatasets;

    ExecutionRights allowedTenants;

    Seq<ExecutionParameterDescriptor> parameters;

    IResultSizeForecast resultSizeForecast;

    IRunningDurationForecast runningDurationForecast;

    IWorkloadEngine engine;

    IExecutable executable;
}
