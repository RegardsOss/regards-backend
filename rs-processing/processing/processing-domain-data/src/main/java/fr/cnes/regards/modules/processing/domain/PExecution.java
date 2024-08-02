/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import io.vavr.collection.List;
import io.vavr.collection.Seq;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * This class defines executions. An execution is what triggers a process' executable,
 * by providing input files to work on.
 * <p>
 * The execution hosts steps, and potentially output files when the execution is finished.
 * <p>
 * Notably, the execution has a current status (corresponding to its latest step's status).
 *
 * @author gandrieu
 */
@Getter
@FieldDefaults(level = AccessLevel.PROTECTED)
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@With
@Builder
public class PExecution {

    /**
     * The execution ID
     */
    UUID id;

    /**
     * The execution correlation ID is provided by the client.
     * It is propagated back when returning a {@link fr.cnes.regards.modules.processing.domain.events.PExecutionResultEvent}
     * to the client.
     */
    String executionCorrelationId;

    /**
     * The batch ID this execution belongs to.
     */
    UUID batchId;

    /**
     * The batch correlation ID
     */
    String batchCorrelationId;

    /**
     * Based on the {@link #inputFiles} and the process' duration forecast, we can derive an expected duration.
     * This is used to detect executions that seem to be unresponsive,
     * and terminate them as {@link fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus#TIMED_OUT}.
     */
    Duration expectedDuration;

    /**
     * The input files for this execution.
     */
    Seq<PInputFile> inputFiles;

    /**
     * The steps that already occurred. This is a sequence and the last step must eventually have a final status.
     */
    Seq<PStep> steps;

    /**
     * The tenant for this execution. Since The database for rs-processing is not multitenant, in order to
     * be able to count the number of concurrent executions for a process in parallel for all tenants, this
     * information is kept as a field.
     */
    String tenant;

    /**
     * The user launching the execution.
     */
    String userName;

    /**
     * The process business ID.
     */
    UUID processBusinessId;

    /**
     * Date of creation of the execution.
     */
    OffsetDateTime created;

    /**
     * Date corresponding to the date of the last step in {@link #steps}
     */
    OffsetDateTime lastUpdated;

    /**
     * This information leaks from the database but needs to be kept in the domain.
     * Since executions are mutable (because of their step sequence) they must be guarded from
     * being overwritten while another thread has already made a modification. Hence, a version
     * number is kept to ensure that all modifications are incremental, and the version is
     * incremented at each update.
     */
    transient int version;

    /**
     * This information leaks from the database but needs to be kept in the domain.
     * It allows the database layer to know if it must CREATE or UPDATE the database
     * for this instance.
     */
    transient boolean persisted;

    public static PExecution create(String executionCorrelationId,
                                    UUID batchId,
                                    String batchCorrelationId,
                                    Duration expectedDuration,
                                    Seq<PInputFile> inputFiles,
                                    String tenant,
                                    String userName,
                                    UUID processBusinessId) {
        PStep registered = PStep.registered("");
        return new PExecution(UUID.randomUUID(),
                              executionCorrelationId,
                              batchId,
                              batchCorrelationId,
                              expectedDuration,
                              inputFiles,
                              List.of(registered),
                              tenant,
                              userName,
                              processBusinessId,
                              registered.getTime(),
                              registered.getTime(),
                              0,
                              false);
    }

    public PExecution addStep(PStep step) {
        return withSteps(this.steps.append(step)).withLastUpdated(step.getTime());
    }

}
