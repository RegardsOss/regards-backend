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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.exception.CannotDeleteOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotPauseOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotRemoveOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotResumeOrderException;

/**
 * Order service
 * @author oroussel
 */
public interface IOrderService {

    String ORDER_TOKEN = "orderToken";

    String SCOPE = "scope";

    String ORDER_ID_KEY = "ORDER_ID";

    /**
     * Create an order
     * @param basket basket from which order is created
     * @param label label, generated when null
     * @param url frontent URL
     * @return copletely loaded order
     */
    Order createOrder(Basket basket, String label, String url) throws EntityInvalidException;

    /**
     *
     * @param basket
     * @param dsTask
     * @param bucketFiles
     * @param order
     */
    void createExternalSubOrder(DatasetTask dsTask, Set<OrderDataFile> bucketFiles, Order order);

    /**
     *
     * @param basket
     * @param dsTask
     * @param bucketFiles
     * @param order
     * @param role
     * @param priority
     */
    void createStorageSubOrder(DatasetTask dsTask, Set<OrderDataFile> bucketFiles, Order order, String role,
            int priority);

    /**
     * Asynchronous method called by createOrder to complete order creation. This method cannot be transactional (due
     * to proxyfication and thread-context execution) so it @see {@link IOrderService#completeOrderCreation} calls next one after forcing given tenant
     * @param basket basket used to create order (removed at the end of the method)
     * @param order created order to be completed
     * @param role current user role
     * @param tenant current tenant
     */
    void asyncCompleteOrderCreation(Basket basket, Order order, String role, String tenant);

    /**
     * Transactional completeOrderCreation method (must be called AFTER forcing tenant)
     * @param basket basket used to create order (removed at the end of the method)
     * @param order created order to be completed
     * @param role user role
     * @param tenant current tenant
     */
    void completeOrderCreation(Basket basket, Order order, String role, String tenant);

    /**
     * Load an order.
     * Order is simple loaded
     * @param id order id
     * @return {@link Order}
     */
    Order loadSimple(Long id);

    /**
     * Load an order.
     * Order is completely loaded
     * <b>BEWARE : this method use systematically a new transaction</b>
     * @param id order id
     * @return {@link Order}
     */
    Order loadComplete(Long id);

    /**
     * Pause an order (status is immediately updated but it's an async task)
     * @param id
     * @throws CannotPauseOrderException
     */
    void pause(Long id) throws CannotPauseOrderException;

    /**
     * Resume a paused order.
     * All associated jobs must be compatible with a PAUSED status (not running nor planned to be run)
     * @param id
     * @throws CannotResumeOrderException
     */
    void resume(Long id) throws CannotResumeOrderException;

    /**
     * Delete an order. Order must be PAUSED and effectiveley paused (ie all associated jobs must be compatible with a
     * PAUSED status (not running nor planned to be run))
     * Only associated data files are removed from database (stats are still available)
     * @param id
     * @throws CannotDeleteOrderException
     */
    void delete(Long id) throws CannotDeleteOrderException;

    /**
     * Remove completely an order. Current order must not be RUNNING,
     * @param id
     * @throws CannotRemoveOrderException
     */
    void remove(Long id) throws CannotRemoveOrderException;

    /**
     * Find all orders sorted by descending date.
     * Orders are simple loaded
     * @param pageRequest
     * @return {@link Order}s
     */
    Page<Order> findAll(Pageable pageRequest);

    /**
     * Write all orders in CSV format
     * @param writer
     * @param status
     * @param from
     * @param to
     * @throws IOException
     */
    void writeAllOrdersInCsv(BufferedWriter writer, OrderStatus status, OffsetDateTime from, OffsetDateTime to)
            throws IOException;

    default Page<Order> findAll(int pageSize) {
        return findAll(PageRequest.of(0, pageSize));
    }

    /**
     * Find all user orders sorted by descending date
     * Orders are simple loaded
     * @param user user
     * @param pageRequest
     * @param excludeStatuses statuses to exclude from the search
     * @return {@link Order}s
     */
    Page<Order> findAll(String user, Pageable pageRequest, OrderStatus... excludeStatuses);

    default Page<Order> findAll(String user, int pageSize) {
        return findAll(user, PageRequest.of(0, pageSize));
    }

    /**
     * Create a ZIP containing all currently available files. Once a file has been part of ZIP file, it will not be
     * part of another again.
     * @param orderOwner order owner
     * @param inDataFiles concerned order data files
     * @param os
     */
    void downloadOrderCurrentZip(String orderOwner, List<OrderDataFile> inDataFiles, OutputStream os);

    /**
     * Create a metalink file with all files.
     * @param orderId concerned order id
     * @param os
     */
    void downloadOrderMetalink(Long orderId, OutputStream os);

    /**
     * Scheduled method to update all current running orders completions values and all order available files count
     * values into database
     */
    void updateCurrentOrdersComputations();

    /**
     * Same method as previous one but for one tenant (hence transactionnal)
     */
    void updateTenantOrdersComputations();

    /**
     * Scheduled method to search for orders whom available files counts haven't been updated since a specific delay
     */
    void sendPeriodicNotifications();

    /**
     * Same method as previous one but for one tenant (hence transactionnal)
     */
    void sendTenantPeriodicNotifications();

    /**
     * Search for expired orders then mark them as EXPIRED
     */
    void cleanExpiredOrders();

    /**
     * Search for ONE order that has reached its expiration date and change sets its status to EXPIRED.
     * @return {@link Order}
     */
    Optional<Order> findOneOrderAndMarkAsExpired();

    /**
     * Clean expired order (pause, wait for end of pause then delete it)
     * @param order
     */
    void cleanExpiredOrder(Order order);

    /**
     * Check if the given order is really paused
     * @param orderId
     * @return
     */
    boolean isPaused(Long orderId);

    /** Tells if this order involves processing on some dataset selection */
    boolean hasProcessing(Order order);

    /**
     * Get {@link Order} by id
     * @param orderId
     * @return
     */
    Order getOrder(Long orderId);
}
