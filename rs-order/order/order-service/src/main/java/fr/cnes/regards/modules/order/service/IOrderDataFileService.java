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

import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.dto.dto.OrderDataFileDTO;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * OrderDataFile specific service (OrderDataFiles are detached entities from Order, DatasetTasks and FilesTasks)
 *
 * @author oroussel
 */
public interface IOrderDataFileService {

    /**
     * Simply save OrderDataFile in database, no more action is done. This method is to be used at sub-orders creation.
     *
     * @return {@link OrderDataFile}s
     */
    Iterable<OrderDataFile> create(Iterable<OrderDataFile> dataFiles);

    /**
     * Save given OrderDataFile, search for associated files task, update its end state then update associated order
     * waiting for user flag
     *
     * @return {@link OrderDataFile}
     */
    OrderDataFile save(OrderDataFile dataFile);

    /**
     * Save given OrderDataFiles, search for associated files task, update them end state then update associated order
     * waiting for user flag
     *
     * @return {@link OrderDataFile}s
     */
    Iterable<OrderDataFile> save(Iterable<OrderDataFile> dataFiles);

    void launchNextFilesTasks(Iterable<OrderDataFile> dataFiles);

    OrderDataFile load(Long dataFileId) throws NoSuchElementException;

    OrderDataFile find(Long orderId, UniformResourceName aipId, String checksum) throws NoSuchElementException;

    /**
     * Find all OrderDataFile with state AVAILABLE associated to given order
     *
     * @param orderId id of order
     * @return {@link OrderDataFile}s
     */
    List<OrderDataFile> findAllAvailables(Long orderId);

    /**
     * Find all OrderDataFile of given order
     *
     * @param orderId if of order
     * @return {@link OrderDataFile}s
     */
    List<OrderDataFile> findAll(Long orderId);

    /**
     * Copy asked file from storage to HttpServletResponse
     *
     * @param asUser Download file as the given user or empty to use security context user
     */
    ResponseEntity<InputStreamResource> downloadFile(OrderDataFile dataFile, Optional<String> asUser);

    /**
     * Search all current orders (ie not finished), compute and update completion values (percentCompleted and files in
     * error count).
     * Search all orders (eventually finished), compute available files count and update values.
     * THIS METHOD DON'T UPDATE ANYTHING INTO DATABASE (it concerns Orders so it is the responsibility of OrderService,
     *
     * @return updated orders
     */
    Set<Order> updateCurrentOrdersComputedValues();

    /**
     * Remove all data files from an order
     */
    void removeAll(Long orderId);

    boolean hasDownloadErrors(Long orderId);

    /**
     * Get a page of available files in a specific order and optionally in a suborder (fileTaskId)
     */
    Page<OrderDataFileDTO> findAvailableDataFiles(Long orderId, @Nullable Long filesTaskId, Pageable page);

    /**
     * Does order have {@link OrderDataFile} in {@link fr.cnes.regards.modules.order.domain.FileState#AVAILABLE}
     * status
     */
    boolean hasAvailableFiles(Long orderId);

}
