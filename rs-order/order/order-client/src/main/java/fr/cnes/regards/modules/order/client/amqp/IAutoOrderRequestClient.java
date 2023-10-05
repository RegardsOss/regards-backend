/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Interface to create automatically orders from {@link OrderRequestDto}s via amqp messages.
 *
 * @author Iliana Ghazali
 **/
public interface IAutoOrderRequestClient {

    /**
     * Create {@link fr.cnes.regards.modules.order.amqp.input.OrderRequestDtoEvent} from given {@link OrderRequestDto}.
     *
     * @param orderSizeLimitOverride If orderSizeLimitOverride parameter is provided, the value is overridden in all
     *                               OrderRequestDtoEvent created.
     * @param orderRequests          requests containing metadata to create an order
     */
    void publishOrderRequestEvents(List<OrderRequestDto> orderRequests, @Nullable Long orderSizeLimitOverride);

}
