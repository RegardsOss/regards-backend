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
package fr.cnes.regards.modules.delivery.service.order.zip;

import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
import fr.cnes.regards.modules.delivery.domain.exception.DeliveryOrderException;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.domain.order.zip.ZipDeliveryInfo;
import fr.cnes.regards.modules.delivery.service.order.zip.steps.DeliveryDownloadService;
import fr.cnes.regards.modules.delivery.service.order.zip.steps.DeliveryZipCreateService;
import fr.cnes.regards.modules.delivery.service.order.zip.steps.DeliveryZipUploadService;
import fr.cnes.regards.modules.delivery.service.order.zip.workspace.DeliveryDownloadWorkspaceManager;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Main class to make delivery. A zip is created and sent to a S3 remote location according to the
 * {@link DeliveryRequest} and the {@link fr.cnes.regards.modules.delivery.domain.settings.DeliverySettings} configured.
 *
 * @author Iliana Ghazali
 **/
@Service
public class OrderDeliveryZipService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderDeliveryZipService.class);

    public static final String DELIVERY_ID_LOG_KEY = "DELIVERY_CORR_ID=";

    private final IWorkspaceService workspaceService;

    private final DeliveryDownloadService downloadService;

    private final DeliveryZipCreateService zipCreateService;

    private final DeliveryZipUploadService zipUploadService;

    public OrderDeliveryZipService(IWorkspaceService workspaceService,
                                   DeliveryDownloadService downloadService,
                                   DeliveryZipUploadService zipUploadService) {
        this.workspaceService = workspaceService;
        this.downloadService = downloadService;
        this.zipCreateService = new DeliveryZipCreateService();
        this.zipUploadService = zipUploadService;
    }

    /**
     * Make a delivery in a four-steps process:
     * <ul>
     *     <li>Prepare specific delivery workspace where to download the files.</li>
     *     <li>Download files requested in the {@link DeliveryRequest} locally.</li>
     *     <li>Create a zip from these files.</li>
     *     <li>Send the zip created to a S3 remote location according to the delivery configuration.</li>
     * </ul>
     *
     * @return metadata about the zip uploaded to the S3 remote location.
     * @throws DeliveryOrderException if the delivery could not be performed.
     */
    public ZipDeliveryInfo makeDelivery(DeliveryRequest deliveryRequest) throws DeliveryOrderException {
        String correlationId = deliveryRequest.getCorrelationId();
        MDC.put(DELIVERY_ID_LOG_KEY, String.valueOf(correlationId));
        LOGGER.debug("Starting processing delivery with correlation id '{}'.", correlationId);

        DeliveryDownloadWorkspaceManager downloadWorkspaceManager = initDeliveryWorkspaceManager(correlationId);
        try {
            // 1. First prepare workspace to download files
            downloadWorkspaceManager.createDeliveryFolder();
            // 2. Get and download files requested from delivery request
            downloadService.getAndDownloadFiles(deliveryRequest, downloadWorkspaceManager);
            // 3. Create a zip from files downloaded
            ZipDeliveryInfo zipCreatedInfo = zipCreateService.createDeliveryZip(downloadWorkspaceManager);
            // 4. Send zip created to S3 configured remote location
            return zipUploadService.uploadZipToS3DeliveryServer(deliveryRequest, zipCreatedInfo);
        } finally {
            LOGGER.debug("End of delivery processing with correlation id '{}'.", correlationId);
            downloadWorkspaceManager.deleteDeliveryFolder();
            MDC.remove(DELIVERY_ID_LOG_KEY);
        }
    }

    /**
     * Initiate specific workspace manager for the delivery request
     *
     * @param correlationId unique identifier to track the delivery request
     * @return manager to handle the delivery workspace
     * @throws DeliveryOrderException if the workspace could not be initiated
     */
    @NotNull
    private DeliveryDownloadWorkspaceManager initDeliveryWorkspaceManager(String correlationId)
        throws DeliveryOrderException {
        try {
            return new DeliveryDownloadWorkspaceManager(correlationId, workspaceService.getMicroserviceWorkspace());
        } catch (IOException e) {
            // Handle error before rethrowing exception
            throw new DeliveryOrderException(String.format(
                "Could not retrieve delivery microservice workspace. Download "
                + "workspace cannot not be initiated. Cause: [%s] '%s'.",
                e.getClass().getSimpleName(),
                e.getMessage()), e);
        }
    }

}
