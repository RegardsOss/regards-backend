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
package fr.cnes.regards.modules.processing.domain.events;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.processing.domain.dto.POutputFileDTO;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import lombok.Value;
import lombok.With;

import java.util.UUID;

@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
@Value @With
public class PExecutionResultEvent implements ISubscribable {

    /** The execution this message was related to. */
    UUID executionId;

    /** The execution correlation ID. */
    String executionCorrelationId;

    /** The batch the execution this message was related to was related to. */
    UUID batchId;

    /** The batch correlation ID */
    String batchCorrelationId;

    /** The process ID */
    UUID processId;

    /** The process information */
    Map<String, String> processInfo;

    /** The execution status */
    ExecutionStatus finalStatus;

    /** Used in case of success, the links to the resulting files generated by the execution. */
    Seq<POutputFileDTO> outputs;

    /** Used in case of failure, to provide context for the failure. */
    Seq<String> messages;

}
