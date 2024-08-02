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
package fr.cnes.regards.modules.order.dto.input;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A request dto contains information to cancel an order.
 *
 * @author Stephane Cortine
 */
public class OrderCancelRequestDto {

    @Size(message = "provided correlationId must not exceed 255 characters.", max = 255)
    @NotNull(message = "correlationId is required to monitor this request.")
    private final String correlationId;

    @NotNull(message = "orderId is required to cancel an order.")
    private final Long orderId;

    public OrderCancelRequestDto(Long orderId, String correlationId) {
        this.orderId = orderId;
        this.correlationId = correlationId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

}
