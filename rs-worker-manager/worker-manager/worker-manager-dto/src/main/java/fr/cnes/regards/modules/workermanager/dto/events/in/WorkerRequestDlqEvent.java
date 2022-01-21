/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.workermanager.dto.events.in;

import fr.cnes.regards.framework.amqp.event.*;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.modules.workermanager.dto.events.out.WorkerRequestEvent;
import org.springframework.amqp.core.MessageProperties;

import javax.validation.constraints.NotNull;

/**
 * Empty POJO to handle worker requests DLQ sent by manager with undefined body.
 *
 * Channel is configured to root message from DLX exchange to the queue to listen for.
 *
 * NOTE : declareDlq is set to false in the Event annotation to avoid creation of a dlq on the queue
 * which is already a DLQ. Is this param is set to true, interface with workers is broken as spring cloud stream
 * does not create a DLQ on the DLQ queues.
 *
 * @autor Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON,
        declareDlq=false)
public class WorkerRequestDlqEvent extends WorkerRequestEvent {

}
