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
package fr.cnes.regards.modules.storage.service.file.request;

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
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.fileaccess.plugin.domain.FileRestorationWorkingSubset;
import fr.cnes.regards.modules.fileaccess.plugin.domain.INearlineStorageLocation;
import fr.cnes.regards.modules.fileaccess.plugin.domain.IStorageLocation;
import fr.cnes.regards.modules.fileaccess.plugin.domain.PreparationResponse;
import fr.cnes.regards.modules.fileaccess.plugin.dto.FileCacheRequestDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesRestorationRequestEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.storage.dao.IFileCacheRequestRepository;
import fr.cnes.regards.modules.storage.domain.database.CacheFile;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.service.DownloadTokenService;
import fr.cnes.regards.modules.storage.service.StorageJobsPriority;
import fr.cnes.regards.modules.storage.service.cache.CacheService;
import fr.cnes.regards.modules.storage.service.file.FileReferenceEventPublisher;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.job.FileCacheRequestJob;
import fr.cnes.regards.modules.storage.service.location.StorageLocationConfigurationService;
import fr.cnes.regards.modules.storage.service.location.StoragePluginConfigurationHandler;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Service to handle {@link FileCacheRequest} entities.
 * Those requests are created when a file reference need to be restored physically thanks to an existing {@link INearlineStorageLocation} plugin.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class FileCacheRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCacheRequestService.class);

    /**
     * Static variable to avoid sending notification of cache full event after each request.
     */
    private static boolean globalCacheLimitReached = false;

    private final IFileCacheRequestRepository fileCacheRequestRepository;

    private final IPluginService pluginService;

    private final IJobInfoService jobInfoService;

    private final IAuthenticationResolver authResolver;

    private final CacheService cacheService;

    private final StoragePluginConfigurationHandler storageHandler;

    private final EntityManager em;

    private final FileReferenceEventPublisher publisher;

    private final RequestsGroupService reqGrpService;

    private final FileReferenceService fileRefService;

    private final StorageLocationConfigurationService storageLocationConfigurationService;

    private final DownloadTokenService downloadTokenService;

    private final RequestStatusService reqStatusService;

    private final INotificationClient notificationClient;

    private final ApplicationContext applicationContext;

    protected FileCacheRequestService self;

    @Value("${regards.storage.cache.requests.per.job:100}")
    private Integer nbRequestsPerJob;

    @Value("${regards.storage.group.requests.days.before.expiration:5}")
    private Integer nbDaysBeforeExpiration;

    public FileCacheRequestService(IFileCacheRequestRepository fileCacheRequestRepository,
                                   IPluginService pluginService,
                                   IJobInfoService jobInfoService,
                                   IAuthenticationResolver authResolver,
                                   CacheService cacheService,
                                   StoragePluginConfigurationHandler storageHandler,
                                   EntityManager em,
                                   FileReferenceEventPublisher publisher,
                                   RequestsGroupService reqGrpService,
                                   FileReferenceService fileRefService,
                                   StorageLocationConfigurationService storageLocationConfigurationService,
                                   DownloadTokenService downloadTokenService,
                                   RequestStatusService reqStatusService,
                                   INotificationClient notificationClient,
                                   ApplicationContext applicationContext,
                                   FileCacheRequestService fileCacheRequestService) {
        this.fileCacheRequestRepository = fileCacheRequestRepository;
        this.pluginService = pluginService;
        this.jobInfoService = jobInfoService;
        this.authResolver = authResolver;
        this.cacheService = cacheService;
        this.storageHandler = storageHandler;
        this.em = em;
        this.publisher = publisher;
        this.reqGrpService = reqGrpService;
        this.fileRefService = fileRefService;
        this.storageLocationConfigurationService = storageLocationConfigurationService;
        this.downloadTokenService = downloadTokenService;
        this.reqStatusService = reqStatusService;
        this.notificationClient = notificationClient;
        this.applicationContext = applicationContext;
        this.self = fileCacheRequestService;
    }

    /**
     * Search for a {@link FileCacheRequest} on the file given checksum.
     */
    @Transactional(readOnly = true)
    public Optional<FileCacheRequest> search(String checksum) {
        return fileCacheRequestRepository.findByChecksum(checksum);
    }

    /**
     * Creates a new {@link FileCacheRequest} if does not exist already; otherwise update it.
     *
     * @param fileRefToRestore  File that we are asking to be put into the cache
     * @param availabilityHours Duration at which the cache request expires
     * @param groupId           Business identifier of the deletion request
     * @return {@link FileCacheRequest} created.
     */
    public Optional<FileCacheRequest> create(FileReference fileRefToRestore, int availabilityHours, String groupId) {
        String checksum = fileRefToRestore.getMetaInfo().getChecksum();
        Optional<FileCacheRequest> fileCacheRequestOptional = fileCacheRequestRepository.findByChecksum(checksum);

        FileCacheRequest fileCacheRequest;
        if (fileCacheRequestOptional.isEmpty()) {
            fileCacheRequest = new FileCacheRequest(fileRefToRestore,
                                                    cacheService.getCacheDirectoryPath(checksum),
                                                    availabilityHours,
                                                    groupId);
            // Save in database
            fileCacheRequest = fileCacheRequestRepository.save(fileCacheRequest);
            LOGGER.trace("File {} (checksum {}) is requested for cache.",
                         fileRefToRestore.getMetaInfo().getFileName(),
                         fileRefToRestore.getMetaInfo().getChecksum());
        } else {
            fileCacheRequest = fileCacheRequestOptional.get();
            fileCacheRequest.setAvailabilityHours(availabilityHours);
            if (fileCacheRequest.getStatus() == FileRequestStatus.ERROR) {
                fileCacheRequest.setStatus(FileRequestStatus.TO_DO);
            }
            // Update in database
            fileCacheRequest = fileCacheRequestRepository.save(fileCacheRequest);
            LOGGER.trace("File {} (checksum {}) is already requested for cache.",
                         fileRefToRestore.getMetaInfo().getFileName(),
                         fileRefToRestore.getMetaInfo().getChecksum());
        }
        return Optional.of(fileCacheRequest);
    }

    public void makeAvailable(Collection<FilesRestorationRequestEvent> filesRestorationRequestEvents) {
        filesRestorationRequestEvents.forEach(availabilityRequestEvt -> {
            reqGrpService.granted(availabilityRequestEvt.getGroupId(),
                                  FileRequestType.AVAILABILITY,
                                  availabilityRequestEvt.getChecksums().size(),
                                  OffsetDateTime.now().plusDays(nbDaysBeforeExpiration));
            makeAvailable(availabilityRequestEvt.getChecksums(),
                          availabilityRequestEvt.getAvailabilityHours(),
                          availabilityRequestEvt.getGroupId());
        });
    }

    /**
     * Ensure availability of given files by their checksum for download.
     *
     * @param checksums         Checksums to be made available
     * @param availabilityHours Duration in hours of available files in the cache internal or external
     * @param groupId           Request group id
     */
    public void makeAvailable(Collection<String> checksums, int availabilityHours, String groupId) {

        Set<FileReference> onlines = Sets.newHashSet();
        Set<FileReference> offlines = Sets.newHashSet();
        Set<FileReference> nearlines = Sets.newHashSet();
        Set<FileReference> refs = fileRefService.search(checksums);
        Set<String> unkownFiles = Sets.newHashSet();
        Set<String> remainingChecksums = Sets.newHashSet(checksums);

        // Dispatch by storage
        ImmutableListMultimap<String, FileReference> filesByStorage = Multimaps.index(refs,
                                                                                      f -> f.getLocation()
                                                                                            .getStorage());
        Set<String> remainingStorages = Sets.newHashSet(filesByStorage.keySet());

        Optional<StorageLocationConfiguration> storage = storageLocationConfigurationService.searchActiveHigherPriority(
            remainingStorages);
        // Handle storage by priority
        while (storage.isPresent() && !remainingStorages.isEmpty() && !remainingChecksums.isEmpty()) {
            // For each storage dispatch files in online, near line and not available
            PluginConfiguration conf = storage.get().getPluginConfiguration();
            String storageName = conf.getLabel();
            ImmutableList<FileReference> storageFiles = filesByStorage.get(storageName);
            // Calculate remaining files to make available for the current storage
            Set<FileReference> files = storageFiles.stream()
                                                   .filter(f -> remainingChecksums.contains(f.getMetaInfo()
                                                                                             .getChecksum()))
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
            remainingChecksums.removeAll(storageFiles.stream()
                                                     .map(f -> f.getMetaInfo().getChecksum())
                                                     .collect(Collectors.toSet()));
            // Remove handled storage
            remainingStorages.remove(storageName);
            // Retrieve the new highest storage priority with the remaining ones
            storage = storageLocationConfigurationService.searchActiveHigherPriority(remainingStorages);
        }
        if (!remainingChecksums.isEmpty()) {
            for (String cs : remainingChecksums) {
                Optional<FileReference> oFileRef = refs.stream()
                                                       .filter(r -> r.getMetaInfo().getChecksum().equals(cs))
                                                       .findFirst();
                if (oFileRef.isPresent()) {
                    offlines.add(oFileRef.get());
                } else {
                    unkownFiles.add(cs);
                }
            }
            // add unknown to offline files
            offlines.addAll(refs.stream()
                                .filter(ref -> remainingChecksums.contains(ref.getMetaInfo().getChecksum()))
                                .collect(Collectors.toSet()));
        }
        notifyUnknowns(unkownFiles, groupId);
        notifyAvailables(onlines, groupId);
        // notifyNotAvailables(offlines, groupId); FIXME: @sbinda what should be done?
        // Handle off lines as near lines files to create new FileCacheRequests.
        nearlines.addAll(offlines);
        makeAvailable(nearlines, availabilityHours, groupId);
    }

    private void notifyUnknowns(Set<String> unkownFiles, String requestGroupId) {
        for (String checksum : unkownFiles) {
            String message = String.format("file with checksum %s does not exists.", checksum);
            LOGGER.error("[AVAILABILITY ERROR {}] - {}", checksum, message);
            publisher.notAvailable(checksum, null, message, requestGroupId);
            reqGrpService.requestError(requestGroupId,
                                       FileRequestType.AVAILABILITY,
                                       checksum,
                                       null,
                                       null,
                                       null,
                                       message);
        }

    }

    /**
     * Update all {@link FileCacheRequest} in error status to change status to {@link FileRequestStatus#TO_DO}.
     *
     * @param groupId request business identifier to retry
     */
    public void retryRequest(String groupId) {
        for (FileCacheRequest request : fileCacheRequestRepository.findByGroupIdAndStatus(groupId,
                                                                                          FileRequestStatus.ERROR)) {
            request.setStatus(FileRequestStatus.TO_DO);
            request.setErrorCause(null);
            fileCacheRequestRepository.save(request);
        }
    }

    /**
     * Schedule all {@link FileCacheRequest}s with given status to be handled in {@link JobInfo}s
     */
    public Collection<JobInfo> scheduleJobs(FileRequestStatus status) {
        LOGGER.trace("[CACHE REQUESTS] Scheduling Cache jobs ...");
        long start = System.currentTimeMillis();
        Collection<JobInfo> jobList = Lists.newArrayList();
        Set<String> allStorages = fileCacheRequestRepository.findStoragesByStatus(status);
        for (String storage : allStorages) {
            Page<FileCacheRequest> filesPage;
            Long maxId = 0L;
            // Always search the first page of requests until there is no requests anymore.
            // To do so, we order on id to ensure to not handle same requests multiple times.
            Pageable page = PageRequest.of(0, nbRequestsPerJob, Direction.ASC, "id");
            do {
                filesPage = fileCacheRequestRepository.findAllByStorageAndStatusAndIdGreaterThan(storage,
                                                                                                 status,
                                                                                                 maxId,
                                                                                                 page);
                if (filesPage.hasContent()) {
                    maxId = filesPage.stream()
                                     .max(Comparator.comparing(FileCacheRequest::getId))
                                     .orElseThrow(() -> new RsRuntimeException(
                                         "This should not happen as there is at least one file"))
                                     .getId();
                    jobList.addAll(self.scheduleJobsByStorage(storage, filesPage.getContent()));
                }
            } while (filesPage.hasContent());
        }
        if (!jobList.isEmpty()) {
            LOGGER.debug("[CACHE REQUESTS] {} jobs scheduled in {} ms",
                         jobList.size(),
                         System.currentTimeMillis() - start);
        }
        return jobList;
    }

    /**
     * Schedule cache requests jobs for given storage using new transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Collection<JobInfo> scheduleJobsByStorage(String storage, List<FileCacheRequest> requests) {
        if (storageHandler.isConfigured(storage)) {
            requests = calculateRestorables(requests);
            Collection<JobInfo> jobInfoList = Sets.newHashSet();
            if (!requests.isEmpty()) {
                try {
                    PluginConfiguration conf = pluginService.getPluginConfigurationByLabel(storage);
                    IStorageLocation storagePlugin = pluginService.getPlugin(conf.getBusinessId());
                    PreparationResponse<FileRestorationWorkingSubset, FileCacheRequestDto> response = storagePlugin.prepareForRestoration(
                        requests.stream().map(FileCacheRequest::toDto).toList());
                    for (FileRestorationWorkingSubset ws : response.getWorkingSubsets()) {
                        jobInfoList.add(scheduleJob(ws, conf.getBusinessId()));
                    }
                    // Handle errors
                    for (Entry<FileCacheRequestDto, String> error : response.getPreparationErrors().entrySet()) {
                        this.handleStorageNotAvailable(FileCacheRequest.fromDto(error.getKey()),
                                                       Optional.ofNullable(error.getValue()));
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

    /**
     * Delete the file cache request in database if the request exists.
     */
    public void delete(FileCacheRequest request) {
        if (fileCacheRequestRepository.existsById(request.getId())) {
            fileCacheRequestRepository.deleteById(request.getId());
        } else {
            LOGGER.warn("Unable to delete file cache request (id=[{}]) cause it does not exists.", request.getId());
        }
    }

    /**
     * Handle a {@link FileCacheRequest} end with success in internal cache.
     * <ul>
     * <li> Creates the new {@link CacheFile} for internal cache</li>
     * <li> Deletes the {@link FileCacheRequest} handled.
     * </ul>
     */
    public void handleSuccessInternalCache(FileCacheRequest fileCacheRequest,
                                           URL cacheLocation,
                                           Collection<String> owners,
                                           Long realFileSize,
                                           String successMessage) {
        handleSuccess(fileCacheRequest, cacheLocation, owners, realFileSize, null, null, successMessage);
    }

    /**
     * Handle a {@link FileCacheRequest} end with success in external cache.
     * <ul>
     * <li> Creates the new {@link CacheFile} for external cache</li>
     * <li> Deletes the {@link FileCacheRequest} handled.
     * </ul>
     */
    public void handleSuccessExternalCache(FileCacheRequest fileCacheRequest,
                                           URL cacheLocation,
                                           Collection<String> owners,
                                           Long realFileSize,
                                           String pluginBusinessId,
                                           OffsetDateTime expirationDate,
                                           String successMessage) {
        handleSuccess(fileCacheRequest,
                      cacheLocation,
                      owners,
                      realFileSize,
                      pluginBusinessId,
                      expirationDate,
                      successMessage);
    }

    private void handleSuccess(FileCacheRequest fileCacheRequest,
                               URL cacheLocation,
                               Collection<String> owners,
                               Long realFileSize,
                               @Nullable String pluginBusinessId,
                               @Nullable OffsetDateTime expirationDate,
                               String successMessage) {
        LOGGER.debug("[AVAILABILITY SUCCESS {}] - {}", fileCacheRequest.getChecksum(), successMessage);
        Optional<FileCacheRequest> fileCacheRequestOptional = fileCacheRequestRepository.findById(fileCacheRequest.getId());

        if (fileCacheRequestOptional.isPresent()) {
            if (expirationDate == null) {
                expirationDate = OffsetDateTime.now().plusHours(fileCacheRequestOptional.get().getAvailabilityHours());
            }
            // Create the internal or external cache file associated
            cacheService.addFile(fileCacheRequestOptional.get().getChecksum(),
                                 realFileSize,
                                 fileCacheRequestOptional.get().getFileReference().getMetaInfo().getFileName(),
                                 fileCacheRequestOptional.get().getFileReference().getMetaInfo().getMimeType(),
                                 fileCacheRequestOptional.get().getFileReference().getMetaInfo().getType(),
                                 cacheLocation,
                                 expirationDate,
                                 fileCacheRequest.getGroupId(),
                                 pluginBusinessId);

            delete(fileCacheRequestOptional.get());
        }

        if (expirationDate == null) {
            // If expirationDate == null then internal cache; otherwise external cache
            expirationDate = OffsetDateTime.now().plusHours(fileCacheRequest.getAvailabilityHours());
        }

        publisher.available(fileCacheRequest.getChecksum(),
                            "cache",
                            fileCacheRequest.getStorage(),
                            cacheLocation,
                            owners,
                            successMessage,
                            fileCacheRequest.getGroupId(),
                            expirationDate);
        // Inform group that a request is done
        reqGrpService.availibilityRequestSuccess(fileCacheRequest.getGroupId(), fileCacheRequest.getChecksum(), owners);
    }

    /**
     * Handle a {@link FileCacheRequest} end with error.
     */
    public void handleError(FileCacheRequest fileReq, String cause) {
        FileReference fileRef = fileReq.getFileReference();
        LOGGER.error("[AVAILABILITY ERROR {}] - Restoration error for file {} from {}. Cause : {}",
                     fileRef.getMetaInfo().getChecksum(),
                     fileRef.getMetaInfo().getFileName(),
                     fileRef.getLocation(),
                     cause);
        Optional<FileCacheRequest> oRequest = fileCacheRequestRepository.findById(fileReq.getId());
        if (oRequest.isPresent()) {
            FileCacheRequest request = oRequest.get();
            request.setStatus(FileRequestStatus.ERROR);
            request.setErrorCause(cause);
            fileCacheRequestRepository.save(request);
        }
        publisher.notAvailable(fileReq.getChecksum(), fileReq.getStorage(), cause, fileReq.getGroupId());
        reqGrpService.requestError(fileReq.getGroupId(),
                                   FileRequestType.AVAILABILITY,
                                   fileReq.getChecksum(),
                                   fileReq.getStorage(),
                                   null,
                                   Lists.newArrayList(),
                                   cause);
    }

    /**
     * Return all the request that can be restored in internal cache to not reach the internal cache size limit.
     * Requests of external cache are ignored, so its requests can always be restored.
     */
    private List<FileCacheRequest> calculateRestorables(Collection<FileCacheRequest> requests) {
        List<FileCacheRequest> restorablesInternalCacheRequest = new ArrayList<>();

        // Calculate internal cache size available by adding internal cache file sizes sum and already queued requests
        Long availableCacheSize = cacheService.getFreeSpaceInBytes();
        Long pendingSize = fileCacheRequestRepository.getPendingFileSize();
        long availableSize = availableCacheSize - pendingSize;

        Iterator<FileCacheRequest> iterator = findAllInternalCacheFileRequest(requests).iterator();
        boolean internalCacheLimitReached = false;
        Long totalSize = 0L;
        while (iterator.hasNext()) {
            FileCacheRequest internalCacheRequest = iterator.next();

            if ((totalSize + internalCacheRequest.getFileSize()) <= availableSize) {
                restorablesInternalCacheRequest.add(internalCacheRequest);
                totalSize += internalCacheRequest.getFileSize();
            } else {
                internalCacheLimitReached = true;
            }
        }
        if (internalCacheLimitReached) {
            if (!globalCacheLimitReached) {
                Long occupation = 100 - ((availableCacheSize / cacheService.getMaxCacheSizeBytes()) * 100);

                notificationClient.notify(String.format(
                    "One or many files to restore has been locked cause internal cache is full (%s%%)",
                    occupation), "Internal cache is full", NotificationLevel.WARNING, DefaultRole.ADMIN);
                globalCacheLimitReached = true;
            }
        } else {
            globalCacheLimitReached = false;
        }
        return restorablesInternalCacheRequest;
    }

    /**
     * Filter requests in order to return only requests for internal cache.
     */
    private Collection<FileCacheRequest> findAllInternalCacheFileRequest(Collection<FileCacheRequest> requests) {
        Set<FileCacheRequest> fileInternalCacheRequests = new HashSet<>();
        // Requests grouped by storage in order to find the plugin
        Map<String, List<FileCacheRequest>> groupByStorage = requests.stream()
                                                                     .collect(Collectors.groupingBy(FileCacheRequest::getStorage));
        for (Map.Entry<String, List<FileCacheRequest>> storage : groupByStorage.entrySet()) {
            try {
                PluginConfiguration conf = pluginService.getPluginConfigurationByLabel(storage.getKey());
                INearlineStorageLocation storagePlugin = pluginService.getPlugin(conf.getBusinessId());

                if (storagePlugin.isInternalCache()) {
                    // Add all requests for internal cache
                    fileInternalCacheRequests.addAll(storage.getValue());
                }
            } catch (ModuleException | NotAvailablePluginConfigurationException e) {
                LOGGER.warn("Impossible to get the plugin with the storage {} in order to know if internal cache or "
                            + "external cache, cause {}", storage.getKey(), e.getMessage());
            }
        }
        return fileInternalCacheRequests;
    }

    /**
     * Schedule a {@link JobInfo} for the given {@link  FileRestorationWorkingSubset}.<br/>
     * NOTE : A new transaction is created for each call at this method. It is mandatory to avoid having too long transactions.
     */
    public JobInfo scheduleJob(FileRestorationWorkingSubset workingSubset, String plgBusinessId) {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(FileCacheRequestJob.DATA_STORAGE_CONF_BUSINESS_ID, plgBusinessId));
        parameters.add(new JobParameter(FileCacheRequestJob.WORKING_SUB_SET, workingSubset));
        JobInfo jobInfo = jobInfoService.createAsQueued(new JobInfo(false,
                                                                    StorageJobsPriority.FILE_CACHE_JOB,
                                                                    parameters,
                                                                    authResolver.getUser(),
                                                                    FileCacheRequestJob.class.getName()));
        workingSubset.getFileRestorationRequests()
                     .forEach(r -> fileCacheRequestRepository.updateStatusAndJobId(FileRequestStatus.PENDING,
                                                                                   jobInfo.getId().toString(),
                                                                                   r.getId()));
        em.flush();
        em.clear();
        return jobInfo;
    }

    /**
     * Creates {@link FileCacheRequest} for each nearline {@link FileReference} to be available for download.
     * After copy in cache, files will be available until the given expiration date.
     *
     * @param fileReferences    Files to put into the cache
     * @param availabilityHours Duration in hours of available files in the cache internal or external
     * @param groupId           Request group id
     */
    public void makeAvailable(Set<FileReference> fileReferences, int availabilityHours, String groupId) {
        // Check files already available in cache
        Set<FileReference> availables = cacheService.getAndUpdateFileCacheIfExists(fileReferences, groupId);
        Set<FileReference> toRestore = fileReferences.stream()
                                                     .filter(f -> !availables.contains(f))
                                                     .collect(Collectors.toSet());
        // Notify available
        notifyAlreadyAvailablesInCache(availables, groupId);
        // Create a restoration request for all to restore
        for (FileReference f : toRestore) {
            create(f, availabilityHours, groupId);
        }
    }

    /**
     * Update a list of {@link FileCacheRequest}s when the storage origin cannot be handled.
     * A storage origin cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     */
    public void handleStorageNotAvailable(Collection<FileCacheRequest> fileRefRequests) {
        fileRefRequests.forEach(request -> this.handleStorageNotAvailable(request, Optional.empty()));
    }

    /**
     * Update a {@link FileCacheRequest} when the storage origin cannot be handled.
     * A storage origin cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     */
    private void handleStorageNotAvailable(FileCacheRequest request, Optional<String> errorCause) {
        // The storage destination is unknown, we can already set the request in error status
        String message = errorCause.orElse(String.format(
            "File <%s> cannot be handle for restoration as origin storage <%s> is unknown or disabled.",
            request.getFileReference().getMetaInfo().getFileName(),
            request.getStorage()));
        request.setStatus(FileRequestStatus.ERROR);
        request.setErrorCause(message);
        fileCacheRequestRepository.save(request);
        LOGGER.error("[AVAILABILITY ERROR] File {} is not available. Cause : {}",
                     request.getFileReference().getMetaInfo().getChecksum(),
                     request.getErrorCause());
        publisher.notAvailable(request.getChecksum(),
                               request.getStorage(),
                               request.getErrorCause(),
                               request.getGroupId());
        reqGrpService.requestError(request.getGroupId(),
                                   FileRequestType.AVAILABILITY,
                                   request.getChecksum(),
                                   request.getStorage(),
                                   null,
                                   request.getFileReference().getLazzyOwners(),
                                   message);
    }

    /**
     * Send {@link FileReferenceEvent} for available given files.
     *
     * @param availables          newly available files
     * @param availabilityGroupId business request identifier of the availability request associated.
     */
    private void notifyAvailables(Set<FileReference> availables, String availabilityGroupId) {
        for (FileReference fileRef : availables) {
            String checksum = fileRef.getMetaInfo().getChecksum();
            String storage = fileRef.getLocation().getStorage();
            String message = String.format("file %s (checksum %s) is available for download.",
                                           fileRef.getMetaInfo().getFileName(),
                                           checksum);
            LOGGER.debug("[AVAILABILITY SUCCESS {}] - {}", checksum, message);
            try {
                // For online files we have to generate access url though storage microservice
                String url = downloadTokenService.generateDownloadUrl(checksum);
                publisher.available(checksum,
                                    storage,
                                    storage,
                                    new URL(url),
                                    fileRef.getLazzyOwners(),
                                    message,
                                    availabilityGroupId,
                                    null);
                reqGrpService.requestSuccess(availabilityGroupId,
                                             FileRequestType.AVAILABILITY,
                                             checksum,
                                             storage,
                                             null,
                                             fileRef.getLazzyOwners(),
                                             fileRef);
            } catch (ModuleException | MalformedURLException e) {
                LOGGER.error(e.getMessage(), e);
                publisher.notAvailable(checksum, storage, e.getMessage(), availabilityGroupId);
                reqGrpService.requestError(availabilityGroupId,
                                           FileRequestType.AVAILABILITY,
                                           checksum,
                                           storage,
                                           null,
                                           fileRef.getLazzyOwners(),
                                           e.getMessage());
            }
        }
    }

    /**
     * Notify all files as AVAILABLE.
     */
    private void notifyAlreadyAvailablesInCache(Set<FileReference> availables, String groupId) {
        for (FileReference fileRef : availables) {
            String checksum = fileRef.getMetaInfo().getChecksum();
            String storage = fileRef.getLocation().getStorage();
            String message = String.format("File %s (checksum %s) is available for download.",
                                           fileRef.getMetaInfo().getFileName(),
                                           checksum);
            URL availableUrl;
            try {
                availableUrl = new URL("file", null, cacheService.getFilePath(checksum));
                publisher.available(checksum,
                                    "cache",
                                    storage,
                                    availableUrl,
                                    fileRef.getLazzyOwners(),
                                    message,
                                    groupId,
                                    null);
                reqGrpService.requestSuccess(groupId,
                                             FileRequestType.AVAILABILITY,
                                             checksum,
                                             storage,
                                             null,
                                             Lists.newArrayList(),
                                             fileRef);
            } catch (MalformedURLException e) {
                // Should not happen
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Delete all requests for the given storage identifier
     */
    public void deleteByStorage(String storageLocationId, Optional<FileRequestStatus> status) {
        if (status.isPresent()) {
            fileCacheRequestRepository.deleteByStorageAndStatus(storageLocationId, status.get());
        } else {
            fileCacheRequestRepository.deleteByStorage(storageLocationId);
        }
    }

    public void delete(FileReference deletedFileRef) {
        fileCacheRequestRepository.deleteByfileReference(deletedFileRef);
    }

}
