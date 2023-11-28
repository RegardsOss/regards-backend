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
package fr.cnes.regards.modules.storage.service.location;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.filecatalog.dto.FileRequestStatus;
import fr.cnes.regards.modules.filecatalog.dto.FileRequestType;
import fr.cnes.regards.modules.filecatalog.dto.StorageLocationDto;
import fr.cnes.regards.modules.filecatalog.dto.request.FileRequestInfoDto;
import fr.cnes.regards.modules.storage.dao.*;
import fr.cnes.regards.modules.storage.domain.database.*;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.service.StorageJobsPriority;
import fr.cnes.regards.modules.storage.service.cache.CacheService;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.job.PeriodicStorageLocationJob;
import fr.cnes.regards.modules.storage.service.file.request.FileCacheRequestService;
import fr.cnes.regards.modules.storage.service.file.request.FileCopyRequestService;
import fr.cnes.regards.modules.storage.service.file.request.FileDeletionRequestService;
import fr.cnes.regards.modules.storage.service.file.request.FileStorageRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to handle actions on {@link StorageLocation}s
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class StorageLocationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageLocationService.class);

    @Autowired
    private FileReferenceService fileReferenceService;

    @Autowired
    private FileDeletionRequestService deletionService;

    @Autowired
    private FileStorageRequestService storageService;

    @Autowired
    private FileCopyRequestService copyService;

    @Autowired
    private IStorageLocationRepository storageLocationRepo;

    @Autowired
    private IStorageMonitoringRepository storageMonitoringRepo;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private FileDeletionRequestService deletionReqService;

    @Autowired
    private FileCacheRequestService cacheReqService;

    @Autowired
    private StorageLocationConfigurationService pLocationConfService;

    @Autowired
    private IFileDeletetionRequestRepository deletionReqRepo;

    @Autowired
    private IFileStorageRequestRepository storageReqRepo;

    @Autowired
    private IStorageLocationConfigurationRepostory storageLocConfRepo;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Value("${regards.storage.data.storage.threshold.percent:70}")
    private Integer threshold;

    @Value("${regards.storage.data.storage.critical.threshold.percent:90}")
    private Integer criticalThreshold;

    @Value("${regards.storage.requests.retry.page:1000}")
    private int pageRetrySize;

    public Optional<StorageLocation> search(String storage) {
        return storageLocationRepo.findByName(storage);
    }

    public StorageLocationDto getByName(String storageName) throws ModuleException {
        Optional<StorageLocation> oLoc = storageLocationRepo.findByName(storageName);
        Optional<StorageLocationConfiguration> oConf = pLocationConfService.search(storageName);
        long nbStorageError = storageService.count(storageName, FileRequestStatus.ERROR);
        long nbDeletionError = deletionReqService.count(storageName, FileRequestStatus.ERROR);
        boolean deletionRunning = deletionReqService.isDeletionRunning(storageName);
        boolean copyRunning = copyService.isCopyRunning(storageName);
        boolean storageRunning = storageService.isStorageRunning(storageName);
        boolean pendingActionRunning = storageService.isPendingActionRunning(storageName);
        boolean pendingActionRemaining = false;
        long nbReferencedFiles = 0L;
        long totalSizeOfReferencedFiles = 0L;
        long nbPendingFiles = 0L;
        StorageLocationConfiguration conf = null;
        if (oConf.isPresent() && oLoc.isPresent()) {
            conf = oConf.get();
            StorageLocation loc = oLoc.get();
            pendingActionRemaining = loc.getPendingActionRemaining();
            if (conf.getPluginConfiguration() != null) {
                nbReferencedFiles = loc.getNumberOfReferencedFiles();
                totalSizeOfReferencedFiles = loc.getTotalSizeOfReferencedFilesInKo();
                nbPendingFiles = loc.getNumberOfPendingFiles();
            }
        } else if (oConf.isPresent()) {
            conf = oConf.get();
        } else if (oLoc.isPresent()) {
            StorageLocation loc = oLoc.get();
            pendingActionRemaining = loc.getPendingActionRemaining();
            nbReferencedFiles = loc.getNumberOfReferencedFiles();
            totalSizeOfReferencedFiles = loc.getTotalSizeOfReferencedFilesInKo();
        } else {
            throw new EntityNotFoundException(storageName, StorageLocation.class);
        }
        return StorageLocationDto.build(storageName, conf != null ? conf.toDto() : null)
                                 .withFilesInformation(nbReferencedFiles, nbPendingFiles, totalSizeOfReferencedFiles)
                                 .withPendingActionRemaining(pendingActionRemaining)
                                 .withErrorInformation(nbStorageError, nbDeletionError)
                                 .withRunningProcessesInformation(storageRunning,
                                                                  deletionRunning,
                                                                  copyRunning,
                                                                  pendingActionRunning)
                                 .withAllowPhysicalDeletion(pLocationConfService.allowPhysicalDeletion(conf));
    }

    /**
     * Retrieve all known storage locations with there monitoring informations.
     *
     * @return {@link StorageLocationDto}s
     */
    public Set<StorageLocationDto> getAllLocations() throws ModuleException {
        Set<StorageLocationDto> locationsDto = Sets.newHashSet();
        // Get all monitored locations
        Map<String, StorageLocation> monitoredLocations = storageLocationRepo.findAll()
                                                                             .stream()
                                                                             .collect(Collectors.toMap(StorageLocation::getName,
                                                                                                       l -> l));
        // Get all non monitored locations
        List<StorageLocationConfiguration> confs = pLocationConfService.searchAll();
        // Handle all online storage configured
        for (StorageLocationConfiguration conf : confs) {
            Long nbStorageError = storageService.count(conf.getName(), FileRequestStatus.ERROR);
            Long nbDeletionError = deletionReqService.count(conf.getName(), FileRequestStatus.ERROR);
            boolean deletionRunning = deletionReqService.isDeletionRunning(conf.getName());
            boolean copyRunning = copyService.isCopyRunning(conf.getName());
            boolean storageRunning = storageService.isStorageRunning(conf.getName());
            boolean pendingActionRunning = storageService.isPendingActionRunning(conf.getName());
            StorageLocation monitored = monitoredLocations.get(conf.getName());
            if (monitored != null) {
                locationsDto.add(StorageLocationDto.build(conf.getName(), conf.toDto())
                                                   .withFilesInformation(monitored.getNumberOfReferencedFiles(),
                                                                         monitored.getNumberOfPendingFiles(),
                                                                         monitored.getTotalSizeOfReferencedFilesInKo())
                                                   .withPendingActionRemaining(monitored.getPendingActionRemaining())
                                                   .withErrorInformation(nbStorageError, nbDeletionError)
                                                   .withRunningProcessesInformation(storageRunning,
                                                                                    deletionRunning,
                                                                                    copyRunning,
                                                                                    pendingActionRunning)
                                                   .withAllowPhysicalDeletion(pLocationConfService.allowPhysicalDeletion(
                                                       conf)));
                monitoredLocations.remove(monitored.getName());
            } else {
                locationsDto.add(StorageLocationDto.build(conf.getName(), conf.toDto())
                                                   .withErrorInformation(nbStorageError, nbDeletionError)
                                                   .withRunningProcessesInformation(storageRunning,
                                                                                    deletionRunning,
                                                                                    copyRunning,
                                                                                    pendingActionRunning)
                                                   .withAllowPhysicalDeletion(pLocationConfService.allowPhysicalDeletion(
                                                       conf)));
            }
        }
        // Handle not configured storage as OFFLINE ones
        for (StorageLocation monitored : monitoredLocations.values()) {
            long nbStorageError = 0L;
            long nbDeletionError = 0L;
            StorageLocationConfiguration conf = new StorageLocationConfiguration(monitored.getName(), null, null);
            locationsDto.add(StorageLocationDto.build(monitored.getName(), conf.toDto())
                                               .withFilesInformation(monitored.getNumberOfReferencedFiles(),
                                                                     monitored.getNumberOfPendingFiles(),
                                                                     monitored.getTotalSizeOfReferencedFilesInKo())
                                               .withErrorInformation(nbStorageError, nbDeletionError));
        }
        return locationsDto;
    }

    /**
     * Monitor all storage locations to calculate information about stored files.
     */
    public void monitorStorageLocations(Boolean reset) {
        LOGGER.trace("Starting locations monitor process (reset={})", reset.toString());
        OffsetDateTime monitoringDate = OffsetDateTime.now();
        // Retrieve last monitoring process
        StorageMonitoring storageMonitoring = storageMonitoringRepo.findById(0L)
                                                                   .orElse(new StorageMonitoring(true,
                                                                                                 null,
                                                                                                 null,
                                                                                                 null));
        if (reset && (storageMonitoring.getId() != null)) {
            storageMonitoringRepo.delete(storageMonitoring);
            storageLocationRepo.resetAll(monitoringDate);
            storageMonitoring = new StorageMonitoring(true, null, null, null);
        }
        storageMonitoring.setRunning(true);
        storageMonitoringRepo.save(storageMonitoring);

        // lets ask the data base to calculate the used space per data storage
        long start = System.currentTimeMillis();
        Collection<StorageMonitoringAggregation> aggregations = fileReferenceService.aggregateFilesSizePerStorage(
            storageMonitoring.getLastFileReferenceIdMonitored());
        LOGGER.trace("Aggregation calcul done (reset={})", reset);
        List<String> storages = aggregations.stream()
                                            .map(StorageMonitoringAggregation::getStorage)
                                            .collect(Collectors.toList());
        for (StorageMonitoringAggregation agg : aggregations) {
            // Retrieve associated storage info if exists
            Optional<StorageLocation> oStorage = storageLocationRepo.findByName(agg.getStorage());
            StorageLocation storage = oStorage.orElse(new StorageLocation(agg.getStorage()));
            storage.setLastUpdateDate(monitoringDate);
            storage.setTotalSizeOfReferencedFilesInKo(storage.getTotalSizeOfReferencedFilesInKo() + (agg.getUsedSize()
                                                                                                     / 1024));
            storage.setNumberOfReferencedFiles(storage.getNumberOfReferencedFiles() + agg.getNumberOfFileReference());

            if ((storageMonitoring.getLastFileReferenceIdMonitored() == null)
                || (storageMonitoring.getLastFileReferenceIdMonitored() < agg.getLastFileReferenceId())) {
                storageMonitoring.setLastFileReferenceIdMonitored(agg.getLastFileReferenceId());
            }
            storageLocationRepo.save(storage);

            // Check for occupation ratio limit reached
            Optional<StorageLocationConfiguration> conf = pLocationConfService.search(agg.getStorage());
            if (conf.isPresent() && (conf.get().getAllocatedSizeInKo() != null) && (conf.get().getAllocatedSizeInKo()
                                                                                    > 0L)) {
                Double ratio = (Double.valueOf(storage.getTotalSizeOfReferencedFilesInKo()) / (conf.get()
                                                                                                   .getAllocatedSizeInKo()))
                               * 100;
                if (ratio >= criticalThreshold) {
                    String message = String.format(
                        "Storage location %s has reach its disk usage critical threshold. %nActual occupation: %.2f%%, critical threshold: %s%%",
                        storage.getName(),
                        ratio,
                        criticalThreshold);
                    LOGGER.error(message);
                    notifyAdmins(String.format("Data storage %s is full", storage.getName()),
                                 message,
                                 NotificationLevel.ERROR,
                                 MimeTypeUtils.TEXT_PLAIN);
                    MaintenanceManager.setMaintenance(runtimeTenantResolver.getTenant());
                } else if (ratio >= threshold) {
                    String message = String.format("Storage location %s has reach its "
                                                   + "disk usage threshold. %nActual occupation: %.2f%%, threshold: %s%%",
                                                   storage.getName(),
                                                   ratio,
                                                   criticalThreshold);
                    LOGGER.warn(message);
                    notifyAdmins(String.format("Data storage %s is almost full", storage.getName()),
                                 message,
                                 NotificationLevel.WARNING,
                                 MimeTypeUtils.TEXT_PLAIN);
                } else {
                    LOGGER.trace("Storage location {} monitoring done with no warnings.", storage.getName());
                }
            } else {
                LOGGER.warn(
                    "[STORAGE LOCATION] Ratio calculation for {} storage disabled cause storage allowed size is not configured.",
                    storage.getName());
            }
        }
        monitorPendingFiles();
        long finish = System.currentTimeMillis();
        storageMonitoring.setLastMonitoringDuration(finish - start);
        storageMonitoring.setLastMonitoringDate(monitoringDate);
        storageMonitoring.setRunning(false);
        storageMonitoringRepo.save(storageMonitoring);
    }

    private void monitorPendingFiles() {
        long start = System.currentTimeMillis();
        LOGGER.debug("Start monitoring storage pending files ...");
        Collection<StoragePendingFilesAggregation> pendingAggregations = fileReferenceService.aggregateFilesPendingPerStorage();
        storageLocationRepo.findAll().forEach(loc -> {
            Long numberOfPending = pendingAggregations.stream()
                                                      .filter(pa -> pa.getStorage().equals(loc.getName()))
                                                      .map(StoragePendingFilesAggregation::getNumberOfPendingReferences)
                                                      .findFirst()
                                                      .orElse(0L);
            loc.setNumberOfPendingFiles(numberOfPending);
            storageLocationRepo.save(loc);
        });
        LOGGER.debug("Monitoring of storage pending files done in {}ms", System.currentTimeMillis() - start);
    }

    private void notifyAdmins(String title, String message, NotificationLevel type, MimeType mimeType) {
        notificationClient.notify(message, title, type, mimeType, DefaultRole.ADMIN);
    }

    /**
     * Up (if possible), the priority of the given storage location configuration
     *
     * @param storageName storage location id
     * @throws EntityNotFoundException if corresponding storage location cannot be found
     */
    public void increasePriority(String storageName) throws EntityNotFoundException {
        pLocationConfService.increasePriority(storageName);
    }

    /**
     * Down (if possible), the priority of the given storage location configuration
     *
     * @param storageName storage location id
     * @throws EntityNotFoundException if corresponding storage location cannot be found
     */
    public void decreasePriority(String storageName) throws EntityNotFoundException {
        pLocationConfService.decreasePriority(storageName);
    }

    /**
     * Delete the given storage location information. <br/>
     * Files reference are not deleted, to do so, use {@link #deleteFiles(String, Boolean, String, String)}
     *
     * @param storageLocationName storage location id
     */
    public void delete(String storageLocationName) throws ModuleException {
        // Delete storage location plugin configuration
        Optional<StorageLocationConfiguration> pStorageLocation = pLocationConfService.search(storageLocationName);
        if (pStorageLocation.isPresent()) {
            // If a storage configuration is associated, delete it.
            pLocationConfService.delete(pStorageLocation.get().getId());
        }
        // Delete informations in storage locations
        storageLocationRepo.deleteByName(storageLocationName);
        storageMonitoringRepo.deleteAll();
        // Delete requests
        storageService.deleteByStorage(storageLocationName, Optional.empty());
        deletionReqService.deleteByStorage(storageLocationName, Optional.empty());
        cacheReqService.deleteByStorage(storageLocationName, Optional.empty());
    }

    /**
     * Delete all referenced files of the given storage location
     *
     * @param storageName  storage location name
     * @param forceDelete  remove reference if physical file deletion fails.
     * @param sessionOwner the user who has requested the deletion of files
     * @param session      tags the deletion files requests with a session name
     */
    public void deleteFiles(String storageName, Boolean forceDelete, String sessionOwner, String session)
        throws ModuleException {
        if (storageName.equals(CacheService.CACHE_NAME)) {
            cacheService.scheduleCacheCleanUp(authResolver.getUser(), forceDelete);
        } else {
            deletionService.scheduleJob(storageName, forceDelete, sessionOwner, session);
        }
    }

    public void copyFiles(String storageName,
                          String sourcePath,
                          String destinationStorageId,
                          Optional<String> destinationPath,
                          Collection<String> types,
                          String sessionOwner,
                          String session) {
        copyService.scheduleJob(storageName,
                                sourcePath,
                                destinationStorageId,
                                destinationPath,
                                types,
                                sessionOwner,
                                session);
    }

    /**
     * Retry all requests in error for the given storage location.
     */
    public void retryErrors(String storageName, FileRequestType type) throws EntityOperationForbiddenException {
        switch (type) {
            case DELETION:
                deletionReqService.scheduleJobs(FileRequestStatus.ERROR, Lists.newArrayList(storageName));
                break;
            case STORAGE:
                storageService.scheduleJobs(FileRequestStatus.ERROR,
                                            Lists.newArrayList(storageName),
                                            Lists.newArrayList());
                break;
            case AVAILABILITY:
            case COPY:
            case REFERENCE:
            default:
                throw new EntityOperationForbiddenException(storageName,
                                                            StorageLocation.class,
                                                            String.format("Retry for type %s is forbidden", type));
        }
    }

    /**
     * Retry requests in error for a given source and session. Only requests of type
     * {@link FileRequestType#DELETION} and {@link FileRequestType#STORAGE} are retried.
     *
     * @param source  origin of the requests, also called sessionOwner
     * @param session group name which was given during the processing of the requests
     */
    public void retryErrorsBySourceAndSession(String source, String session) {
        // CASE DELETION REQUESTS, retry them all
        Pageable pageToRequest = PageRequest.of(0, pageRetrySize, Sort.by("id"));
        Page<FileDeletionRequest> deletionReqPage;
        do {
            deletionReqPage = this.deletionReqRepo.findByStatusAndSessionOwnerAndSession(FileRequestStatus.ERROR,
                                                                                         source,
                                                                                         session,
                                                                                         pageToRequest);
            List<FileDeletionRequest> deletionReqList = deletionReqPage.getContent();
            // update all requests status and decrement errors to the session agent
            if (!deletionReqList.isEmpty()) {
                this.deletionReqService.retryBySession(deletionReqList, source, session);
            }
        } while (deletionReqPage.hasNext());

        // CASE STORAGE REQUESTS, retry them all
        Page<FileStorageRequestAggregation> storageReqPage;
        do {
            storageReqPage = this.storageReqRepo.findByStatusAndSessionOwnerAndSession(FileRequestStatus.ERROR,
                                                                                       source,
                                                                                       session,
                                                                                       pageToRequest);
            List<FileStorageRequestAggregation> storageReqList = storageReqPage.getContent();
            // update all requests status and decrement errors to the session agent
            if (!storageReqList.isEmpty()) {
                this.storageService.retryBySession(storageReqList, source, session);
            }
        } while (deletionReqPage.hasNext());
    }

    /**
     * Creates a new configuration for the given storage location.
     */
    public StorageLocationDto configureLocation(StorageLocationDto storageLocation) throws ModuleException {
        Assert.notNull(storageLocation, "Storage location to configure can not be null");
        Assert.notNull(storageLocation.getName(), "Storage location name to configure can not be null");
        Assert.notNull(storageLocation.getConfiguration(), "Storage location / Configuration can not be null");
        StorageLocationConfiguration conf = pLocationConfService.create(storageLocation.getName(),
                                                                        storageLocation.getConfiguration()
                                                                                       .getPluginConfiguration()
                                                                        != null ?
                                                                            PluginConfiguration.fromDto(storageLocation.getConfiguration()
                                                                                                                       .getPluginConfiguration()) :
                                                                            null,
                                                                        storageLocation.getConfiguration()
                                                                                       .getAllocatedSizeInKo());
        return StorageLocationDto.build(storageLocation.getName(), conf.toDto())
                                 .withAllowPhysicalDeletion(pLocationConfService.allowPhysicalDeletion(conf));
    }

    /**
     * Update the configuration of the given storage location.
     */
    public StorageLocationDto updateLocationConfiguration(String storageName, StorageLocationDto storageLocation)
        throws ModuleException {
        Assert.notNull(storageLocation, "Storage location to configure can not be null");
        Assert.notNull(storageLocation.getName(), "Storage location name to update can not be null");
        Assert.notNull(storageLocation.getConfiguration(), "Storage location / Configuration can not be null");
        StorageLocationConfiguration newConf = pLocationConfService.update(storageName,
                                                                           StorageLocationConfiguration.fromDto(
                                                                               storageLocation.getConfiguration()));
        return StorageLocationDto.build(storageLocation.getName(), newConf.toDto())
                                 .withAllowPhysicalDeletion(pLocationConfService.allowPhysicalDeletion(newConf));
    }

    public void deleteRequests(String storageName, FileRequestType type, Optional<FileRequestStatus> status) {
        switch (type) {
            case AVAILABILITY:
                cacheReqService.deleteByStorage(storageName, status);
                break;
            case COPY:
                copyService.deleteByStorage(storageName, status);
                break;
            case DELETION:
                deletionReqService.deleteByStorage(storageName, status);
                break;
            case REFERENCE:
                break;
            case STORAGE:
                storageService.deleteByStorage(storageName, status);
                break;
            default:
                break;
        }
    }

    /**
     * Update {@link StorageLocation} to set pending action remaining boolean value
     */
    public void updateLocationPendingAction(String locationName, boolean pendingActionRemaining) {
        Optional<StorageLocation> oLocation = storageLocationRepo.findByName(locationName);
        if (oLocation.isPresent()) {
            StorageLocation location = oLocation.get();
            location.setPendingActionRemaining(pendingActionRemaining);
            storageLocationRepo.save(location);
        }
    }

    public Page<FileRequestInfoDto> getRequestInfos(String storageName,
                                                    FileRequestType type,
                                                    Optional<FileRequestStatus> status,
                                                    Pageable page) {
        Page<FileRequestInfoDto> results = Page.empty(page);
        switch (type) {
            case AVAILABILITY:
                break;
            case COPY:
                break;
            case DELETION:
                Page<FileDeletionRequest> delRequests;
                if (status.isPresent()) {
                    delRequests = deletionReqService.search(storageName, status.get(), page);
                } else {
                    delRequests = deletionReqService.search(storageName, page);
                }
                results = new PageImpl<>(delRequests.getContent()
                                                    .stream()
                                                    .map(this::toRequestInfosDto)
                                                    .collect(Collectors.toList()),
                                         page,
                                         delRequests.getTotalElements());
                break;
            case REFERENCE:
                break;
            case STORAGE:
                Page<FileStorageRequestAggregation> requests;
                if (status.isPresent()) {
                    requests = storageService.search(storageName, status.get(), page);
                } else {
                    requests = storageService.search(storageName, page);
                }
                results = new PageImpl<>(requests.getContent()
                                                 .stream()
                                                 .map(this::toRequestInfosDto)
                                                 .collect(Collectors.toList()), page, requests.getTotalElements());
                break;
            default:
                break;
        }
        return results;
    }

    private FileRequestInfoDto toRequestInfosDto(FileStorageRequestAggregation request) {
        String fileName = "";
        if (request.getMetaInfo() != null) {
            fileName = request.getMetaInfo().getFileName();
        }
        return FileRequestInfoDto.build(request.getId(),
                                        fileName,
                                        FileRequestType.STORAGE,
                                        request.getStatus(),
                                        request.getErrorCause());
    }

    private FileRequestInfoDto toRequestInfosDto(FileDeletionRequest request) {
        String fileName = "";
        if ((request.getFileReference() != null) && (request.getFileReference().getMetaInfo() != null)) {
            fileName = request.getFileReference().getMetaInfo().getFileName();
        }
        return FileRequestInfoDto.build(request.getId(),
                                        fileName,
                                        FileRequestType.DELETION,
                                        request.getStatus(),
                                        request.getErrorCause());
    }

    /**
     * Schedule a {@link PeriodicStorageLocationJob} for each storage location containing
     * files with pending remaining actions
     */
    public Set<JobInfo> runPeriodicTasks() {
        Set<JobInfo> jobs = Sets.newHashSet();
        storageLocationRepo.findStorageWithPendingActionRemaining().forEach(storage -> {
            Set<JobParameter> parameters = Sets.newHashSet();
            parameters.add(new JobParameter(PeriodicStorageLocationJob.DATA_STORAGE_CONF_BUSINESS_ID,
                                            storage.getName()));
            jobs.add(jobInfoService.createAsQueued(new JobInfo(false,
                                                               StorageJobsPriority.STORAGE_PERIODIC_ACTION_JOB,
                                                               parameters,
                                                               authResolver.getUser(),
                                                               PeriodicStorageLocationJob.class.getName())));
            LOGGER.debug("[STORAGE PERIODIC ACTION] Job scheduled on storage {}", storage.getName());
        });
        return jobs;
    }
}
