/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import lombok.Value;
import lombok.With;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value @With

public class PExecution {

    UUID id;

    String executionCorrelationId;

    UUID batchId;

    String batchCorrelationId;

    Duration expectedDuration;

    Seq<PInputFile> inputFiles;

    Seq<PStep> steps;

    String tenant;

    String userName;

    UUID processBusinessId;

    String processName;

    OffsetDateTime created;

    OffsetDateTime lastUpdated;

    transient int version;

    transient boolean persisted;

    public static PExecution create(
            String executionCorrelationId,
            UUID batchId,
            String batchCorrelationId,
            Duration expectedDuration,
            Seq<PInputFile> inputFiles,
            String tenant,
            String userName,
            UUID processBusinessId,
            String processName
    ) {
        PStep registered = PStep.registered("");
        return new PExecution(
            UUID.randomUUID(),
            executionCorrelationId,
            batchId,
            batchCorrelationId,
            expectedDuration,
            inputFiles,
            List.of(registered),
            tenant,
            userName,
            processBusinessId,
            processName,
            registered.getTime(),
            registered.getTime(),
            0,
            false
        );
    }

    public PExecution addStep(PStep step) {
        return withSteps(this.steps.append(step)).withLastUpdated(step.getTime());
    }

}
