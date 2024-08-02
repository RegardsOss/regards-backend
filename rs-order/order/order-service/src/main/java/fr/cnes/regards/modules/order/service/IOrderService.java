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

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.dto.dto.OrderStatus;
import fr.cnes.regards.modules.order.domain.SearchRequestParameters;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.dto.dto.OrderStatusDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IOrderService {

    String ORDER_TOKEN = "orderToken";
    String SCOPE = "scope";
    String ORDER_ID_KEY = "ORDER_ID";
    String BASKET_OWNER_PREFIX = "ORDER_";
    String BASKET_RESTART_OWNER_PREFIX = "RESTART_ORDER_";

    default Page<Order> findAll(int pageSize) {
        return findAll(PageRequest.of(0, pageSize));
    }

    default Page<Order> findAll(String user, int pageSize) {
        return findAll(user, PageRequest.of(0, pageSize));
    }

    /**
     * Get {@link Order} by id
     *
     * @return {@link Order}
     */
    Order getOrder(Long orderId);

    /**
     * Get {@link Order#getOwner()} by id
     */
    Optional<String> getOrderOwner(Long orderId);

    /**
     * Load an order.
     * Order is simple loaded
     *
     * @param orderId order id
     * @return {@link Order}
     */
    Order loadSimple(Long orderId);

    /**
     * Load an order.
     * Order is completely loaded
     * <b>BEWARE : this method use systematically a new transaction</b>
     *
     * @param orderId order id
     * @return {@link Order}
     */
    Order loadComplete(Long orderId);

    /**
     * Find all orders sorted by descending date.
     * Orders are simple loaded
     *
     * @return {@link Order}s
     */
    Page<Order> findAll(Pageable pageRequest);

    /**
     * Find all user orders sorted by descending date
     * Orders are simple loaded
     *
     * @param user            user
     * @param excludeStatuses statuses to exclude from the search
     * @return {@link Order}s
     */
    Page<Order> findAll(String user, Pageable pageRequest, OrderStatus... excludeStatuses);

    /**
     * Find all orders thanks to filters
     */
    Page<Order> searchOrders(SearchRequestParameters filters, Pageable pageRequest);

    /**
     * Check if the given order is really paused
     */
    boolean isPaused(Long orderId);

    /**
     * Tells if this order involves processing on some dataset selection
     */
    boolean hasProcessing(Order order);

    /**
     * Create an order
     *
     * @param basket           basket from which order is created
     * @param label            label, generated when null
     * @param url              frontend URL
     * @param subOrderDuration validity period in hours
     * @return completely loaded order
     */
    Order createOrder(Basket basket, String label, String url, int subOrderDuration) throws EntityInvalidException;

    Order createOrder(Basket basket, String label, String url, int subOrderDuration, String user, String correlationId)
        throws EntityInvalidException;

    Order create(Order order);

    /**
     * Pause an order (status is immediately updated but it's an async task)
     */
    void pause(Long orderId, boolean checkConnectedUser) throws ModuleException;

    /**
     * Resume a paused order.
     * All associated jobs must be compatible with a PAUSED status (not running nor planned to be run)
     */
    void resume(Long orderId) throws ModuleException;

    /**
     * Delete an order (set only the {@link OrderStatus.DELETED} status). Order must be PAUSED and effectiveley paused (ie all associated
     * jobs must be compatible with a
     * PAUSED status (not running nor planned to be run))
     * Only associated data files are removed from database (stats are still available)
     */
    void delete(Long orderId, boolean checkConnectedUser) throws ModuleException;

    /**
     * Restart an order, using the same basket to create a new one with the exact same parameters
     *
     * @return {@link Order}
     */
    Order restart(long oldOrderId, String label, String successUrl) throws ModuleException;

    /**
     * Retry failed files in an order.
     */
    void retryErrors(Long orderId) throws ModuleException;

    /**
     * Remove completely an order. Current order must not be RUNNING,
     */
    void remove(Long orderId) throws ModuleException;

    /**
     * Write all orders in CSV format
     */
    void writeAllOrdersInCsv(BufferedWriter writer, SearchRequestParameters filters) throws IOException;

    boolean isActionAvailable(Long orderId, OrderService.Action action);

    /**
     * Check if current user has access to the order using order's owner.
     * User must validate one of the following conditions :
     * <li>have role ADMIN_PROJECT</li>
     * <li>have role ADMIN_INSTANCE</li>
     * <li>is the owner of the order</li>
     *
     * @return false if none of these conditions are validated
     */
    boolean hasCurrentUserAccessTo(String owner);

    List<OrderStatusDto> findByIdsAndStatus(Collection<Long> orderIds, Collection<OrderStatus> orderStatuses);

    void updateErrorWithMessageIfNecessary(Long orderId, @Nullable String msg);
}
