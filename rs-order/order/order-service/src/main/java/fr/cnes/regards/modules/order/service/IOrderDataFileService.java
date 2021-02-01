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

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;

/**
 * OrderDataFile specific service (OrderDataFiles are detached entities from Order, DatasetTasks and FilesTasks)
 * @author oroussel
 */
public interface IOrderDataFileService {

    /**
     * Simply save OrderDataFile in database, no more action is done. This method is to be used at sub-orders creation.
     * @param dataFiles
     * @return {@link OrderDataFile}s
     */
    Iterable<OrderDataFile> create(Iterable<OrderDataFile> dataFiles);

    /**
     * Save given OrderDataFile, search for associated files task, update its end state then update associated order
     * waiting for user flag
     * @param dataFile
     * @return {@link OrderDataFile}
     */
    OrderDataFile save(OrderDataFile dataFile);

    /**
     * Save given OrderDataFiles, search for associated files task, update them end state then update associated order
     * waiting for user flag
     * @param dataFiles
     * @return {@link OrderDataFile}s
     */
    Iterable<OrderDataFile> save(Iterable<OrderDataFile> dataFiles);

    void launchNextFilesTasks(Iterable<OrderDataFile> dataFiles);

    OrderDataFile load(Long dataFileId) throws NoSuchElementException;

    OrderDataFile find(Long orderId, UniformResourceName aipId, String checksum) throws NoSuchElementException;

    /**
     * Find all OrderDataFile with state AVAILABLE associated to given order
     * @param orderId id of order
     * @return {@link OrderDataFile}s
     */
    List<OrderDataFile> findAllAvailables(Long orderId);

    /**
     * Find all OrderDataFile of given order
     * @param orderId if of order
     * @return {@link OrderDataFile}s
     */
    List<OrderDataFile> findAll(Long orderId);

    /**
     * Copy asked file from storage to HttpServletResponse
     * @param dataFile
     * @param asUser Download file as the given user or empty to use security context user
     * @param os
     * @return
     * @throws IOException
     */
    ResponseEntity<InputStreamResource> downloadFile(OrderDataFile dataFile, Optional<String> asUser);

    /**
     * Search all current orders (ie not finished), compute and update completion values (percentCompleted and files in
     * error count).
     * Search all orders (eventually finished), compute available files count and update values.
     * THIS METHOD DON'T UPDATE ANYTHING INTO DATABASE (it concerns Orders so it is the responsibility of OrderService,
     * @see IOrderService#updateTenantOrdersComputations )
     * @return updated orders
     */
    Set<Order> updateCurrentOrdersComputedValues();

    /**
     * Remove all data files from an order
     * @param orderId
     */
    void removeAll(Long orderId);
}
