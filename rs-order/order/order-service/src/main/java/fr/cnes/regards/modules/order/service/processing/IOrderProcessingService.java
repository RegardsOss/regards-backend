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
package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.service.utils.OrderCounts;

import java.util.Collection;
import java.util.UUID;

/**
 * This interface defines signatures for the companion of IOrderService dealing with
 * dataset selections having processing.
 *
 * @author Guillaume Andrieu
 */
public interface IOrderProcessingService {

    OrderCounts manageProcessedDatasetSelection(Order order,
                                                String owner,
                                                String role,
                                                BasketDatasetSelection dsSel,
                                                String tenant,
                                                String user,
                                                String userRole,
                                                OrderCounts counts,
                                                int subOrderDuration) throws ModuleException;

    /**
     * Enqueue a ProcessExecutionJob in pending state.
     *
     * @param processJobId {@link UUID} id of the processing job to enqueue
     * @param dataFiles    {@link OrderDataFile}s to delete. Corresponding to associated processing job input files
     * @param user         {@link String} Order owner email.
     */
    void enqueuedProcessingJob(UUID processJobId, Collection<OrderDataFile> dataFiles, String user);
}
