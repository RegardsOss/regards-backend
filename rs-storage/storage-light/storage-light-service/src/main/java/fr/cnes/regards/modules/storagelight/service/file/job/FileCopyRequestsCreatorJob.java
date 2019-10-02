/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.service.file.job;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileCopyRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.flow.CopyFlowItem;
import fr.cnes.regards.modules.storagelight.service.file.FileReferenceService;

/**
 * JOB to handle copy requests on many {@link FileReference}s.<br>
 * This jobs requests database to retrieve {@link FileReference}s with search criterion and for each, send a {@link CopyFlowItem} events.<br>
 * Events can be then handled by the first available storage microservice to create associated {@link FileCopyRequest}.<br>
 * NOTE : Be careful that the {@link this#run()} stays not transactional.
 *
 * @author Sébastien Binda
 */
public class FileCopyRequestsCreatorJob extends AbstractJob<Void> {

    public static final String STORAGE_LOCATION_SOURCE_ID = "source";

    public static final String STORAGE_LOCATION_DESTINATION_ID = "dest";

    public static final String SOURCE_PATH = "sourcePath";

    public static final String DESTINATION_PATH = "destinationPath";

    private static final int PAGE_BULK_SIZE = 500;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private FileReferenceService fileRefService;

    private String storageLocationSourceId;

    private String storageLocationDestinationId;

    private String sourcePath;

    private String destinationPath;

    private int totalPages = 0;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        storageLocationSourceId = parameters.get(STORAGE_LOCATION_SOURCE_ID).getValue();
        storageLocationDestinationId = parameters.get(STORAGE_LOCATION_DESTINATION_ID).getValue();
        sourcePath = parameters.get(SOURCE_PATH).getValue();
        destinationPath = parameters.get(DESTINATION_PATH).getValue();
    }

    @Override
    public void run() {

        Pageable pageRequest = PageRequest.of(0, PAGE_BULK_SIZE);
        Page<FileReference> pageResults;
        do {
            // Search for all file references matching the given storage location.
            pageResults = fileRefService.search(storageLocationSourceId, pageRequest);
            totalPages = pageResults.getTotalPages();
            for (FileReference fileRef : pageResults.getContent()) {
                try {
                    Optional<Path> desinationFilePath = getDestinationFilePath(fileRef.getLocation().getUrl(),
                                                                               sourcePath, destinationPath);
                    if (desinationFilePath.isPresent()) {
                        // For each file reference located in the given path, send a copy request to the destination storage location.
                        publisher.publish(CopyFlowItem.build(FileCopyRequestDTO
                                .build(fileRef.getMetaInfo().getChecksum(), storageLocationDestinationId,
                                       desinationFilePath.get().toString()), UUID.randomUUID().toString()));
                    }
                } catch (MalformedURLException | ModuleException e) {
                    LOGGER.error("Unable to handle file reference {} for copy from {} to {}. Cause {}",
                                 fileRef.getLocation().getUrl(), storageLocationSourceId, storageLocationDestinationId);
                }
                this.advanceCompletion();
            }
        } while (pageResults.hasNext());
    }

    @Override
    public int getCompletionCount() {
        return totalPages > 0 ? totalPages : super.getCompletionCount();
    }

    /**
     * Check if the given file is in the path to copy. If it is, calculate the relative destination path.
     * @param fileUrl
     * @param sourcePathToCopy
     * @param destinationPath
     * @return
     * @throws MalformedURLException
     * @throws ModuleException
     */
    public static Optional<Path> getDestinationFilePath(String fileUrl, String sourcePathToCopy, String destinationPath)
            throws MalformedURLException, ModuleException {
        String destinationFilePath = "";
        if (destinationPath == null) {
            destinationFilePath = "";
        } else if (destinationPath.startsWith("/")) {
            // Make sure destination path is relative
            destinationFilePath = destinationPath.substring(1, destinationPath.length());
        } else {
            destinationFilePath = destinationPath;
        }
        URL url = new URL(fileUrl);
        Path fileDirectoryPath = Paths.get(url.getPath()).getParent();
        String fileDir = fileDirectoryPath.toString();
        if (fileDir.startsWith(sourcePathToCopy)) {
            Path destinationSubDirPath = Paths.get(sourcePathToCopy).relativize(fileDirectoryPath);
            destinationFilePath = Paths.get(destinationPath, destinationSubDirPath.toString()).toString();
            if (destinationFilePath.length() > FileLocation.URL_MAX_LENGTH) {
                throw new ModuleException(String
                        .format("Destination path <%s> legnth is too long (> %d). fileUrl=%s,sourcePathToCopy=%s,destinationPath=%s",
                                destinationFilePath.toString(), FileLocation.URL_MAX_LENGTH, fileUrl, sourcePathToCopy,
                                destinationPath));
            }
            return Optional.of(Paths.get(destinationFilePath));
        } else {
            return Optional.empty();
        }
    }

}
