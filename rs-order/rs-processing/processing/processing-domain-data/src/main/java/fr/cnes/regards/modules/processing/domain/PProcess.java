/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
*/
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
import lombok.With;

import java.util.UUID;

/**
 * This interface defines processes.
 * It is not a class to allow to inherit from something else, depending on the implementations.
 *
 * A process is an abstract definition of a program and its inputs.
 * There are two kinds of inputs:
 * - the process parameters provided at the batch creation
 * - the input files provided at the execution creation (and execution is always the child of a batch).
 *
 * Furthermore, a process has several associated metadata, among which:
 * - free information (key/value pairs) in {@link #getProcessInfo()}
 * - constraint checkers to validate that a batch/execution can be launched,
 * - size/duration forecasts,
 * - some way to associate execution outputs back to inputs.
 *
 * A process is supposed to be immutable, but some mutations are possible (process name for instance).
 *
 * @author gandrieu
 */
public interface PProcess {

    /** The process ID */
    UUID getProcessId();

    /** The process name */
    String getProcessName();

    /** The process info, which is basically a set of key/values, to be interpreted by the client. */
    Map<String, String> getProcessInfo();

    /** Whether the process is active (batches/executions can be created for it) */
    boolean isActive();

    /** Allows to validate that a batch can be launched. */
    ConstraintChecker<PBatch> getBatchChecker();

    /** Allows to validate that an execution can be launched. */
    ConstraintChecker<PExecution> getExecutionChecker();

    /** The process parameter definitions. This is only the description of the parameters.
     * The batch will need to provide the values for these parameters. */
    Seq<ExecutionParameterDescriptor> getParameters();

    /** How much data is generated by an execution of this process, absolute or depending on the input size.
     * May be used by constraint checkers. */
    IResultSizeForecast getResultSizeForecast();

    /** How much time is needed by an execution of this process, absolute or depending on the input size.
     * May be used by constraint checkers. Is used to determine if an execution has timed out. */
    IRunningDurationForecast getRunningDurationForecast();

    /** Which engine to use for launching executions. */
    IWorkloadEngine getEngine();

    /** The executable launched by the engine. */
    IExecutable getExecutable();

    /** The way to map output back to inputs. */
    IOutputToInputMapper getMapper();

    /**
     * This class is a default immutable concrete implementation for {@link PProcess}.
     */
    @Value @With
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
