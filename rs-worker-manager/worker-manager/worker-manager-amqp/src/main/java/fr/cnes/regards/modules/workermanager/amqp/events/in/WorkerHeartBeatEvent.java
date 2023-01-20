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
package fr.cnes.regards.modules.workermanager.amqp.events.in;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.workercommon.dto.WorkerHeartBeat;

import java.time.OffsetDateTime;

/**
 * The heart beat received by the worker manager
 *
 * @author Léo Mieulet
 */
@Event(target = Target.ALL, converter = JsonMessageConverter.GSON, routingKey = "#", autoDelete = true)
public class WorkerHeartBeatEvent extends WorkerHeartBeat implements ISubscribable {

    public WorkerHeartBeatEvent(String id, String type, OffsetDateTime heartBeatDate) {
        this.setId(id);
        this.setType(type);
        this.setHeartBeatDate(heartBeatDate);
    }
}