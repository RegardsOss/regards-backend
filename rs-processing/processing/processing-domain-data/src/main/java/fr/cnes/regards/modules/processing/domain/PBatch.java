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

import fr.cnes.regards.modules.processing.domain.parameters.ExecutionStringParameterValue;
import fr.cnes.regards.modules.processing.domain.size.FileSetStatistics;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import lombok.Value;
import lombok.With;

import java.util.UUID;

/**
 * This class defines a batch of executions for a process.
 *
 * A batch is a logical group of executions, all belonging to the same "group",
 * whatever that means in context of the client. Once a batch is created though,
 * there is no limit on how many executions can be created within this batch.
 * A batch may perfectly have only one execution.
 *
 * A batch is given the parameter values to use for the process' parameters.
 *
 * A batch is immutable.
 *
 * @author gandrieu
 */
@Value @With
public class PBatch {

    /** The batch correlation ID, provided by the client and given back on each execution result. */
    String correlationId;

    /** The batch ID. */
    UUID id;

    /** The process ID. */
    UUID processBusinessId;

    String tenant;

    String user;

    String userRole;

    Seq<ExecutionStringParameterValue> userSuppliedParameters;

    Map<String, FileSetStatistics> filesetsByDataset;

    /** This information leaks from the database but needs to be kept in the domain.
     * It allows the database layer to know if it must CREATE or UPDATE the database
     * for this instance. */
    transient boolean persisted;

    public PBatch asNew() { return this.withId(UUID.randomUUID()).withPersisted(false); }

}
