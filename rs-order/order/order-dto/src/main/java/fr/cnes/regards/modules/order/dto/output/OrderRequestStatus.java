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
package fr.cnes.regards.modules.order.dto.output;

/**
 * Available states to indicate the progress of a {@link fr.cnes.regards.modules.order.dto.input.OrderRequestDto}
 *
 * <pre>
 *     OrderRequest sent
 *        ____|____
 *       /         \
 *      /           \
 *    DENIED      GRANTED
 *                   |
 *                   |  loop
 *                   |_____________ SUBORDER_DONE (a suborder is done if at least one file is available to download)
 *                   |
 *                   |
 *           ________|______________
 *          |                      |
 *      FAILED                  DONE
 * (>0 suborder failed) (0 suborder failed)
 *
 * </pre>
 *
 * @author Iliana Ghazali
 **/
public enum OrderRequestStatus {
    /**
     * Request taken into account
     */
    GRANTED,
    /**
     * Request is denied by order
     */
    DENIED,
    /**
     * One of the subcommand has ended in success
     */
    SUBORDER_DONE,
    /**
     * The order has ended in success
     */
    DONE,
    /**
     * The order has failed
     */
    FAILED
}
