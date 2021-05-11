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
package fr.cnes.regards.modules.storage.service.location;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storage.dao.IStorageLocationRepository;
import fr.cnes.regards.modules.storage.dao.IStorageMonitoringRepository;
import fr.cnes.regards.modules.storage.domain.database.StorageLocation;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.domain.database.StorageMonitoring;
import fr.cnes.regards.modules.storage.domain.database.StorageMonitoringAggregation;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileRequestInfoDTO;
import fr.cnes.regards.modules.storage.domain.event.FileRequestType;
import fr.cnes.regards.modules.storage.service.cache.CacheScheduler;
import fr.cnes.regards.modules.storage.service.cache.CacheService;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.request.FileCacheRequestService;
import fr.cnes.regards.modules.storage.service.file.request.FileCopyRequestService;
import fr.cnes.regards.modules.storage.service.file.request.FileDeletionRequestService;
import fr.cnes.regards.modules.storage.service.file.request.FileStorageRequestService;

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
    private CacheScheduler cacheScheduler;

    @Value("${regards.storage.data.storage.threshold.percent:70}")
    private Integer threshold;

    @Value("${regards.storage.data.storage.critical.threshold.percent:90}")
    private Integer criticalThreshold;

    public Optional<StorageLocation> search(String storage) {
        return storageLocationRepo.findByName(storage);
    }

    /**
     * Retrieve one {@link StorageLocation} by its id
     * @param storageId
     * @throws EntityNotFoundException
     */
    public StorageLocationDTO getById(String storageId) throws ModuleException {
        Optional<StorageLocation> oLoc = storageLocationRepo.findByName(storageId);
        Optional<StorageLocationConfiguration> oConf = pLocationConfService.search(storageId);
        Long nbStorageError = storageService.count(storageId, FileRequestStatus.ERROR);
        Long nbDeletionError = deletionReqService.count(storageId, FileRequestStatus.ERROR);
        boolean deletionRunning = deletionReqService.isDeletionRunning(storageId);
        boolean copyRunning = copyService.isCopyRunning(storageId);
        boolean storageRunning = storageService.isStorageRunning(storageId);
        Long nbReferencedFiles = null;
        Long totalSizeOfReferencedFiles = null;
        StorageLocationConfiguration conf = null;
        if (oConf.isPresent() && oLoc.isPresent()) {
            conf = oConf.get();
            StorageLocation loc = oLoc.get();
            if (conf.getPluginConfiguration() != null) {
                nbReferencedFiles = loc.getNumberOfReferencedFiles();
                totalSizeOfReferencedFiles = loc.getTotalSizeOfReferencedFilesInKo();
            }
        } else if (oConf.isPresent()) {
            conf = oConf.get();
        } else if (oLoc.isPresent()) {
            StorageLocation loc = oLoc.get();
            nbReferencedFiles = loc.getNumberOfReferencedFiles();
            totalSizeOfReferencedFiles = loc.getTotalSizeOfReferencedFilesInKo();
        } else {
            throw new EntityNotFoundException(storageId, StorageLocation.class);
        }
        return new StorageLocationDTO(storageId, nbReferencedFiles, totalSizeOfReferencedFiles, nbStorageError,
                                      nbDeletionError, storageRunning, deletionRunning, copyRunning, conf,
                                      pLocationConfService.allowPhysicalDeletion(conf));
    }

    /**
     * Retrieve all known storage locations with there monitoring informations.
     * @return {@link StorageLocationDTO}s
     */
    public Set<StorageLocationDTO> getAllLocations() throws ModuleException {
        Set<StorageLocationDTO> locationsDto = Sets.newHashSet();
        // Get all monitored locations
        Map<String, StorageLocation> monitoredLocations = storageLocationRepo.findAll().stream()
                .collect(Collectors.toMap(l -> l.getName(), l -> l));
        // Get all non monitored locations
        List<StorageLocationConfiguration> confs = pLocationConfService.searchAll();
        // Handle all online storage configured
        for (StorageLocationConfiguration conf : confs) {
            Long nbStorageError = storageService.count(conf.getName(), FileRequestStatus.ERROR);
            Long nbDeletionError = deletionReqService.count(conf.getName(), FileRequestStatus.ERROR);
            boolean deletionRunning = deletionReqService.isDeletionRunning(conf.getName());
            boolean copyRunning = copyService.isCopyRunning(conf.getName());
            boolean storageRunning = storageService.isStorageRunning(conf.getName());
            StorageLocation monitored = monitoredLocations.get(conf.getName());
            if (monitored != null) {
                locationsDto.add(new StorageLocationDTO(conf.getName(), monitored.getNumberOfReferencedFiles(),
                                                        monitored.getTotalSizeOfReferencedFilesInKo(), nbStorageError,
                                                        nbDeletionError, storageRunning, deletionRunning, copyRunning,
                                                        conf, pLocationConfService.allowPhysicalDeletion(conf)));
                monitoredLocations.remove(monitored.getName());
            } else {
                locationsDto.add(new StorageLocationDTO(conf.getName(), 0L, 0L, nbStorageError, nbDeletionError,
                                                        storageRunning, deletionRunning, copyRunning, conf,
                                                        pLocationConfService.allowPhysicalDeletion(conf)));
            }
        }
        // Handle not configured storage as OFFLINE ones
        for (StorageLocation monitored : monitoredLocations.values()) {
            Long nbStorageError = 0L;
            Long nbDeletionError = 0L;
            locationsDto.add(new StorageLocationDTO
                    (monitored.getName(), monitored.getNumberOfReferencedFiles(),
                     monitored.getTotalSizeOfReferencedFilesInKo(), nbStorageError, nbDeletionError, false, false,
                     false, new StorageLocationConfiguration(monitored.getName(), null, null), false));
        }
        return locationsDto;
    }

    /**
     * Monitor all storage locations to calculate informations about stored files.
     */
    public void monitorStorageLocations(Boolean reset) {
        LOGGER.trace("Starting locations monitor process (reset={})", reset.toString());
        OffsetDateTime monitoringDate = OffsetDateTime.now();
        // Retrieve last monitoring process
        StorageMonitoring storageMonitoring = storageMonitoringRepo.findById(0L)
                .orElse(new StorageMonitoring(true, null, null, null));
        if (reset && (storageMonitoring.getId() != null)) {
            storageMonitoringRepo.delete(storageMonitoring);
            storageLocationRepo.deleteAll();
            storageMonitoring = new StorageMonitoring(true, null, null, null);
        }
        storageMonitoring.setRunning(true);
        storageMonitoringRepo.save(storageMonitoring);

        // lets ask the data base to calculate the used space per data storage
        long start = System.currentTimeMillis();
        Collection<StorageMonitoringAggregation> aggregations = fileReferenceService
                .aggragateFilesSizePerStorage(storageMonitoring.getLastFileReferenceIdMonitored());
        LOGGER.trace("Aggregation calcul done (reset={})", reset.toString());
        for (StorageMonitoringAggregation agg : aggregations) {
            // Retrieve associated storage info if exists
            Optional<StorageLocation> oStorage = storageLocationRepo.findByName(agg.getStorage());
            StorageLocation storage = oStorage.orElse(new StorageLocation(agg.getStorage()));
            storage.setLastUpdateDate(monitoringDate);
            storage.setTotalSizeOfReferencedFilesInKo(storage.getTotalSizeOfReferencedFilesInKo()
                    + (agg.getUsedSize() / 1024));
            storage.setNumberOfReferencedFiles(storage.getNumberOfReferencedFiles() + agg.getNumberOfFileReference());

            if ((storageMonitoring.getLastFileReferenceIdMonitored() == null)
                    || (storageMonitoring.getLastFileReferenceIdMonitored() < agg.getLastFileReferenceId())) {
                storageMonitoring.setLastFileReferenceIdMonitored(agg.getLastFileReferenceId());
            }
            storageLocationRepo.save(storage);

            // Check for occupation ratio limit reached
            Optional<StorageLocationConfiguration> conf = pLocationConfService.search(agg.getStorage());
            if (conf.isPresent() && (conf.get().getAllocatedSizeInKo() != null)
                    && (conf.get().getAllocatedSizeInKo() > 0L)) {
                Double ratio = (Double.valueOf(storage.getTotalSizeOfReferencedFilesInKo())
                        / (conf.get().getAllocatedSizeInKo())) * 100;
                if (ratio >= criticalThreshold) {
                    String message = String
                            .format("Storage location %s has reach its disk usage critical threshold. %nActual occupation: %.2f%%, critical threshold: %s%%",
                                    storage.getName(), ratio, criticalThreshold);
                    LOGGER.error(message);
                    notifyAdmins(String.format("Data storage %s is full", storage.getName()), message,
                                 NotificationLevel.ERROR, MimeTypeUtils.TEXT_PLAIN);
                    MaintenanceManager.setMaintenance(runtimeTenantResolver.getTenant());
                } else if (ratio >= threshold) {
                    String message = String.format("Storage location %s has reach its "
                            + "disk usage threshold. %nActual occupation: %.2f%%, threshold: %s%%", storage.getName(),
                                                   ratio, criticalThreshold);
                    LOGGER.warn(message);
                    notifyAdmins(String.format("Data storage %s is almost full", storage.getName()), message,
                                 NotificationLevel.WARNING, MimeTypeUtils.TEXT_PLAIN);
                } else {
                    LOGGER.trace("Storage location %s monitoring done with no warnings.", storage.getName());
                }
            } else {
                LOGGER.warn("[STORAGE LOCATION] Ratio calculation for {} storage disabled cause storage allowed size is not configured.",
                            storage.getName());
            }
        }
        long finish = System.currentTimeMillis();
        storageMonitoring.setLastMonitoringDuration(finish - start);
        storageMonitoring.setLastMonitoringDate(monitoringDate);
        storageMonitoring.setRunning(false);
        storageMonitoringRepo.save(storageMonitoring);
    }

    private void notifyAdmins(String title, String message, NotificationLevel type, MimeType mimeType) {
        notificationClient.notify(message, title, type, mimeType, DefaultRole.ADMIN);
    }

    /**
     * Up (if possible), the priority of the given storage location configuration
     * @param storageLocationId
     * @throws EntityNotFoundException
     */
    public void increasePriority(String storageLocationId) throws EntityNotFoundException {
        pLocationConfService.increasePriority(storageLocationId);
    }

    /**
     * Down (if possible), the priority of the given storage location configuration
     * @param storageLocationId
     * @throws EntityNotFoundException
     */
    public void decreasePriority(String storageLocationId) throws EntityNotFoundException {
        pLocationConfService.decreasePriority(storageLocationId);
    }

    /**
     * Delete the given storage location informations. <br/>
     * Files reference are not deleted, to do so, use {@link #deleteFiles(String, Boolean)}
     * @param storageLocationId
     * @throws EntityNotFoundException
     */
    public void delete(String storageLocationId) throws ModuleException {
        // Delete storage location plugin configuration
        Optional<StorageLocationConfiguration> pStorageLocation = pLocationConfService.search(storageLocationId);
        if (pStorageLocation.isPresent()) {
            // If a storage configuration is associated, delete it.
            pLocationConfService.delete(pStorageLocation.get().getId());
        }
        // Delete informations in storage locations
        storageLocationRepo.deleteByName(storageLocationId);
        storageMonitoringRepo.deleteAll();
        // Delete requests
        storageService.deleteByStorage(storageLocationId, Optional.empty());
        deletionReqService.deleteByStorage(storageLocationId, Optional.empty());
        cacheReqService.deleteByStorage(storageLocationId, Optional.empty());
    }

    /**
     * Delete all referenced files of the give storage location
     * @param storageLocationId
     * @param forceDelete remove reference if physical file deletion fails.
     * @throws ModuleException
     */
    public void deleteFiles(String storageLocationId, Boolean forceDelete) throws ModuleException {
        if (storageLocationId.equals(CacheService.CACHE_NAME)) {
            cacheScheduler.cleanCache();
        } else {
            deletionService.scheduleJob(storageLocationId, forceDelete);
        }
    }

    /**
     * Copy files of a directory from one storage to an other
     * @param storageLocationId
     * @param destinationStorageId
     * @param sourcePath
     * @param destinationPath
     */
    public void copyFiles(String storageLocationId, String sourcePath, String destinationStorageId,
            Optional<String> destinationPath, Collection<String> types) {
        copyService.scheduleJob(storageLocationId, sourcePath, destinationStorageId, destinationPath, types);
    }

    /**
     * Retry all requests in error for the given storage location.
     * @param storageLocationId
     * @param type
     * @throws EntityOperationForbiddenException
     */
    public void retryErrors(String storageLocationId, FileRequestType type) throws EntityOperationForbiddenException {
        switch (type) {
            case DELETION:
                deletionReqService.scheduleJobs(FileRequestStatus.ERROR, Lists.newArrayList(storageLocationId));
                break;
            case STORAGE:
                storageService.scheduleJobs(FileRequestStatus.ERROR, Lists.newArrayList(storageLocationId),
                                            Lists.newArrayList());
                break;
            case AVAILABILITY:
            case COPY:
            case REFERENCE:
            default:
                throw new EntityOperationForbiddenException(storageLocationId, StorageLocation.class,
                        String.format("Retry for type %s is forbidden", type));
        }
    }

    /**
     * Creates a new configuration for the given storage location.
     * @param storageLocation
     * @throws ModuleException
     */
    public StorageLocationDTO configureLocation(StorageLocationDTO storageLocation) throws ModuleException {
        Assert.notNull(storageLocation, "Storage location to configure can not be null");
        Assert.notNull(storageLocation.getName(), "Storage location name to configure can not be null");
        Assert.notNull(storageLocation.getConfiguration(), "Storage location / Configuration can not be null");
        StorageLocationConfiguration newConf = pLocationConfService
                .create(storageLocation.getName(), storageLocation.getConfiguration().getPluginConfiguration(),
                        storageLocation.getConfiguration().getAllocatedSizeInKo());
        return new StorageLocationDTO(storageLocation.getName(), 0L, 0L, 0L, 0L, false, false, false, newConf,
                                      pLocationConfService.allowPhysicalDeletion(newConf));
    }

    /**
     * Update the configuration of the given storage location.
     * @param storageLocation
     * @throws ModuleException
     */
    public StorageLocationDTO updateLocationConfiguration(String storageId, StorageLocationDTO storageLocation)
            throws ModuleException {
        Assert.notNull(storageLocation, "Storage location to configure can not be null");
        Assert.notNull(storageLocation.getName(), "Storage location name to update can not be null");
        Assert.notNull(storageLocation.getConfiguration(), "Storage location / Configuration can not be null");
        StorageLocationConfiguration newConf = pLocationConfService.update(storageId,
                                                                           storageLocation.getConfiguration());
        return new StorageLocationDTO(storageLocation.getName(), 0L, 0L, 0L, 0L, false, false, false, newConf,
                                      pLocationConfService.allowPhysicalDeletion(newConf));
    }

    /**
     * @param storageLocationId
     * @param type
     */
    public void deleteRequests(String storageLocationId, FileRequestType type, Optional<FileRequestStatus> status) {
        switch (type) {
            case AVAILABILITY:
                cacheReqService.deleteByStorage(storageLocationId, status);
                break;
            case COPY:
                copyService.deleteByStorage(storageLocationId, status);
                break;
            case DELETION:
                deletionReqService.deleteByStorage(storageLocationId, status);
                break;
            case REFERENCE:
                break;
            case STORAGE:
                storageService.deleteByStorage(storageLocationId, status);
                break;
            default:
                break;
        }
    }

    public Page<FileRequestInfoDTO> getRequestInfos(String storageLocationId, FileRequestType type,
            Optional<FileRequestStatus> status, Pageable page) {
        Page<FileRequestInfoDTO> results = Page.empty(page);
        switch (type) {
            case AVAILABILITY:
                break;
            case COPY:
                break;
            case DELETION:
                Page<FileDeletionRequest> delRequests;
                if (status.isPresent()) {
                    delRequests = deletionReqService.search(storageLocationId, status.get(), page);
                } else {
                    delRequests = deletionReqService.search(storageLocationId, page);
                }
                results = new PageImpl<FileRequestInfoDTO>(
                        delRequests.getContent().stream().map(this::toRequestInfosDto).collect(Collectors.toList()),
                        page, delRequests.getTotalElements());
                break;
            case REFERENCE:
                break;
            case STORAGE:
                Page<FileStorageRequest> requests;
                if (status.isPresent()) {
                    requests = storageService.search(storageLocationId, status.get(), page);
                } else {
                    requests = storageService.search(storageLocationId, page);
                }
                results = new PageImpl<FileRequestInfoDTO>(
                        requests.getContent().stream().map(this::toRequestInfosDto).collect(Collectors.toList()), page,
                        requests.getTotalElements());
                break;
            default:
                break;
        }
        return results;
    }

    private FileRequestInfoDTO toRequestInfosDto(FileStorageRequest request) {
        String fileName = "";
        if (request.getMetaInfo() != null) {
            fileName = request.getMetaInfo().getFileName();
        }
        return FileRequestInfoDTO.build(request.getId(), fileName, FileRequestType.STORAGE, request.getStatus(),
                                        request.getErrorCause());
    }

    private FileRequestInfoDTO toRequestInfosDto(FileDeletionRequest request) {
        String fileName = "";
        if ((request.getFileReference() != null) && (request.getFileReference().getMetaInfo() != null)) {
            fileName = request.getFileReference().getMetaInfo().getFileName();
        }
        return FileRequestInfoDTO.build(request.getId(), fileName, FileRequestType.DELETION, request.getStatus(),
                                        request.getErrorCause());
    }

}
