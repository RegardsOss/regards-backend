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
package fr.cnes.regards.modules.storage.service.file.request;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.storage.dao.IFileCacheRequestRepository;
import fr.cnes.regards.modules.storage.domain.database.CacheFile;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storage.domain.event.FileRequestType;
import fr.cnes.regards.modules.storage.domain.flow.AvailabilityFlowItem;
import fr.cnes.regards.modules.storage.domain.plugin.FileRestorationWorkingSubset;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineStorageLocation;
import fr.cnes.regards.modules.storage.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storage.domain.plugin.PreparationResponse;
import fr.cnes.regards.modules.storage.service.StorageJobsPriority;
import fr.cnes.regards.modules.storage.service.cache.CacheService;
import fr.cnes.regards.modules.storage.service.file.FileDownloadService;
import fr.cnes.regards.modules.storage.service.file.FileReferenceEventPublisher;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.job.FileCacheRequestJob;
import fr.cnes.regards.modules.storage.service.location.StorageLocationConfigurationService;
import fr.cnes.regards.modules.storage.service.location.StoragePluginConfigurationHandler;

/**
 * Service to handle {@link FileCacheRequest}s.
 * Those requests are created when a file reference need to be restored physically thanks to an existing {@link INearlineStorageLocation} plugin.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileCacheRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCacheRequestService.class);

    @Autowired
    private IFileCacheRequestRepository repository;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private StoragePluginConfigurationHandler storageHandler;

    @Autowired
    private EntityManager em;

    @Autowired
    protected FileCacheRequestService self;

    @Autowired
    private FileReferenceEventPublisher publisher;

    @Autowired
    private RequestsGroupService reqGrpService;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private StorageLocationConfigurationService pStorageService;

    @Autowired
    private FileDownloadService downloadService;

    @Autowired
    private RequestStatusService reqStatusService;

    @Autowired
    private INotificationClient notificationClient;

    @Value("${regards.storage.cache.requests.per.job:100}")
    private Integer nbRequestsPerJob;

    /**
     * Static variable to avoid sending notification of cache full event after each request.
     */
    private static boolean globalCacheLimitReached = false;

    /**
     * Search for a {@link FileCacheRequest} on the file given checksum.
     * @param checksum
     * @return {@link FileCacheRequest}
     */
    @Transactional(readOnly = true)

    public Optional<FileCacheRequest> search(String checksum) {
        return repository.findByChecksum(checksum);
    }

    /**
     * Creates a new {@link FileCacheRequest} if does not exists already.
     * @param fileRefToRestore
     * @param expirationDate
     * @param groupId Business identifier of the deletion request
     * @return {@link FileCacheRequest} created.
     */
    public Optional<FileCacheRequest> create(FileReference fileRefToRestore, OffsetDateTime expirationDate,
            String groupId) {
        String checksum = fileRefToRestore.getMetaInfo().getChecksum();
        Optional<FileCacheRequest> oFcr = repository.findByChecksum(checksum);
        FileCacheRequest request = null;
        if (!oFcr.isPresent()) {
            request = new FileCacheRequest(fileRefToRestore, cacheService.getCacheDirectoryPath(checksum),
                    expirationDate, groupId);
            request = repository.save(request);
            LOGGER.trace("File {} (checksum {}) is requested for cache.", fileRefToRestore.getMetaInfo().getFileName(),
                         fileRefToRestore.getMetaInfo().getChecksum());
        } else {
            request = oFcr.get();
            if (request.getStatus() == FileRequestStatus.ERROR) {
                request.setStatus(reqStatusService.getNewStatus(request));
                request = repository.save(request);
            }
            LOGGER.trace("File {} (checksum {}) is already requested for cache.",
                         fileRefToRestore.getMetaInfo().getFileName(), fileRefToRestore.getMetaInfo().getChecksum());
        }
        return Optional.ofNullable(request);
    }

    public void makeAvailable(Collection<AvailabilityFlowItem> items) {
        items.forEach(i -> {
            reqGrpService.granted(i.getGroupId(), FileRequestType.AVAILABILITY, i.getChecksums().size(),
                                  i.getExpirationDate());
            makeAvailable(i.getChecksums(), i.getExpirationDate(), i.getGroupId());
        });
    }

    /**
     * Ensure availability of given files by their checksum for download.
     * @param checksums
     * @param expirationDate availability expiration date.
     * @param groupId
     * @return Number of availability requests created.
     */
    public int makeAvailable(Collection<String> checksums, OffsetDateTime expirationDate, String groupId) {

        Set<FileReference> onlines = Sets.newHashSet();
        Set<FileReference> offlines = Sets.newHashSet();
        Set<FileReference> nearlines = Sets.newHashSet();
        Set<FileReference> refs = fileRefService.search(checksums);
        Set<String> unkownFiles = Sets.newHashSet();
        Set<String> remainingChecksums = Sets.newHashSet(checksums);

        // Dispatch by storage
        ImmutableListMultimap<String, FileReference> filesByStorage = Multimaps
                .index(refs, f -> f.getLocation().getStorage());
        Set<String> remainingStorages = Sets.newHashSet(filesByStorage.keySet());

        Optional<StorageLocationConfiguration> storage = pStorageService.searchActiveHigherPriority(remainingStorages);
        // Handle storage by priority
        while (storage.isPresent() && !remainingStorages.isEmpty() && !remainingChecksums.isEmpty()) {
            // For each storage dispatch files in online, near line and not available
            PluginConfiguration conf = storage.get().getPluginConfiguration();
            String storageName = conf.getLabel();
            ImmutableList<FileReference> storageFiles = filesByStorage.get(storageName);
            // Calculate remaining files to make available for the current storage
            Set<FileReference> files = storageFiles.stream()
                    .filter(f -> remainingChecksums.contains(f.getMetaInfo().getChecksum()))
                    .collect(Collectors.toSet());
            switch (storage.get().getStorageType()) {
                case NEARLINE:
                    nearlines.addAll(files);
                    break;
                case ONLINE:
                    // If storage is an online one, so file is already available
                    onlines.addAll(files);
                    break;
                default:
                    // Unknown storage type
                    offlines.addAll(files);
                    break;
            }
            // Remove handled checksum by the current storage
            remainingChecksums.removeAll(storageFiles.stream().map(f -> f.getMetaInfo().getChecksum())
                    .collect(Collectors.toSet()));
            // Remove handled storage
            remainingStorages.remove(storageName);
            // Retrieve the new highest storage priority with the remaining ones
            storage = pStorageService.searchActiveHigherPriority(remainingStorages);
        }
        if (!remainingChecksums.isEmpty()) {
            for (String cs : remainingChecksums) {
                Optional<FileReference> oFileRef = refs.stream().filter(r -> r.getMetaInfo().getChecksum().equals(cs))
                        .findFirst();
                if (oFileRef.isPresent()) {
                    offlines.add(oFileRef.get());
                } else {
                    unkownFiles.add(cs);
                }
            }
            // add unknown to offline files
            offlines.addAll(refs.stream().filter(ref -> remainingChecksums.contains(ref.getMetaInfo().getChecksum()))
                    .collect(Collectors.toSet()));
        }
        notifyUnknowns(unkownFiles, groupId);
        notifyAvailables(onlines, groupId);
        // notifyNotAvailables(offlines, groupId);
        // Handle off lines as near lines files to create new FileCacheRequests.
        nearlines.addAll(offlines);
        int nbRequests = makeAvailable(nearlines, expirationDate, groupId);
        return nbRequests;
    }

    /**
     * @param unkownFiles
     */
    private void notifyUnknowns(Set<String> unkownFiles, String requestGroupId) {
        for (String checksum : unkownFiles) {
            String message = String.format("file with checksum %s does not exists.", checksum);
            LOGGER.error("[AVAILABILITY ERROR {}] - {}", checksum, message);
            publisher.notAvailable(checksum, null, message, requestGroupId);
            reqGrpService.requestError(requestGroupId, FileRequestType.AVAILABILITY, checksum, null, null, null,
                                       message);
        }

    }

    /**
     * Update all {@link FileCacheRequest} in error status to change status to {@link FileRequestStatus#TO_DO}.
     * @param groupId request business identifier to retry
     */
    public void retryRequest(String groupId) {
        for (FileCacheRequest request : repository.findByGroupIdAndStatus(groupId, FileRequestStatus.ERROR)) {
            request.setStatus(reqStatusService.getNewStatus(request));
            request.setErrorCause(null);
            repository.save(request);
        }
    }

    /**
     * Schedule all {@link FileCacheRequest}s with given status to be handled in {@link JobInfo}s
     * @param status
     * @return scheduled {@link JobInfo}s
     */
    public Collection<JobInfo> scheduleJobs(FileRequestStatus status) {
        LOGGER.trace("[CACHE REQUESTS] Scheduling Cache jobs ...");
        long start = System.currentTimeMillis();
        Collection<JobInfo> jobList = Lists.newArrayList();
        Set<String> allStorages = repository.findStoragesByStatus(status);
        for (String storage : allStorages) {
            Page<FileCacheRequest> filesPage;
            Long maxId = 0L;
            // Always search the first page of requests until there is no requests anymore.
            // To do so, we order on id to ensure to not handle same requests multiple times.
            Pageable page = PageRequest.of(0, nbRequestsPerJob, Direction.ASC, "id");
            do {
                filesPage = repository.findAllByStorageAndStatusAndIdGreaterThan(storage, status, maxId, page);
                if (filesPage.hasContent()) {
                    maxId = filesPage.stream().max(Comparator.comparing(FileCacheRequest::getId)).get().getId();
                    jobList.addAll(self.scheduleJobsByStorage(storage, filesPage.getContent()));
                }
            } while (filesPage.hasContent());
        }
        if (!jobList.isEmpty()) {
            LOGGER.debug("[CACHE REQUESTS] {} jobs scheduled in {} ms", jobList.size(),
                         System.currentTimeMillis() - start);
        }
        return jobList;
    }

    /**
     * Schedule cache requests jobs for given storage using new transaction.
     * @param jobList
     * @param storage
     * @param requests
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Collection<JobInfo> scheduleJobsByStorage(String storage, List<FileCacheRequest> requests) {
        if (storageHandler.isConfigured(storage)) {
            requests = calculateRestorables(requests);
            Collection<JobInfo> jobInfoList = Sets.newHashSet();
            if ((requests != null) && !requests.isEmpty()) {
                try {
                    PluginConfiguration conf = pluginService.getPluginConfigurationByLabel(storage);
                    IStorageLocation storagePlugin = pluginService.getPlugin(conf.getBusinessId());
                    PreparationResponse<FileRestorationWorkingSubset, FileCacheRequest> response = storagePlugin
                            .prepareForRestoration(requests);
                    for (FileRestorationWorkingSubset ws : response.getWorkingSubsets()) {
                        jobInfoList.add(scheduleJob(ws, conf.getBusinessId()));
                    }
                    // Handle errors
                    for (Entry<FileCacheRequest, String> error : response.getPreparationErrors().entrySet()) {
                        this.handleStorageNotAvailable(error.getKey(), Optional.ofNullable(error.getValue()));
                    }
                } catch (ModuleException | NotAvailablePluginConfigurationException e) {
                    LOGGER.error(e.getMessage(), e);
                    this.handleStorageNotAvailable(requests);
                }
            }
            return jobInfoList;
        } else {
            handleStorageNotAvailable(requests);
            return Collections.emptyList();
        }
    }

    public void delete(FileCacheRequest request) {
        if (repository.existsById(request.getId())) {
            repository.deleteById(request.getId());
        } else {
            LOGGER.warn("Unable to delete file cache request {} cause it does not exists.", request.getId());
        }
    }

    /**
     * Handle a {@link FileCacheRequest} end with success.<ul>
     *  <li> Creates the new {@link CacheFile}</li>
     *  <li> Deletes the {@link FileCacheRequest} handled.
     *  </ul>
     * @param fileReq
     * @param cacheLocation
     * @param realFileSize
     */
    public void handleSuccess(FileCacheRequest fileReq, URL cacheLocation, Collection<String> owners, Long realFileSize,
            String successMessage) {
        LOGGER.debug("[AVAILABILITY SUCCESS {}] - {}", fileReq.getChecksum(), successMessage);
        Optional<FileCacheRequest> oRequest = repository.findById(fileReq.getId());
        if (oRequest.isPresent()) {
            // Create the cache file associated
            cacheService.addFile(oRequest.get().getChecksum(), realFileSize,
                                 oRequest.get().getFileReference().getMetaInfo().getFileName(),
                                 oRequest.get().getFileReference().getMetaInfo().getMimeType(),
                                 oRequest.get().getFileReference().getMetaInfo().getType(), cacheLocation,
                                 oRequest.get().getExpirationDate(), fileReq.getGroupId());
            delete(oRequest.get());
        }
        publisher.available(fileReq.getChecksum(), "cache", fileReq.getStorage(), cacheLocation, owners, successMessage,
                            fileReq.getGroupId());
        // Inform group that a request is done
        reqGrpService.requestSuccess(fileReq.getGroupId(), FileRequestType.AVAILABILITY, fileReq.getChecksum(), null,
                                     null, owners, null);
    }

    /**
     * Handle a {@link FileCacheRequest} end with error.
     * @param fileReq
     * @param cause
     */
    public void handleError(FileCacheRequest fileReq, String cause) {
        FileReference fileRef = fileReq.getFileReference();
        LOGGER.error("[AVAILABILITY ERROR {}] - Restoration error for file {} from {}. Cause : {}",
                     fileRef.getMetaInfo().getChecksum(), fileRef.getMetaInfo().getFileName(),
                     fileRef.getLocation().toString(), cause);
        Optional<FileCacheRequest> oRequest = repository.findById(fileReq.getId());
        if (oRequest.isPresent()) {
            FileCacheRequest request = oRequest.get();
            request.setStatus(FileRequestStatus.ERROR);
            request.setErrorCause(cause);
            repository.save(request);
        }
        publisher.notAvailable(fileReq.getChecksum(), fileReq.getStorage(), cause, fileReq.getGroupId());
        reqGrpService.requestError(fileReq.getGroupId(), FileRequestType.AVAILABILITY, fileReq.getChecksum(),
                                   fileReq.getStorage(), null, Lists.newArrayList(), cause);
    }

    /**
     * Return all the request that can be restored in cache to not reach the cache size limit.
     * @param requests
     * @return available {@link FileCacheRequest} requests for restoration in cache
     */
    private List<FileCacheRequest> calculateRestorables(Collection<FileCacheRequest> requests) {
        List<FileCacheRequest> restorables = Lists.newArrayList();
        // Calculate cache size available by adding cache file sizes sum and already queued requests
        Long availableCacheSize = cacheService.getFreeSpaceInBytes();
        Long occupation = 100 - ((availableCacheSize / cacheService.getCacheSizeLimit()) * 100);
        Long pendingSize = repository.getPendingFileSize();
        Long availableSize = availableCacheSize - pendingSize;
        Iterator<FileCacheRequest> it = requests.iterator();
        boolean cacheLimitReached = false;
        Long totalSize = 0L;
        while (it.hasNext()) {
            FileCacheRequest request = it.next();
            if ((totalSize + request.getFileSize()) <= availableSize) {
                restorables.add(request);
                totalSize += request.getFileSize();
            } else {
                cacheLimitReached = true;
            }
        }
        if (cacheLimitReached) {
            if (!globalCacheLimitReached) {
                String message = String
                        .format("One or many files to restore has been locked cause cache is full (%s%%)", occupation);
                notificationClient.notify(message, "Cache is full", NotificationLevel.WARNING, DefaultRole.ADMIN);
                globalCacheLimitReached = true;
            }
        } else {
            globalCacheLimitReached = false;
        }
        return restorables;
    }

    /**
     * Schedule a {@link JobInfo} for the given {@link  FileRestorationWorkingSubset}.<br/>
     * NOTE : A new transaction is created for each call at this method. It is mandatory to avoid having too long transactions.
     * @param workingSubset
     * @param plgBusinessId
     * @return {@link JobInfo} scheduled.
     */
    public JobInfo scheduleJob(FileRestorationWorkingSubset workingSubset, String plgBusinessId) {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(FileCacheRequestJob.DATA_STORAGE_CONF_BUSINESS_ID, plgBusinessId));
        parameters.add(new JobParameter(FileCacheRequestJob.WORKING_SUB_SET, workingSubset));
        JobInfo jobInfo = jobInfoService.createAsQueued(new JobInfo(false, StorageJobsPriority.FILE_CACHE_JOB,
                                                                    parameters, authResolver.getUser(), FileCacheRequestJob.class.getName()));
        workingSubset.getFileRestorationRequests().forEach(r -> repository
                .updateStatusAndJobId(FileRequestStatus.PENDING, jobInfo.getId().toString(), r.getId()));
        em.flush();
        em.clear();
        return jobInfo;
    }

    /**
     * Creates {@link FileCacheRequest} for each nearline {@link FileReference} to be available for download.
     * After copy in cache, files will be available until the given expiration date.
     * @param fileReferences
     * @param expirationDate
     * @param groupId
     * @return number of cache request created.
     */
    public int makeAvailable(Set<FileReference> fileReferences, OffsetDateTime expirationDate, String groupId) {
        // Check files already available in cache
        Set<FileReference> availables = cacheService.getFilesAvailableInCache(fileReferences, groupId);
        Set<FileReference> toRestore = fileReferences.stream().filter(f -> !availables.contains(f))
                .collect(Collectors.toSet());
        // Notify available
        notifyAlreadyAvailablesInCache(availables, groupId);
        // Create a restoration request for all to restore
        for (FileReference f : toRestore) {
            create(f, expirationDate, groupId);
        }
        return toRestore.size();
    }

    /**
     * Update a list of {@link FileCacheRequest}s when the storage origin cannot be handled.
     * A storage origin cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     * @param fileRefRequests
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStorageNotAvailable(Collection<FileCacheRequest> fileRefRequests) {
        fileRefRequests.forEach((r) -> this.handleStorageNotAvailable(r, Optional.empty()));
    }

    /**
     * Update a {@link FileCacheRequest} when the storage origin cannot be handled.
     * A storage origin cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     * @param request
     */
    private void handleStorageNotAvailable(FileCacheRequest request, Optional<String> errorCause) {
        // The storage destination is unknown, we can already set the request in error status
        String message = errorCause.orElse(String
                .format("File <%s> cannot be handle for restoration as origin storage <%s> is unknown or disabled.",
                        request.getFileReference().getMetaInfo().getFileName(), request.getStorage()));
        request.setStatus(FileRequestStatus.ERROR);
        request.setErrorCause(message);
        repository.save(request);
        LOGGER.error("[AVAILABILITY ERROR] File {} is not available. Cause : {}",
                     request.getFileReference().getMetaInfo().getChecksum(), request.getErrorCause());
        publisher.notAvailable(request.getChecksum(), request.getStorage(), request.getErrorCause(),
                               request.getGroupId());
        reqGrpService.requestError(request.getGroupId(), FileRequestType.AVAILABILITY, request.getChecksum(),
                                   request.getStorage(), null, request.getFileReference().getLazzyOwners(), message);
    }

    /**
     * Send {@link FileReferenceEvent} for available given files.
     * @param availables newly available files
     * @param availabilityGroupId business request identifier of the availability request associated.
     */
    private void notifyAvailables(Set<FileReference> availables, String availabilityGroupId) {
        for (FileReference fileRef : availables) {
            String checksum = fileRef.getMetaInfo().getChecksum();
            String storage = fileRef.getLocation().getStorage();
            String message = String.format("file %s (checksum %s) is available for download.",
                                           fileRef.getMetaInfo().getFileName(), checksum);
            LOGGER.debug("[AVAILABILITY SUCCESS {}] - {}", checksum, message);
            try {
                // For online files we have to generate access url though storage microservice
                String url = downloadService.generateDownloadUrl(checksum);
                publisher.available(checksum, storage, storage, new URL(url), fileRef.getLazzyOwners(), message,
                                    availabilityGroupId);
                reqGrpService.requestSuccess(availabilityGroupId, FileRequestType.AVAILABILITY, checksum, storage, null,
                                             fileRef.getLazzyOwners(), fileRef);
            } catch (ModuleException | MalformedURLException e) {
                LOGGER.error(e.getMessage(), e);
                publisher.notAvailable(checksum, storage, e.getMessage(), availabilityGroupId);
                reqGrpService.requestError(availabilityGroupId, FileRequestType.AVAILABILITY, checksum, storage, null,
                                           fileRef.getLazzyOwners(), e.getMessage());
            }
        }
    }

    /**
     * Notify all files as AVAILABLE.
     * @param availables
     * @param groupId
     */
    private void notifyAlreadyAvailablesInCache(Set<FileReference> availables, String groupId) {
        for (FileReference fileRef : availables) {
            String checksum = fileRef.getMetaInfo().getChecksum();
            String storage = fileRef.getLocation().getStorage();
            String message = String.format("File %s (checksum %s) is available for download.",
                                           fileRef.getMetaInfo().getFileName(), checksum);
            URL availableUrl;
            try {
                availableUrl = new URL("file", null, cacheService.getFilePath(checksum));
                publisher.available(checksum, "cache", storage, availableUrl, fileRef.getLazzyOwners(), message,
                                    groupId);
                reqGrpService.requestSuccess(groupId, FileRequestType.AVAILABILITY, checksum, storage, null,
                                             Lists.newArrayList(), fileRef);
            } catch (MalformedURLException e) {
                // Should not happen
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Delete all requests for the given storage identifier
     * @param storageLocationId
     */
    public void deleteByStorage(String storageLocationId, Optional<FileRequestStatus> status) {
        if (status.isPresent()) {
            repository.deleteByStorageAndStatus(storageLocationId, status.get());
        } else {
            repository.deleteByStorage(storageLocationId);
        }
    }

    /**
     * @param deletedFileRef
     */
    public void delete(FileReference deletedFileRef) {
        repository.deleteByfileReference(deletedFileRef);
    }
}
