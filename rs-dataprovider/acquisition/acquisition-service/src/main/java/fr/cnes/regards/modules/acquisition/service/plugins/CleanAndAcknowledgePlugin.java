/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.ScanDirectoryInfo;
import fr.cnes.regards.modules.acquisition.plugins.IChainBlockingPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ISipPostProcessingPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This post processing plugin allows to optionally :
 * <ul>
 * <li>create acknowledgement for each product file</li>
 * <li>clean all original product files</li>
 * </ul>
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 */
@Plugin(id = "CleanAndAcknowledgePlugin",
        version = "1.0.0-SNAPSHOT",
        markdown = "CleanAndAcknowledgePlugin.md",
        description = "Optionally clean and/or create an acknowledgement for each product file",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class CleanAndAcknowledgePlugin implements ISipPostProcessingPlugin, IChainBlockingPlugin {

    public static final String CLEAN_FILE_PARAM = "cleanFile";

    public static final String CREATE_ACK_PARAM = "createAck";

    public static final String FOLDER_ACK_PARAM = "folderAck";

    public static final String EXTENSION_ACK_PARAM = "extensionAck";

    public static final String RECURSIVE_CHECK_PARAM = "recursiveCheck";

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanAndAcknowledgePlugin.class);

    private static final String STORE_ACK_IN_ROOT_DIRECTORY = "createAckIntoRootDirectory";

    @PluginParameter(name = CLEAN_FILE_PARAM,
                     label = "Enable product files removal",
                     defaultValue = "false",
                     optional = true)
    public Boolean cleanFile;

    @PluginParameter(name = CREATE_ACK_PARAM,
                     label = "An acknowledgement of successful completion of SIP saved by the ingest microservice",
                     defaultValue = "false",
                     optional = true)
    public Boolean createAck;

    @PluginParameter(name = FOLDER_ACK_PARAM,
                     label = "The sub folder where the acknowledgement is created",
                     defaultValue = "ack_regards",
                     optional = true)
    public String folderAck;

    @PluginParameter(name = EXTENSION_ACK_PARAM,
                     label = "The extension added to the data file to create the acknowledgement file",
                     defaultValue = ".regards",
                     optional = true)
    public String extensionAck;

    @PluginParameter(name = RECURSIVE_CHECK_PARAM,
                     label = "Enable recursive permission check of scan folders",
                     defaultValue = "false",
                     optional = true)
    public Boolean recursiveCheck;

    @PluginParameter(name = STORE_ACK_IN_ROOT_DIRECTORY,
                     label = "Store ack file in root directory instead of in the same directory than the scanned file.",
                     defaultValue = "false",
                     optional = true)
    public Boolean storeAckIntoRootDirectory;

    private final String className = this.getClass().getSimpleName();

    @Autowired
    private INotificationClient notificationClient;

    @Override
    public void postProcess(Product product) {
        Stream<AcquisitionFile> acquisitionFileStream = product.getAcquisitionFiles()
                                                               .stream()
                                                               .filter(p -> p.getState()
                                                                            == AcquisitionFileState.ACQUIRED);
        // Manage acknowledgement
        if (Boolean.TRUE.equals(createAck)) {
            int nbAckNotCreated = acquisitionFileStream.map(this::createAck).reduce(0, Integer::sum);
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
        if (Boolean.TRUE.equals(cleanFile)) {
            int nbDeletionIssues = acquisitionFileStream.map(acqFile -> {
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
     *
     * @param acqFile the current {@link AcquisitionFile}
     * @return number of ack that could not be created
     */
    private int createAck(AcquisitionFile acqFile) {
        Path ackFilePath = computeAckFilePath(acqFile);
        int errorCount = 1;
        if (createAckFileWithRetries(ackFilePath)) {
            try {
                // Update last modified date of the ack file in case of the ack already exists.
                Files.setLastModifiedTime(ackFilePath, FileTime.from(OffsetDateTime.now().toInstant()));
                errorCount = 0;
            } catch (IOException e) {
                LOGGER.error("Error updating last modification date of newly created/updated ack file", e);
            }
        }
        return errorCount;
    }

    /**
     * Creates the given Ack file by creating parent directory if missing and retrying given number of time in case
     * of IO error.
     *
     * @return <ul>
     * <li>false if Exception during ack creation, or ackFilePath null. </li>
     * <li>true if ack correctly created (or if ack already exists) </li>
     * </ul>
     */
    private boolean createAckFileWithRetries(@Nullable Path ackFilePath) {
        if (ackFilePath == null) {
            return false;
        }
        boolean success = false;
        int maxLoop = 30;
        int loopCount = 0;
        do {
            try {
                if (loopCount > 0) {
                    // Hack to handle possible nfs write error of file on disk, retry ack write with time delay
                    // between each try.
                    Thread.sleep(loopCount * 500L);
                }
                // Create acknowledgement directory (if necessary)
                if (!Files.exists(ackFilePath.getParent())) {
                    Files.createDirectories(ackFilePath.getParent());
                }
                // Create acknowledgement
                Files.createFile(ackFilePath);
                success = true;
            } catch (FileAlreadyExistsException e) {
                LOGGER.warn(e.getMessage(), e);
            } catch (InterruptedException e) {
                loopCount = maxLoop;
            } catch (IOException e) {
                String msg = String.format("%sCannot create acknowledgement for  file \"%s\" because %s",
                                           loopCount > 0 ? "[Retry] " : "",
                                           ackFilePath,
                                           e.getClass().getSimpleName());
                if (loopCount == maxLoop) {
                    LOGGER.warn(msg, e);
                } else {
                    LOGGER.debug(msg);
                }
            }
            loopCount++;
        } while (!success && loopCount <= maxLoop);
        return success;
    }

    @Override
    public List<String> getExecutionBlockers(AcquisitionProcessingChain chain) {
        List<String> executionBlockers = new ArrayList<>();
        chain.getFileInfos()
             .forEach(acquisitionFileInfo -> acquisitionFileInfo.getScanDirInfo()
                                                                .forEach(scanDirectoryInfo -> executionBlockers.addAll(
                                                                    getExecutionBlockers(scanDirectoryInfo.getScannedDirectory()))));
        return executionBlockers;
    }

    /**
     * @return generated ackFilePath depending on plugin options and file location :
     * <ul>
     *     <li>option storeAckIntoRootDirectory true: ackPath will be the scan root directory + folder ack location</li>
     *     <li>option storeAckIntoRootDirectory false: ackPath will be the current file location + folder ack
     *     location</li>
     * </ul>
     */
    private Path computeAckFilePath(AcquisitionFile acqFile) {
        Path ackFolderPath = null;
        Path ackFilePath = null;
        if (storeAckIntoRootDirectory) {
            // find scan dir concerned by the current acqFile
            Optional<Path> scanDir = acqFile.getFileInfo()
                                            .getScanDirInfo()
                                            .stream()
                                            .map(ScanDirectoryInfo::getScannedDirectory)
                                            .filter(path -> acqFile.getFilePath().startsWith(path))
                                            .findFirst();
            if (scanDir.isPresent()) {
                ackFolderPath = scanDir.get().resolve(folderAck);
            } else {
                LOGGER.warn("Cannot retrieve scan folder of acqFile {}, at location {} ",
                            acqFile.getId(),
                            acqFile.getFilePath());
            }
        } else {
            ackFolderPath = acqFile.getFilePath().getParent().resolve(folderAck);
        }
        if (ackFolderPath != null) {
            ackFilePath = ackFolderPath.resolve(acqFile.getFilePath().getFileName() + extensionAck);
        }
        return ackFilePath;
    }

    private List<String> getExecutionBlockers(Path scanDirectory) {
        List<String> executionBlockers = new ArrayList<>();
        if (!Files.exists(scanDirectory)) {
            executionBlockers.add(String.format("%s - Scan directory not found : %s", className, scanDirectory));
        } else {
            if (Boolean.TRUE.equals(recursiveCheck)) {
                checkDirectoryTree(scanDirectory, executionBlockers);
            } else {
                checkDirectory(scanDirectory, executionBlockers);
            }
        }
        return executionBlockers;
    }

    private void checkDirectoryTree(Path directory, List<String> executionBlockers) {
        checkDirectory(directory, executionBlockers);
        File[] subDirectories = directory.toFile().listFiles(File::isDirectory);
        if (subDirectories != null) {
            Arrays.stream(subDirectories)
                  .filter(file -> !file.getName().equals(folderAck))
                  .forEach(file -> checkDirectoryTree(file.toPath(), executionBlockers));
        }
    }

    private void checkDirectory(Path directory, List<String> executionBlockers) {
        if (Boolean.TRUE.equals(cleanFile) && !Files.isWritable(directory)) {
            executionBlockers.add(String.format("%s - Unable to remove product files in directory in : %s",
                                                className,
                                                directory));
        }
        if (Boolean.TRUE.equals(createAck)) {
            Path ackDirectory = directory.resolve(folderAck);
            if (!Files.exists(ackDirectory)) {
                if (!Files.isWritable(directory)) {
                    executionBlockers.add(String.format("%s - Unable to create ack directory in : %s",
                                                        className,
                                                        directory));
                }
            } else if (!Files.isWritable(ackDirectory)) {
                executionBlockers.add(String.format("%s - Unable to write to ack directory : %s",
                                                    className,
                                                    ackDirectory));
            }
        }
    }

}
