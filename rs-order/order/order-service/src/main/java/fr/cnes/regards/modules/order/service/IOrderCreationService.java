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
package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.service.utils.OrderCounts;

public interface IOrderCreationService {

    /**
     * Asynchronous method called by createOrder to complete order creation. This method cannot be transactional (due
     * to proxyfication and thread-context execution) so it @see {@link IOrderCreationService#completeOrderCreation} calls next one after forcing given tenant
     *
     * @param basket  basket used to create order (removed at the end of the method)
     * @param owner   username of the order owner
     * @param orderId created order to be completed
     * @param role    current user role
     * @param tenant  current tenant
     */
    void asyncCompleteOrderCreation(Basket basket,
                                    String owner,
                                    Long orderId,
                                    int subOrderDuration,
                                    String role,
                                    String tenant);

    /**
     * Transactional completeOrderCreation method (must be called AFTER forcing tenant)
     *
     * @param basket  basket used to create order (removed at the end of the method)
     * @param orderId created order to be completed
     * @param role    user role
     * @param tenant  current tenant
     */
    void completeOrderCreation(Basket basket, Long orderId, String role, int subOrderDuration, String tenant);

    /**
     * Process a dataset of an order, see {@link this#completeOrderCreation(Basket, Long, String, int, String)}
     *
     * @param order            order to process
     * @param owner            order creator
     * @param role             owner's role
     * @param priority         order priority
     * @param orderCounts      metadata about the order processing
     * @param dsSel            dataset currently processed
     * @param subOrderDuration suborder availability duration in hours
     * @return updated order metadata
     * @throws ModuleException in dataset could not be processed
     */
    OrderCounts manageDatasetSelection(Order order,
                                       String owner,
                                       String role,
                                       int priority,
                                       OrderCounts orderCounts,
                                       BasketDatasetSelection dsSel,
                                       int subOrderDuration) throws ModuleException;
}
