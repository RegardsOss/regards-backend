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

package fr.cnes.regards.modules.acquisition.service.plugins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.plugins.ISipPostProcessingPlugin;

/**
 * This post processing plugin allows to optionally :
 * <ul>
 * <li>create acknowledgement for each product file</li>
 * <li>clean all original product files</li>
 * </ul>
 * @author Christophe Mertz
 * @author Marc Sordi
 */
@Plugin(id = "CleanAndAcknowledgePlugin", version = "1.0.0-SNAPSHOT",
        description = "Optionally clean and/or create an acknowledgement for each product file",
        author = "REGARDS Team", contact = "regards@c-s.fr", license = "GPLv3", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class CleanAndAcknowledgePlugin implements ISipPostProcessingPlugin {

    public static final String CLEAN_FILE_PARAM = "cleanFile";

    public static final String CREATE_ACK_PARAM = "createAck";

    public static final String FOLDER_ACK_PARAM = "folderAck";

    public static final String EXTENSION_ACK_PARAM = "extensionAck";

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanAndAcknowledgePlugin.class);

    @PluginParameter(name = CLEAN_FILE_PARAM, label = "Enable product files removal", defaultValue = "false",
            optional = true)
    public Boolean cleanFile;

    @PluginParameter(name = CREATE_ACK_PARAM,
            label = "An acknowledgement of successful completion of SIP saved by the ingest microservice",
            defaultValue = "false", optional = true)
    public Boolean createAck;

    @PluginParameter(name = FOLDER_ACK_PARAM, label = "The sub folder where the acknowledgement is created",
            defaultValue = "ack_regards", optional = true)
    public String folderAck;

    @PluginParameter(name = EXTENSION_ACK_PARAM,
            label = "The extension added to the data file to create the acknowledgement file",
            defaultValue = ".regards", optional = true)
    public String extensionAck;

    @Autowired
    private INotificationClient notificationClient;

    @Override
    public void postProcess(Product product) throws ModuleException {

        // Manage acknowledgement
        if (createAck) {
            int nbAckNotCreated = product.getAcquisitionFiles().stream().map(acqFile -> createAck(acqFile))
                    .reduce(0, Integer::sum);
            if (nbAckNotCreated > 0) {
                notificationClient.notify(String.format("%d acknowledgement could not be created for product %s",
                                                        nbAckNotCreated,
                                                        product.getProductName()),
                                          "Issues creating acknowledgement",
                                          NotificationLevel.ERROR,
                                          DefaultRole.EXPLOIT);
            }
        }

        // Manage file cleaning
        if (cleanFile) {
            int nbDeletionIssues = product.getAcquisitionFiles().stream().map(acqFile -> {
                try {
                    Files.delete(acqFile.getFilePath());
                    return 0;
                } catch (IOException e) {
                    // Skipping silently
                    String msg = String.format("Deletion failure for product \"%s\" and  file \"%s\"",
                                               product.getProductName(),
                                               acqFile.getFilePath().toString());
                    LOGGER.warn(msg, e);
                    return 1;
                }
            }).reduce(0, Integer::sum);
            if (nbDeletionIssues > 0) {
                notificationClient.notify(String.format("%s acquisition file could not be cleaned up for product %s",
                                                        nbDeletionIssues,
                                                        product.getProductName()),
                                          "Issues cleaning up files",
                                          NotificationLevel.ERROR,
                                          DefaultRole.EXPLOIT);
            }
        }
    }

    /**
     * Create the acknowledgement for an {@link AcquisitionFile}
     * @param acqFile the current {@link AcquisitionFile}
     * @return number of ack that could not be created
     */
    private int createAck(AcquisitionFile acqFile) {

        try {
            // Create acknowledgement directory (if necessary)
            Path ackDirPath = acqFile.getFilePath().getParent().resolve(folderAck);
            Files.createDirectories(ackDirPath);

            // Create acknowledgement
            Path ackFilePath = ackDirPath.resolve(acqFile.getFilePath().getFileName() + extensionAck);
            Files.createFile(ackFilePath);
            Files.setLastModifiedTime(ackFilePath, FileTime.from(OffsetDateTime.now().toInstant()));
            return 0;
        } catch (IOException e) {
            // Skipping silently
            String msg = String.format("Cannot create acknowledgement for  file \"%s\" because %s",
                                       acqFile.getFilePath().toString(),
                                       e.getClass().getSimpleName());
            LOGGER.warn(msg, e);
            return 1;
        }
    }
}
