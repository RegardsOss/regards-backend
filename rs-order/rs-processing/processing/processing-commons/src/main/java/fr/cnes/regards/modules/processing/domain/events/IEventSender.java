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
package fr.cnes.regards.modules.processing.domain.events;

import fr.cnes.regards.framework.amqp.event.ISubscribable;
import reactor.core.publisher.Mono;

/**
 * Interface defining the signature to send an event in te context of a tenant, giving back a Mono of the event.
 *
 * @param <M> the message type
 *
 * @author gandrieu
 */
public interface IEventSender<M extends ISubscribable> {

    Mono<M> send(String tenant, M message);

}
