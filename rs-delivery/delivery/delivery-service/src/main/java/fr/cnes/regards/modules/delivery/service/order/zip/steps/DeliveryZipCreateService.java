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
package fr.cnes.regards.modules.delivery.service.order.zip.steps;

import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.framework.utils.file.CompressToZipUtils;
import fr.cnes.regards.modules.delivery.domain.exception.DeliveryOrderException;
import fr.cnes.regards.modules.delivery.domain.zip.ZipDeliveryInfo;
import fr.cnes.regards.modules.delivery.service.order.zip.workspace.DeliveryDownloadWorkspaceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import static org.apache.commons.io.FilenameUtils.getName;
import static org.apache.commons.io.FilenameUtils.removeExtension;

/**
 * Create a delivery zip from files requested by the client.
 *
 * @author Iliana Ghazali
 **/
public class DeliveryZipCreateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveryZipCreateService.class);

    /**
     * Pattern to build zip name if it contains more than 1 entry.
     */
    private static final String MULTIPLE_FILES_ZIP_NAME_PATTERN = "delivery-%s.zip"; // delivery-<corrId>.zip

    /**
     * Create a delivery zip from files previously downloaded in the {@link DeliveryDownloadWorkspaceManager}.
     *
     * @param downloadWorkspace local workspace where the files are located.
     * @return metadata about the zip created
     * @throws DeliveryOrderException if the zip could not be created.
     */
    public ZipDeliveryInfo createDeliveryZip(DeliveryDownloadWorkspaceManager downloadWorkspace)
        throws DeliveryOrderException {
        String correlationId = downloadWorkspace.getCorrelationId();
        Path workspaceFolderPath = downloadWorkspace.getDeliveryTmpFolderPath();
        Path downloadFolderPath = downloadWorkspace.getDownloadSubfolder();
        LOGGER.debug("Starting creating zip from delivery files located at '{}'", downloadFolderPath);

        // Zip delivery files
        String zipName = getZipName(correlationId, downloadFolderPath);
        Path zipPath = workspaceFolderPath.resolve(zipName);
        try {
            CompressToZipUtils.compressDirectoriesToZip(downloadFolderPath, zipPath);
            ZipDeliveryInfo zipInfo = new ZipDeliveryInfo(correlationId,
                                                          zipName,
                                                          zipPath.toFile().length(),
                                                          computeChecksum(zipPath),
                                                          zipPath.toUri().toString());
            LOGGER.debug("Successfully created local delivery zip '{}.'", zipInfo);
            return zipInfo;
        } catch (IOException e) {
            throw new DeliveryOrderException(String.format("Unable to create zip at '%s'.", zipPath), e);
        }
    }

    /**
     * Get the zip name according to the number of files present in the download folder.
     */
    private String getZipName(String correlationId, Path downloadPath) {
        String zipName;
        File[] downloadFolder = downloadPath.toFile().listFiles();
        assert downloadFolder != null;
        if (downloadFolder.length == 1) {
            zipName = removeExtension(getName(downloadFolder[0].getName())) + ".zip";
        } else {
            zipName = String.format(MULTIPLE_FILES_ZIP_NAME_PATTERN, correlationId);
        }
        return zipName;
    }

    /**
     * Compute the md5 checksum of the zip created.
     */
    private String computeChecksum(Path zipFolderPath) throws DeliveryOrderException {
        try {
            return ChecksumUtils.computeHexChecksum(zipFolderPath, "MD5");
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new DeliveryOrderException(String.format("Could not compute MD5 md5Checksum from zip located at '%s'",
                                                           zipFolderPath), e);
        }
    }
}
