/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.amqp.input;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.order.dto.input.OrderCancelRequestDto;
import org.springframework.util.Assert;

/**
 * A request event to cancel an order {@link OrderCancelRequestDto}
 *
 * @author Stephane Cortine
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class OrderCancelRequestDtoEvent extends OrderCancelRequestDto implements ISubscribable {

    public OrderCancelRequestDtoEvent(Long orderId, String correlationId) {
        super(orderId, correlationId);
        Assert.notNull(orderId, "orderId is mandatory for OrderCancelRequestDtoEvent");
        Assert.notNull(correlationId, "correlationId is mandatory for OrderCancelRequestDtoEvent");
    }
}
