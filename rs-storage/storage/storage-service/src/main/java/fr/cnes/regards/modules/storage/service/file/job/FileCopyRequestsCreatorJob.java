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
package fr.cnes.regards.modules.storage.service.file.job;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesCopyEvent;
import fr.cnes.regards.modules.filecatalog.dto.request.FileCopyRequestDto;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storage.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.request.FileCopyRequestService;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * JOB to handle copy requests on many {@link FileReference}s.<br>
 * This jobs requests database to retrieve {@link FileReference}s with search criterion and for each, send a {@link FilesCopyEvent} events.<br>
 * Events can be then handled by the first available storage microservice to create associated {@link FileCopyRequest}.<br>
 * NOTE : Be careful that the {@link #run()} stays not transactional.
 *
 * @author Sébastien Binda
 */
public class FileCopyRequestsCreatorJob extends AbstractJob<Void> {

    public static final String STORAGE_LOCATION_SOURCE_ID_PARMETER_NAME = "source";

    public static final String STORAGE_LOCATION_DESTINATION_ID_PARMETER_NAME = "dest";

    public static final String SOURCE_PATH_PARMETER_NAME = "sourcePath";

    public static final String DESTINATION_PATH_PARMETER_NAME = "destinationPath";

    public static final String FILE_TYPES_PARMETER_NAME = "types";

    public static final String SESSION_OWNER_PARMETER_NAME = "sessionOwner";

    public static final String SESSION_PARMETER_NAME = "session";

    @Autowired
    private IPublisher publisher;

    @Autowired
    private INotificationClient notifClient;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private ILockingTaskExecutors lockingTaskExecutors;

    private String storageLocationSourceId;

    private String storageLocationDestinationId;

    private String sourcePath;

    private String destinationPath;

    private Set<String> types = Sets.newHashSet();

    private int totalPages = 0;

    private String sessionOwner;

    private String session;

    private IStorageLocation sourcePlugin;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        storageLocationSourceId = parameters.get(STORAGE_LOCATION_SOURCE_ID_PARMETER_NAME).getValue();
        storageLocationDestinationId = parameters.get(STORAGE_LOCATION_DESTINATION_ID_PARMETER_NAME).getValue();
        sourcePath = parameters.get(SOURCE_PATH_PARMETER_NAME).getValue();
        destinationPath = parameters.get(DESTINATION_PATH_PARMETER_NAME).getValue();
        sessionOwner = parameters.get(SESSION_OWNER_PARMETER_NAME).getValue();
        session = parameters.get(SESSION_PARMETER_NAME).getValue();
        if (parameters.get(FILE_TYPES_PARMETER_NAME) != null) {
            types = parameters.get(FILE_TYPES_PARMETER_NAME).getValue();
        }
        try {
            sourcePlugin = pluginService.getPlugin(storageLocationSourceId);
        } catch (ModuleException | NotAvailablePluginConfigurationException e) {
            throw new JobParameterInvalidException(String.format(
                "Invalid source location plugin %s. Associated storage location plugin not available",
                storageLocationSourceId), e);
        }
    }

    /**
     * Publish {@link FilesCopyEvent}s for each {@link FileReference} to copy from one destination to an other one.
     */
    private final Task publishCopyFlowItemsTask = () -> {
        lockingTaskExecutors.assertLocked();
        long start = System.currentTimeMillis();
        logger.info("[COPY JOB] Calculate all files to copy from storage location {} to {} ...",
                    storageLocationSourceId,
                    storageLocationDestinationId);
        Pageable pageRequest = PageRequest.of(0, FilesCopyEvent.MAX_REQUEST_PER_GROUP);
        Page<FileReference> pageResults;
        Optional<Path> sourceRootPath = sourcePlugin.getRootPath();
        logger.info("[COPY JOB] Origin source location {}", sourceRootPath.orElse(Paths.get("/")));
        long nbFilesToCopy = 0L;
        do {
            // Search for all file references matching the given storage location.
            if (types.isEmpty()) {
                pageResults = fileRefService.search(storageLocationSourceId, pageRequest);
            } else {
                pageResults = fileRefService.search(storageLocationSourceId, types, pageRequest);
            }
            totalPages = pageResults.getTotalPages();
            String groupId = UUID.randomUUID().toString();
            Set<FileCopyRequestDto> requests = Sets.newHashSet();
            for (FileReference fileRef : pageResults.getContent()) {
                try {
                    Optional<Path> desinationFilePath = getDestinationFilePath(fileRef.getLocation().getUrl(),
                                                                               sourceRootPath,
                                                                               sourcePath,
                                                                               destinationPath);
                    if (desinationFilePath.isPresent()) {
                        nbFilesToCopy++;
                        // For each file reference located in the given path, send a copy request to the destination storage location.
                        requests.add(FileCopyRequestDto.build(fileRef.getMetaInfo().getChecksum(),
                                                              storageLocationDestinationId,
                                                              desinationFilePath.get().toString(),
                                                              sessionOwner,
                                                              session));
                    }
                } catch (MalformedURLException | ModuleException e) {
                    logger.error(String.format("Unable to handle file reference %s for copy from %s to %s. Cause:",
                                               fileRef.getLocation().getUrl(),
                                               storageLocationSourceId,
                                               storageLocationDestinationId), e);
                }
                this.advanceCompletion();
            }
            publisher.publish(new FilesCopyEvent(requests, groupId));
            pageRequest = pageRequest.next();
        } while (pageResults.hasNext());
        String message = String.format("Copy process found %s files to copy from %s:%s to %s:%s.",
                                       nbFilesToCopy,
                                       storageLocationSourceId,
                                       sourcePath,
                                       storageLocationDestinationId,
                                       destinationPath);
        if (nbFilesToCopy > 0) {
            message = message + " Copy of files is now running, to monitor copy process go to storage locations page.";
            notifClient.notify(message, "Copy files", NotificationLevel.INFO, DefaultRole.EXPLOIT);
        } else {
            notifClient.notify(message, "Copy files", NotificationLevel.WARNING, DefaultRole.EXPLOIT);
        }
        logger.info("[COPY JOB] {} All jobs scheduled in {}ms", message, System.currentTimeMillis() - start);
    };

    @Override
    public void run() {
        try {
            lockingTaskExecutors.executeWithLock(publishCopyFlowItemsTask,
                                                 new LockConfiguration(FileCopyRequestService.COPY_PROCESS_LOCK,
                                                                       Instant.now().plusSeconds(300)));
        } catch (Throwable e) {
            logger.error("[COPY JOB] Unable to get a lock for copy process. Copy job canceled");
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public int getCompletionCount() {
        return totalPages > 0 ? totalPages : super.getCompletionCount();
    }

    /**
     * Check if the given file is in the path to copy. If it is, calculate the relative destination path.
     */
    public static Optional<Path> getDestinationFilePath(String fileUrl,
                                                        Optional<Path> sourceRootPath,
                                                        String sourcePathToCopy,
                                                        String destinationPath)
        throws MalformedURLException, ModuleException {
        String destinationFilePath = "";
        if (destinationPath == null) {
            destinationFilePath = "";
        } else if (destinationPath.startsWith("/")) {
            // Make sure destination path is relative
            destinationFilePath = destinationPath.substring(1);
        } else {
            destinationFilePath = destinationPath;
        }
        URL url = new URL(fileUrl);
        Path fileDirectoryPath = Paths.get(url.getPath()).getParent();
        String fileDir = fileDirectoryPath.toString();
        Path resolvedSourcePathToCopy;
        // If source path to copy is absolute, copy from the exact given directory
        if (sourcePathToCopy.startsWith("/")) {
            resolvedSourcePathToCopy = Paths.get(sourcePathToCopy);
        } else if (sourcePathToCopy.isEmpty()) {
            // If source path to copy is empty, copy from the storage location root path
            resolvedSourcePathToCopy = sourceRootPath.orElse(Paths.get("/"));
        } else {
            // If source path to copy is relative, copy from the storage location root path resoved with the source path to copy given
            resolvedSourcePathToCopy = sourceRootPath.orElse(Paths.get("/")).resolve(sourcePathToCopy);
        }

        if (fileDir.startsWith(resolvedSourcePathToCopy.toString())) {
            Path destinationSubDirPath = resolvedSourcePathToCopy.relativize(fileDirectoryPath);
            destinationFilePath = Paths.get(destinationPath, destinationSubDirPath.toString()).toString();
            if (destinationFilePath.length() > FileLocation.URL_MAX_LENGTH) {
                throw new ModuleException(String.format(
                    "Destination path <%s> legnth is too long (> %d). fileUrl=%s,sourcePathToCopy=%s,destinationPath=%s",
                    destinationFilePath,
                    FileLocation.URL_MAX_LENGTH,
                    fileUrl,
                    resolvedSourcePathToCopy,
                    destinationPath));
            }
            return Optional.of(Paths.get(destinationFilePath));
        } else {
            return Optional.empty();
        }
    }

}
