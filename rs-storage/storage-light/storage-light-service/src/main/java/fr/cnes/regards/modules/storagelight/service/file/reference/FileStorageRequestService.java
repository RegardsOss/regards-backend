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
package fr.cnes.regards.modules.storagelight.service.file.reference;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.storagelight.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.plugin.FileStorageWorkingSubset;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storagelight.service.JobsPriority;
import fr.cnes.regards.modules.storagelight.service.file.reference.flow.FileReferenceEventPublisher;
import fr.cnes.regards.modules.storagelight.service.file.reference.job.FileStorageRequestJob;
import fr.cnes.regards.modules.storagelight.service.storage.flow.StoragePluginConfigurationHandler;

/**
 * Service to handle {@link FileStorageRequest}s.
 * Those requests are created when a file reference need to be stored physically thanks to an existing {@link IStorageLocation} plugin.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileStorageRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageRequestService.class);

    private static final int NB_REFERENCE_BY_PAGE = 1000;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IFileStorageRequestRepository fileStorageRequestRepo;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private FileReferenceEventPublisher eventPublisher;

    @Autowired
    private StoragePluginConfigurationHandler storageHandler;

    @Autowired
    protected FileStorageRequestService self;

    @Autowired
    private EntityManager em;

    /**
     * Search for {@link FileStorageRequest}s matching the given destination storage and checksum
     * @param destinationStorage
     * @param checksum
     * @return {@link FileStorageRequest}
     */
    @Transactional(readOnly = true)
    public Optional<FileStorageRequest> search(String destinationStorage, String checksum) {
        return fileStorageRequestRepo.findByMetaInfoChecksumAndStorage(checksum, destinationStorage);
    }

    /**
     * Search for all {@link FileStorageRequest}s
     * @param pageable
     * @return {@link FileStorageRequest}s by page
     */
    @Transactional(readOnly = true)
    public Page<FileStorageRequest> search(Pageable pageable) {
        return fileStorageRequestRepo.findAll(pageable);
    }

    /**
     * Search for {@link FileStorageRequest}s associated to the given destination storage location.
     * @param pageable
     * @return {@link FileStorageRequest}s by page
     */
    @Transactional(readOnly = true)
    public Page<FileStorageRequest> search(String destinationStorage, Pageable pageable) {
        return fileStorageRequestRepo.findByStorage(destinationStorage, pageable);
    }

    /**
     * Delete a {@link FileStorageRequest}
     * @param fileStorageRequest to delete
     */
    public void delete(FileStorageRequest fileStorageRequest) {
        fileStorageRequestRepo.deleteById(fileStorageRequest.getId());
    }

    /**
     * Update all {@link FileStorageRequest} in error status to change status to todo.
     * @param requestId request business identifier to retry
     */
    public void retryRequest(String requestId) {
        for (FileStorageRequest request : fileStorageRequestRepo.findByRequestIdsAndStatus(requestId,
                                                                                           FileRequestStatus.ERROR)) {
            request.setStatus(FileRequestStatus.TODO);
            request.setErrorCause(null);
            update(request);
        }
    }

    /**
     * Update all {@link FileStorageRequest} in error status to change status to todo.
     * @param requestId
     */
    public void retry(Collection<String> owners) {
        Pageable page = PageRequest.of(0, NB_REFERENCE_BY_PAGE, Sort.by(Direction.ASC, "id"));
        Page<FileStorageRequest> results;
        do {
            results = fileStorageRequestRepo.findByOwnersInAndStatus(owners, FileRequestStatus.ERROR, page);
            for (FileStorageRequest request : results) {
                request.setStatus(FileRequestStatus.TODO);
                request.setErrorCause(null);
                update(request);
            }
            // Always retrieve the first page has we modify each element of the results.
            // All element are handled when result is empty.
        } while (results.hasNext());
    }

    /**
     * Update a {@link FileStorageRequest}
     * @param fileStorageRequest to delete
     */
    public void update(FileStorageRequest fileStorageRequest) {
        fileStorageRequestRepo.save(fileStorageRequest);
    }

    /**
     * Schedule {@link FileStorageRequestJob}s for all {@link FileStorageRequest}s matching the given parameters
     * @param status of the request to handle
     * @param storages of the request to handle
     * @param owners of the request to handle
     * @return {@link JobInfo}s scheduled
     */
    public Collection<JobInfo> scheduleJobs(FileRequestStatus status, Collection<String> storages,
            Collection<String> owners) {
        Collection<JobInfo> jobList = Lists.newArrayList();
        Set<String> allStorages = fileStorageRequestRepo.findStoragesByStatus(status);
        Set<String> storagesToSchedule = (storages != null) && !storages.isEmpty()
                ? allStorages.stream().filter(storages::contains).collect(Collectors.toSet())
                : allStorages;
        long start = System.currentTimeMillis();
        LOGGER.info("... scheduling storage jobs");
        for (String storage : storagesToSchedule) {
            Page<FileStorageRequest> filesPage;
            Pageable page = PageRequest.of(0, NB_REFERENCE_BY_PAGE, Sort.by("id"));
            do {
                if ((owners != null) && !owners.isEmpty()) {
                    filesPage = fileStorageRequestRepo.findAllByStorageAndOwnersIn(storage, owners, page);
                } else {
                    filesPage = fileStorageRequestRepo.findAllByStorage(storage, page);
                }
                List<FileStorageRequest> fileStorageRequests = filesPage.getContent();

                if (storageHandler.getConfiguredStorages().contains(storage)) {
                    jobList.addAll(scheduleJobsByStorage(storage, fileStorageRequests));
                } else {
                    handleStorageNotAvailable(fileStorageRequests);
                }
                page = filesPage.nextPageable();
            } while (filesPage.hasNext());
        }
        LOGGER.info("...{} jobs scheduled in {} ms", jobList.size(), System.currentTimeMillis() - start);
        return jobList;
    }

    /**
     * Schedule {@link FileStorageRequestJob}s for all given {@link FileStorageRequest}s and a given storage location.
     * @param storage
     * @param fileStorageRequests
     * @return {@link JobInfo}s scheduled
     */
    private Collection<JobInfo> scheduleJobsByStorage(String storage,
            Collection<FileStorageRequest> fileStorageRequests) {
        Collection<JobInfo> jobInfoList = Sets.newHashSet();
        try {
            PluginConfiguration conf = pluginService.getPluginConfigurationByLabel(storage);
            IStorageLocation storagePlugin = pluginService.getPlugin(conf.getBusinessId());
            Collection<FileStorageWorkingSubset> workingSubSets = storagePlugin.prepareForStorage(fileStorageRequests);
            for (FileStorageWorkingSubset ws : workingSubSets) {
                jobInfoList.add(self.scheduleJob(ws, conf.getBusinessId()));
            }
        } catch (ModuleException | NotAvailablePluginConfigurationException e) {
            this.handleStorageNotAvailable(fileStorageRequests);
        }
        return jobInfoList;
    }

    /**
     * Schedule a {@link JobInfo} for the given {@link  FileStorageWorkingSubset}.<br/>
     * NOTE : A new transaction is created for each call at this method. It is mandatory to avoid having too long transactions.
     * @param workingSubset
     * @param pluginConfId
     * @return {@link JobInfo} scheduled.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobInfo scheduleJob(FileStorageWorkingSubset workingSubset, String plgBusinessId) {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(FileStorageRequestJob.DATA_STORAGE_CONF_BUSINESS_ID, plgBusinessId));
        parameters.add(new JobParameter(FileStorageRequestJob.WORKING_SUB_SET, workingSubset));
        workingSubset.getFileReferenceRequests().forEach(fileStorageRequest -> fileStorageRequestRepo
                .updateStatus(FileRequestStatus.PENDING, fileStorageRequest.getId()));
        JobInfo jobInfo = jobInfoService.createAsQueued(new JobInfo(false, JobsPriority.FILE_STORAGE_JOB.getPriority(),
                parameters, authResolver.getUser(), FileStorageRequestJob.class.getName()));
        em.flush();
        em.clear();
        return jobInfo;
    }

    /**
     * Create a new {@link FileStorageRequest}
     * @param owners owners of the file to store
     * @param fileMetaInfo meta information of the file to store
     * @param originUrl file origin location
     * @param storage storage destination location
     * @param storageSubDirectory Optioanl subdirectory where to store file in the storage destination location
     * @param requestId Business identifier of the deletion request
     */
    public void create(String owner, FileReferenceMetaInfo fileMetaInfo, URL originUrl, String storage,
            Optional<String> storageSubDirectory, String requestId) {
        create(owner, fileMetaInfo, originUrl, storage, storageSubDirectory, FileRequestStatus.TODO, requestId);
    }

    public void create(String owner, FileReferenceMetaInfo fileMetaInfo, URL originUrl, String storage,
            Optional<String> storageSubDirectory, FileRequestStatus status, String requestId) {
        // Check if file storage request already exists
        Optional<FileStorageRequest> oFileRefRequest = search(storage, fileMetaInfo.getChecksum());
        if (oFileRefRequest.isPresent()) {
            handleAlreadyExists(oFileRefRequest.get(), fileMetaInfo, owner, requestId);
        } else {
            FileStorageRequest fileStorageRequest = new FileStorageRequest(Lists.newArrayList(owner), fileMetaInfo,
                    originUrl, storage, storageSubDirectory, requestId);
            fileStorageRequest.setStatus(status);
            if (!storageHandler.getConfiguredStorages().contains(storage)) {
                // The storage destination is unknown, we can already set the request in error status
                handleStorageNotAvailable(fileStorageRequest);
            } else {
                fileStorageRequestRepo.save(fileStorageRequest);
                LOGGER.debug("New file storage request created for file <{}> to store to {} with status {}",
                             fileStorageRequest.getMetaInfo().getFileName(), fileStorageRequest.getStorage(),
                             fileStorageRequest.getStatus());
            }
        }
    }

    /**
     * Method to update a {@link FileStorageRequest} when a new request is sent for the same associated {@link FileReference}.<br/>
     * If the existing file request is in error state, update the state to todo to allow store request retry.<br/>
     * The existing request is also updated to add new owners of the future stored and referenced {@link FileReference}.
     * @param fileStorageRequest
     * @param newMetaInfo
     * @param owners
     */
    private void handleAlreadyExists(FileStorageRequest fileStorageRequest, FileReferenceMetaInfo newMetaInfo,
            String owner, String newRequestId) {
        if (!fileStorageRequest.getOwners().contains(owner)) {
            fileStorageRequest.getOwners().add(owner);
            fileStorageRequest.getRequestIds().add(newRequestId);
            if (newMetaInfo.equals(fileStorageRequest.getMetaInfo())) {
                LOGGER.warn("Existing file meta information differs from new meta information. Previous ones are maintained");
            }
        }

        switch (fileStorageRequest.getStatus()) {
            case ERROR:
                // Allows storage retry.
                fileStorageRequest.setStatus(FileRequestStatus.TODO);
                break;
            case PENDING:
                // A storage is already in progress for this request.
            case DELAYED:
            case TODO:
            default:
                // Request has not been handled yet, we can update it.
                break;
        }
        fileStorageRequestRepo.save(fileStorageRequest);
    }

    /**
     * Update a list of {@link FileStorageRequest}s when the storage destination cannot be handled.
     * A storage destination cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     * @param fileStorageRequests
     */
    private void handleStorageNotAvailable(Collection<FileStorageRequest> fileStorageRequests) {
        fileStorageRequests.forEach(this::handleStorageNotAvailable);
    }

    /**
     * Update a {@link FileStorageRequest} when the storage destination cannot be handled.
     * A storage destination cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     * @param fileStorageRequest
     */
    private void handleStorageNotAvailable(FileStorageRequest fileStorageRequest) {
        // The storage destination is unknown, we can already set the request in error status
        fileStorageRequest.setStatus(FileRequestStatus.ERROR);
        fileStorageRequest.setErrorCause(String
                .format("File <%s> cannot be handle for storage as destination storage <%s> is unknown or disabled.",
                        fileStorageRequest.getMetaInfo().getFileName(), fileStorageRequest.getStorage()));
        update(fileStorageRequest);
        LOGGER.error(fileStorageRequest.getErrorCause());
        eventPublisher.storeError(fileStorageRequest.getMetaInfo().getChecksum(), fileStorageRequest.getOwners(),
                                  fileStorageRequest.getStorage(), fileStorageRequest.getErrorCause(),
                                  fileStorageRequest.getRequestIds());
    }

}
