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
package fr.cnes.regards.modules.delivery.service.order.zip.steps;

import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.framework.utils.file.CompressToZipUtils;
import fr.cnes.regards.modules.delivery.domain.exception.DeliveryOrderException;
import fr.cnes.regards.modules.delivery.domain.order.zip.ZipDeliveryInfo;
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
    public static final String MULTIPLE_FILES_ZIP_NAME_PATTERN = "delivery-%s.zip"; // delivery-<correlationId>.zip

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
        ZipNameAndSource zip = getZipNameAndSource(correlationId, downloadFolderPath);
        Path zipPath = workspaceFolderPath.resolve(zip.name());
        try {
            CompressToZipUtils.compressDirectoriesToZip(zip.sourceDirectory(), zipPath);
            ZipDeliveryInfo zipInfo = new ZipDeliveryInfo(correlationId,
                                                          zip.name(),
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
     * Get the zip name and source folder (parent folder of files to zip) according to the number of files present in
     * the download folder.
     * <p>
     * DeliveryDownService creates one sub-folder per product. So if there is only one sub-folder, the zip should be
     * named after this sub-folder and directly contain its files. If there are multiple sub-folders (meaning
     * multiple products), the sub-folders should be preserved in the zip, and the zip should be named according to
     * the delivery correlation id.
     */
    private ZipNameAndSource getZipNameAndSource(String correlationId, Path downloadPath) {
        File[] downloadFolder = downloadPath.toFile().listFiles();
        assert downloadFolder != null;
        if (downloadFolder.length == 1) {
            return new ZipNameAndSource(removeExtension(getName(downloadFolder[0].getName())) + ".zip",
                                        downloadFolder[0].toPath());
        } else {
            return new ZipNameAndSource(String.format(MULTIPLE_FILES_ZIP_NAME_PATTERN, correlationId), downloadPath);
        }
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

    /**
     * Information about a zip to create: its name, and the source folder containing the files/directories to zip.
     */
    private record ZipNameAndSource(String name,
                                    Path sourceDirectory) {

    }
}
