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
package fr.cnes.regards.modules.delivery.service.order.clean;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.service.order.zip.workspace.DeliveryDownloadWorkspaceManager;
import fr.cnes.regards.modules.order.amqp.input.OrderCancelRequestDtoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Perform cleaning tasks after delivery requests in error.
 *
 * @author Iliana Ghazali
 **/
@Service
public class CleanOrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanOrderService.class);

    private final IWorkspaceService workspaceService;

    private final IPublisher publisher;

    public CleanOrderService(IWorkspaceService workspaceService, IPublisher publisher) {
        this.workspaceService = workspaceService;
        this.publisher = publisher;
    }

    /**
     * Cleaning tasks:
     * <ul>
     *     <li>Delete download workspace if it exists.</li>
     *     <li>Publish cancel event to interrupt on-going orders.</li>
     * </ul>
     */
    public void cleanDeliveryOrder(List<DeliveryRequest> deliveryRequests) {
        List<OrderCancelRequestDtoEvent> cancelEvents = new ArrayList<>(deliveryRequests.size());
        deliveryRequests.forEach(deliveryRequest -> {
            deleteDownloadWorkspaceIfNecessary(deliveryRequest.getCorrelationId());
            if (deliveryRequest.getOrderId() != null) {
                // orderId can be null in case of DENIED response from order
                // no need to delete anyway because order is not created in this case
                cancelEvents.add(new OrderCancelRequestDtoEvent(deliveryRequest.getOrderId(),
                                                                deliveryRequest.getCorrelationId()));
            }
        });
        // send events to cancel corresponding orders
        publisher.publish(cancelEvents);
    }

    private void deleteDownloadWorkspaceIfNecessary(String correlationId) {
        try {
            DeliveryDownloadWorkspaceManager downloadWorkspaceManager = new DeliveryDownloadWorkspaceManager(
                correlationId,
                workspaceService.getMicroserviceWorkspace());
            if (downloadWorkspaceManager.isDeliveryTmpFolderPathExists()) {
                downloadWorkspaceManager.deleteDeliveryFolder();
            }
        } catch (IOException e) {
            LOGGER.warn("Not able to access download workspace for delivery with correlation id '{}'.",
                        correlationId,
                        e);
        }
    }
}
