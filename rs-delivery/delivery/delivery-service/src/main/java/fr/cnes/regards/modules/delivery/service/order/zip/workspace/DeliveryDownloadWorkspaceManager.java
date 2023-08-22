/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.service.order.zip.workspace;

import fr.cnes.regards.modules.delivery.domain.exception.DeliveryOrderException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Local workspace to handle downloaded files from a delivery.
 *
 * @author Iliana Ghazali
 **/
public class DeliveryDownloadWorkspaceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveryDownloadWorkspaceManager.class);

    private static final String DOWNLOAD_SUBFOLDER = "download";

    private final String correlationId;

    private final Path deliveryTmpFolderPath;

    public DeliveryDownloadWorkspaceManager(String correlationId, Path msWorkspacePath) {
        this.correlationId = correlationId;
        this.deliveryTmpFolderPath = msWorkspacePath.resolve(correlationId);
    }

    /**
     * Create a temporary delivery subfolder in the configured delivery workspace with the provided correlation id.
     * <pre>
     * |_ delivery_workspace
     *   |_ /corrId
     *     |_ /download_subfolder
     * </pre>
     **/
    public void createDeliveryFolder() throws DeliveryOrderException {
        try {
            // init temporary delivery base folder
            Path deliveryFolderPath = Files.createDirectory(deliveryTmpFolderPath);
            // init download subfolder
            Files.createDirectory(deliveryFolderPath.resolve(DOWNLOAD_SUBFOLDER));

            LOGGER.debug("Successfully created workspace for delivery with correlation id '{}' at '{}'.",
                         correlationId,
                         deliveryFolderPath);
        } catch (IOException e) {
            throw new DeliveryOrderException(String.format("Could not create download folder from delivery with "
                                                           + "correlation id '%s'. Cause: [%s] '%s'.",
                                                           correlationId,
                                                           e.getClass().getSimpleName(),
                                                           e.getMessage()), e);
        }
    }

    /**
     * Delete the temporary delivery folder quietly (no exception will be thrown in case of error).
     */
    public void deleteDeliveryFolder() {
        boolean folderDeleted = FileUtils.deleteQuietly(deliveryTmpFolderPath.toFile());
        if (!folderDeleted) {
            LOGGER.warn("Not able to delete delivery folder located at '{}' for delivery with correlation id '{}'",
                        deliveryTmpFolderPath,
                        correlationId);
        } else {
            LOGGER.debug("Successfully deleted delivery folder for delivery with correlation id '{}' located at '{}'.",
                         correlationId,
                         deliveryTmpFolderPath);
        }
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Path getDeliveryTmpFolderPath() {
        return deliveryTmpFolderPath;
    }

    public Path getDownloadSubfolder() {
        return this.deliveryTmpFolderPath.resolve(DOWNLOAD_SUBFOLDER);
    }
}
