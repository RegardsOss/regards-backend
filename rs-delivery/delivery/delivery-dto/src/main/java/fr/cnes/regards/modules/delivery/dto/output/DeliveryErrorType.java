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
package fr.cnes.regards.modules.delivery.dto.output;

import fr.cnes.regards.modules.order.dto.OrderErrorType;

import jakarta.annotation.Nullable;

/**
 * Type of delivery errors
 *
 * @author Iliana Ghazali
 **/
public enum DeliveryErrorType {
    /**
     * Delivery request malformed
     */
    INVALID_CONTENT,
    /**
     * User is not allowed to access delivery feature
     */
    FORBIDDEN,
    /**
     * Order requested is too big and cannot be performed
     */
    ORDER_LIMIT_REACHED,
    /**
     * Order requested contains no data to order
     */
    EMPTY_ORDER,
    /**
     * Delivery request has expired
     */
    EXPIRED,
    /**
     * Internal error occurred during the retrieval of the order
     */
    INTERNAL_ERROR,
    /**
     * Order returns more than 1 sub-order for a given Delivery request
     */
    TOO_MANY_SUBORDERS;

    /**
     * Convert a error code of rs-order {@link OrderErrorType} to error type of rs-delivery {@link DeliveryErrorType}.
     *
     * @param orderErrorType error type of rs-order.
     * @return error type of rs-delivery
     */
    public static DeliveryErrorType convert(@Nullable OrderErrorType orderErrorType) {
        if (orderErrorType == null) {
            return null;
        }
        return switch (orderErrorType) {
            case FORBIDDEN -> DeliveryErrorType.FORBIDDEN;
            case EMPTY_ORDER -> DeliveryErrorType.EMPTY_ORDER;
            case INVALID_CONTENT -> DeliveryErrorType.INVALID_CONTENT;
            case ORDER_LIMIT_REACHED -> DeliveryErrorType.ORDER_LIMIT_REACHED;
            default -> DeliveryErrorType.INTERNAL_ERROR;
        };
    }
}
