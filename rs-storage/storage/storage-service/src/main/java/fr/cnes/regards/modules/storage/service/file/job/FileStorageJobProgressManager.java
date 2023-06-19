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
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestResultDTO;
import fr.cnes.regards.modules.storage.domain.plugin.IPeriodicActionProgressManager;
import fr.cnes.regards.modules.storage.domain.plugin.IStorageProgressManager;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.request.FileStorageRequestService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Progress manager class to handle {@link FileStorageRequestJob} advancement.<br>
 * This progress manager should be used by all storage plugin to inform a storage success or a storage error.<br>
 * This manager is used by storage plugins to inform {@link FileStorageRequestJob}s progression.
 *
 * @author Sébastien Binda
 */
public class FileStorageJobProgressManager extends PeriodicActionProgressManager
    implements IStorageProgressManager, IPeriodicActionProgressManager {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorageJobProgressManager.class);

    private final IJob<?> job;

    private final FileStorageRequestService storageRequestService;

    private final StorageLocationService storageLocationService;

    private final Set<FileStorageRequestResultDTO> handledRequests = Sets.newHashSet();

    private final Set<FileStorageRequestResultDTO> handledAndSavedRequests = Sets.newHashSet();

    public FileStorageJobProgressManager(FileStorageRequestService storageRequestService,
                                         FileReferenceService fileRefService,
                                         StorageLocationService storageLocationService,
                                         IJob<?> job) {
        super(fileRefService, storageLocationService);
        this.job = job;
        this.storageRequestService = storageRequestService;
        this.storageLocationService = storageLocationService;
    }

    @Override
    public void storageSucceed(FileStorageRequest fileReferenceRequest, URL storedUrl, Long fileSize) {
        storageSucceed(fileReferenceRequest, storedUrl, fileSize, false, false, true);
    }

    @Override
    public void storageSucceedWithPendingActionRemaining(FileStorageRequest fileReferenceRequest,
                                                         URL storedUrl,
                                                         Long fileSize,
                                                         Boolean notifyAdministrators) {
        storageSucceed(fileReferenceRequest, storedUrl, fileSize, true, notifyAdministrators, false);
    }

    private void storageSucceed(FileStorageRequest request,
                                URL storedUrl,
                                Long fileSize,
                                boolean pendingActionRemaining,
                                Boolean notifyAdministrators,
                                boolean handleByBulk) {
        // We do not save the new successfully stored file to avoid performance leak by committing files one by one in the database.
        // Files are saved in one transaction thanks to the bulkSave method.
        if (storedUrl == null) {
            storageFailed(request,
                          String.format(
                              "File {} has been successfully stored, nevertheless plugin <%> does not provide the new file location",
                              request.getStorage(),
                              request.getMetaInfo().getFileName()));
        } else {
            LOG.debug("[STORE SUCCESS] - File {} ({}octets) checksum={} stored on {} at {}.",
                      request.getMetaInfo().getFileName(),
                      fileSize,
                      request.getMetaInfo().getChecksum(),
                      request.getStorage(),
                      storedUrl);
            FileStorageRequestResultDTO requestDTO = FileStorageRequestResultDTO.build(request,
                                                                                       storedUrl.toString(),
                                                                                       fileSize,
                                                                                       pendingActionRemaining,
                                                                                       notifyAdministrators);
            // If bulk is requested, only add success to list of handle by bulk at the end.
            if (handleByBulk) {
                handledRequests.add(requestDTO);
            } else {
                // Else, handle success for given file.
                handledAndSavedRequests.add(requestDTO);
                storageRequestService.handleSuccess(Sets.newHashSet(requestDTO));
            }
            job.advanceCompletion();
        }
    }

    @Override
    public void storageFailed(FileStorageRequest request, String cause) {
        // We do not save the new error stored file to avoid performance leak by committing files one by one in the database.
        // Files are saved in one transaction thanks to the bulkSave method.
        LOG.error("[STORE ERROR {}] - Store error for file {} (id={})in {}. Cause : {}",
                  request.getMetaInfo().getChecksum(),
                  request.getMetaInfo().getFileName(),
                  request.getId(),
                  request.getStorage(),
                  cause);
        handledRequests.add(FileStorageRequestResultDTO.build(request, null, null, false, false).error(cause));
        job.advanceCompletion();
    }

    public void bulkSave() {
        long start = System.currentTimeMillis();
        Set<FileStorageRequestResultDTO> successes = handledRequests.stream()
                                                                    .filter(r -> !r.isError())
                                                                    .collect(Collectors.toSet());
        Set<FileStorageRequestResultDTO> errors = handledRequests.stream()
                                                                 .filter(r -> r.isError())
                                                                 .collect(Collectors.toSet());
        storageRequestService.handleSuccess(successes);
        storageRequestService.handleError(errors);
        // Job as also completed some remaining pending action on previous stored files. Update this files.
        super.bulkSavePendings();
        LOG.debug("[STORE END] Job requests final status updated ({} successes & {} errors) in {}ms.",
                  successes.size(),
                  errors.size(),
                  System.currentTimeMillis() - start);
    }

    /**
     * Does the given requests has been handled by the current job ?
     *
     * @param req {@link FileStorageRequest} to check for
     */
    public boolean isHandled(FileStorageRequest req) {
        return this.handledRequests.stream().filter(f -> f.getRequest().equals(req)).findFirst().isPresent()
               || this.handledAndSavedRequests.stream().filter(f -> f.getRequest().equals(req)).findFirst().isPresent();
    }
}
