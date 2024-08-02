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

/**
 * Available states to indicate the progress of a {@link fr.cnes.regards.modules.delivery.dto.input.DeliveryRequestDto}
 *
 * <pre>
 *     DeliveryRequestDto sent
 *        ____|____
 *       /         \
 *      /           \
 *    DENIED      GRANTED
 *                   |
 *                   |
 *           ________|______________
 *          |                      |
 *      ERROR                    DONE
 *
 * </pre>
 *
 * @author Iliana Ghazali
 **/
public enum DeliveryRequestStatus {
    /**
     * Request validated and ready to be processed
     */
    GRANTED,
    /**
     * Request is not valid and cannot be processed
     */
    DENIED,
    /**
     * Request has ended in success
     */
    DONE,
    /**
     * An error occurred during the processing of the request
     */
    ERROR
}
