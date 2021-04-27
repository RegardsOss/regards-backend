/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.exception.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.time.OffsetDateTime;

public interface IOrderService {

    String ORDER_TOKEN = "orderToken";
    String SCOPE = "scope";
    String ORDER_ID_KEY = "ORDER_ID";
    String BASKET_OWNER_PREFIX = "ORDER_";

    default Page<Order> findAll(int pageSize) {
        return findAll(PageRequest.of(0, pageSize));
    }

    default Page<Order> findAll(String user, int pageSize) {
        return findAll(user, PageRequest.of(0, pageSize));
    }

    /**
     * Get {@link Order} by id
     *
     * @param orderId
     * @return {@link Order}
     */
    Order getOrder(Long orderId);

    /**
     * Load an order.
     * Order is simple loaded
     *
     * @param id order id
     * @return {@link Order}
     */
    Order loadSimple(Long id);

    /**
     * Load an order.
     * Order is completely loaded
     * <b>BEWARE : this method use systematically a new transaction</b>
     *
     * @param id order id
     * @return {@link Order}
     */
    Order loadComplete(Long id);

    /**
     * Find all orders sorted by descending date.
     * Orders are simple loaded
     *
     * @param pageRequest
     * @return {@link Order}s
     */
    Page<Order> findAll(Pageable pageRequest);

    /**
     * Find all user orders sorted by descending date
     * Orders are simple loaded
     *
     * @param user            user
     * @param pageRequest
     * @param excludeStatuses statuses to exclude from the search
     * @return {@link Order}s
     */
    Page<Order> findAll(String user, Pageable pageRequest, OrderStatus... excludeStatuses);

    /**
     * Check if the given order is really paused
     *
     * @param orderId
     * @return
     */
    boolean isPaused(Long orderId);

    /**
     * Tells if this order involves processing on some dataset selection
     */
    boolean hasProcessing(Order order);

    /**
     * Create an order
     *
     * @param basket basket from which order is created
     * @param label  label, generated when null
     * @param url    frontend URL
     * @param subOrderDuration validity period in hours
     * @return completely loaded order
     */
    Order createOrder(Basket basket, String label, String url, int subOrderDuration) throws EntityInvalidException;

    /**
     * Create an order
     *
     * @param basket basket from which order is created
     * @param label  label, generated when null
     * @param url    frontend URL
     * @param subOrderDuration validity period in hours
     * @param role   role to be used for creation
     * @return copletely loaded order
     */
    Order createOrder(Basket basket, String label, String url, int subOrderDuration, String role) throws EntityInvalidException;

    /**
     * Pause an order (status is immediately updated but it's an async task)
     *
     * @param id
     * @throws ModuleException
     */
    void pause(Long id) throws ModuleException;

    /**
     * Resume a paused order.
     * All associated jobs must be compatible with a PAUSED status (not running nor planned to be run)
     *
     * @param id
     * @throws ModuleException
     */
    void resume(Long id) throws ModuleException;

    /**
     * Delete an order. Order must be PAUSED and effectiveley paused (ie all associated jobs must be compatible with a
     * PAUSED status (not running nor planned to be run))
     * Only associated data files are removed from database (stats are still available)
     *
     * @param id
     * @throws ModuleException
     */
    void delete(Long id) throws ModuleException;

    /**
     * Restart an order, using the same basket to create a new one with the exact same parameters
     *
     * @param oldOrderId
     * @param label
     * @param successUrl
     * @return {@link Order}
     * @throws ModuleException
     */
    Order restart(long oldOrderId, String label, String successUrl) throws ModuleException;

    /**
     * Retry failed files in an order.
     *
     * @param orderId
     * @throws ModuleException
     */
    void retryErrors(long orderId) throws ModuleException;

    /**
     * Remove completely an order. Current order must not be RUNNING,
     *
     * @param id
     * @throws CannotRemoveOrderException
     */
    void remove(Long id) throws CannotRemoveOrderException;

    /**
     * Write all orders in CSV format
     *
     * @param writer
     * @param status
     * @param from
     * @param to
     * @throws IOException
     */
    void writeAllOrdersInCsv(BufferedWriter writer, OrderStatus status, OffsetDateTime from, OffsetDateTime to) throws IOException;

    boolean isActionAvailable(long orderId, OrderService.Action action);

}
