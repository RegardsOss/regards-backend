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
package fr.cnes.regards.modules.order.client.amqp;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.order.amqp.input.OrderRequestDtoEvent;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nullable;
import java.util.List;

/**
 * see {@link IAutoOrderRequestClient}
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class AutoOrderRequestClient implements IAutoOrderRequestClient {

    private final IPublisher publisher;

    public AutoOrderRequestClient(IPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishOrderRequestEvents(List<OrderRequestDto> orderRequests, @Nullable Long orderSizeLimitOverride) {
        List<OrderRequestDtoEvent> orderRequestEvents = orderRequests.stream()
                                                                     .map(orderRequestDto -> new OrderRequestDtoEvent(
                                                                         orderRequestDto.getQueries(),
                                                                         orderRequestDto.getFilters(),
                                                                         orderRequestDto.getCorrelationId(),
                                                                         orderRequestDto.getUser(),
                                                                         orderSizeLimitOverride != null ?
                                                                             orderSizeLimitOverride :
                                                                             orderRequestDto.getSizeLimitInBytes()))
                                                                     .toList();
        if (!orderRequests.isEmpty()) {
            publisher.publish(orderRequestEvents);
        }
    }
}
