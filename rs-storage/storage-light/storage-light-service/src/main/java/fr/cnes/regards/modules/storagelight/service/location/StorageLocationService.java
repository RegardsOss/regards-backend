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
package fr.cnes.regards.modules.storagelight.service.location;

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
import org.springframework.stereotype.Service;
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
import fr.cnes.regards.modules.storagelight.dao.IStorageLocationRepository;
import fr.cnes.regards.modules.storagelight.dao.IStorageMonitoringRepository;
import fr.cnes.regards.modules.storagelight.domain.database.PrioritizedStorage;
import fr.cnes.regards.modules.storagelight.domain.database.StorageLocation;
import fr.cnes.regards.modules.storagelight.domain.database.StorageMonitoring;
import fr.cnes.regards.modules.storagelight.domain.database.StorageMonitoringAggregation;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.StorageLocationType;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageType;
import fr.cnes.regards.modules.storagelight.service.file.FileReferenceService;
import fr.cnes.regards.modules.storagelight.service.file.request.FileCacheRequestService;
import fr.cnes.regards.modules.storagelight.service.file.request.FileCopyRequestService;
import fr.cnes.regards.modules.storagelight.service.file.request.FileDeletionRequestService;
import fr.cnes.regards.modules.storagelight.service.file.request.FileStorageRequestService;

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
    private FileStorageRequestService storageReqService;

    @Autowired
    private FileDeletionRequestService deletionReqService;

    @Autowired
    private FileCacheRequestService cacheReqService;

    @Autowired
    private PrioritizedStorageService pStorageService;

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
     * @return
     * @throws EntityNotFoundException
     */
    public StorageLocationDTO getById(String storageId) throws EntityNotFoundException {
        Optional<StorageLocation> oLoc = storageLocationRepo.findByName(storageId);
        Optional<PrioritizedStorage> oConf = pStorageService.search(storageId);
        Long nbStorageError = storageReqService.count(storageId, FileRequestStatus.ERROR);
        Long nbDeletionError = deletionReqService.count(storageId, FileRequestStatus.ERROR);
        if (oConf.isPresent() && oLoc.isPresent()) {
            PrioritizedStorage conf = oConf.get();
            StorageLocation loc = oLoc.get();
            StorageLocationType type = conf.getStorageType() == StorageType.ONLINE ? StorageLocationType.ONLINE
                    : StorageLocationType.NEALINE;
            return StorageLocationDTO.build(conf.getStorageConfiguration().getBusinessId(), type,
                                            loc.getNumberOfReferencedFiles(), loc.getTotalSizeOfReferencedFiles(), null,
                                            nbStorageError, nbDeletionError, conf);
        } else if (oConf.isPresent()) {
            PrioritizedStorage conf = oConf.get();
            StorageLocationType type = conf.getStorageType() == StorageType.ONLINE ? StorageLocationType.ONLINE
                    : StorageLocationType.NEALINE;
            return StorageLocationDTO.build(conf.getStorageConfiguration().getBusinessId(), type, null, null, null,
                                            nbStorageError, nbDeletionError, conf);
        } else if (oLoc.isPresent()) {
            StorageLocation loc = oLoc.get();
            return StorageLocationDTO.build(storageId, StorageLocationType.OFFLINE, loc.getNumberOfReferencedFiles(),
                                            loc.getTotalSizeOfReferencedFiles(), null, 0L, 0L, null);
        } else {
            throw new EntityNotFoundException(storageId, StorageLocation.class);
        }
    }

    /**
     * Retrieve all known storage locations with there monitoring informations.
     * @return {@link StorageLocationDTO}s
     */
    public Set<StorageLocationDTO> getAllLocations() {
        Set<StorageLocationDTO> locationsDto = Sets.newHashSet();
        // Get all monitored locations
        Map<String, StorageLocation> monitoredLocations = storageLocationRepo.findAll().stream()
                .collect(Collectors.toMap(l -> l.getName(), l -> l));
        // Get all non monitored locations
        List<PrioritizedStorage> onlines = pStorageService.search(StorageType.ONLINE);
        List<PrioritizedStorage> nearlines = pStorageService.search(StorageType.NEARLINE);
        // Handle all online storage configured
        for (PrioritizedStorage online : onlines) {
            Long nbStorageError = storageReqService.count(online.getStorageConfiguration().getLabel(),
                                                          FileRequestStatus.ERROR);
            Long nbDeletionError = deletionReqService.count(online.getStorageConfiguration().getLabel(),
                                                            FileRequestStatus.ERROR);
            StorageLocation monitored = monitoredLocations.get(online.getStorageConfiguration().getBusinessId());
            if (monitored != null) {
                locationsDto.add(StorageLocationDTO
                        .build(online.getStorageConfiguration().getBusinessId(), StorageLocationType.ONLINE,
                               monitored.getNumberOfReferencedFiles(), monitored.getTotalSizeOfReferencedFiles(), null,
                               nbStorageError, nbDeletionError, online));
                monitoredLocations.remove(monitored.getName());
            } else {
                locationsDto.add(StorageLocationDTO.build(online.getStorageConfiguration().getBusinessId(),
                                                          StorageLocationType.ONLINE, 0L, 0L, null, nbStorageError,
                                                          nbDeletionError, online));
            }
        }
        // Handle all nearlines storage configured
        for (PrioritizedStorage nearline : nearlines) {
            Long nbStorageError = storageReqService.count(nearline.getStorageConfiguration().getLabel(),
                                                          FileRequestStatus.ERROR);
            Long nbDeletionError = deletionReqService.count(nearline.getStorageConfiguration().getLabel(),
                                                            FileRequestStatus.ERROR);
            StorageLocation monitored = monitoredLocations.get(nearline.getStorageConfiguration().getBusinessId());
            if (monitored != null) {
                locationsDto.add(StorageLocationDTO
                        .build(nearline.getStorageConfiguration().getBusinessId(), StorageLocationType.NEALINE,
                               monitored.getNumberOfReferencedFiles(), monitored.getTotalSizeOfReferencedFiles(), null,
                               nbStorageError, nbDeletionError, nearline));
                monitoredLocations.remove(monitored.getName());
            } else {
                locationsDto.add(StorageLocationDTO.build(nearline.getStorageConfiguration().getBusinessId(),
                                                          StorageLocationType.NEALINE, 0L, 0L, null, nbStorageError,
                                                          nbDeletionError, nearline));
            }
        }
        // Handle not configured storage as OFFLINE ones
        for (StorageLocation monitored : monitoredLocations.values()) {
            Long nbStorageError = 0L;
            Long nbDeletionError = 0L;
            locationsDto.add(StorageLocationDTO
                    .build(monitored.getName(), StorageLocationType.OFFLINE, monitored.getNumberOfReferencedFiles(),
                           monitored.getTotalSizeOfReferencedFiles(), null, nbStorageError, nbDeletionError, null));
        }
        return locationsDto;
    }

    /**
     * Monitor all storage locations to calculate informations about stored files.
     */
    public void monitorDataStorages() {
        OffsetDateTime monitoringDate = OffsetDateTime.now();
        // Retrieve last monitoring process
        StorageMonitoring storageMonitoring = storageMonitoringRepo.findById(0L)
                .orElse(new StorageMonitoring(true, null, null, null));
        storageMonitoring.setRunning(true);
        storageMonitoringRepo.save(storageMonitoring);

        // lets ask the data base to calculate the used space per data storage
        long start = System.currentTimeMillis();
        Collection<StorageMonitoringAggregation> aggregations = fileReferenceService
                .aggragateFilesSizePerStorage(storageMonitoring.getLastFileReferenceIdMonitored());
        for (StorageMonitoringAggregation agg : aggregations) {
            // Retrieve associated storage info if exists
            Optional<StorageLocation> oStorage = storageLocationRepo.findByName(agg.getStorage());
            StorageLocation storage = oStorage.orElse(new StorageLocation(agg.getStorage()));
            storage.setLastUpdateDate(monitoringDate);
            storage.setTotalSizeOfReferencedFiles(storage.getTotalSizeOfReferencedFiles() + agg.getUsedSize());
            storage.setNumberOfReferencedFiles(storage.getNumberOfReferencedFiles() + agg.getNumberOfFileReference());
            if ((storageMonitoring.getLastFileReferenceIdMonitored() == null)
                    || (storageMonitoring.getLastFileReferenceIdMonitored() < agg.getLastFileReferenceId())) {
                storageMonitoring.setLastFileReferenceIdMonitored(agg.getLastFileReferenceId());
            }
            storageLocationRepo.save(storage);

            // Check for occupation ratio limit reached
            if (storage.getAllowedSize() > 0) {
                Double ratio = (Double.valueOf(storage.getTotalSizeOfReferencedFiles()) / storage.getAllowedSize())
                        * 100;
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
                }
            } else {
                LOGGER.info("[STORAGE LOCATION] Ratio calculation for {} storage disabled cause storage allowed size is not configured.",
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
        Optional<PrioritizedStorage> pStorage = pStorageService.search(storageLocationId);
        if (pStorage.isPresent()) {
            pStorageService.increasePriority(pStorage.get().getId());
        } else {
            throw new EntityNotFoundException(storageLocationId, PrioritizedStorage.class);
        }
    }

    /**
     * Down (if possible), the priority of the given storage location configuration
     * @param storageLocationId
     * @throws EntityNotFoundException
     */
    public void decreasePriority(String storageLocationId) throws EntityNotFoundException {
        Optional<PrioritizedStorage> pStorage = pStorageService.search(storageLocationId);
        if (pStorage.isPresent()) {
            pStorageService.decreasePriority(pStorage.get().getId());
        } else {
            throw new EntityNotFoundException(storageLocationId, PrioritizedStorage.class);
        }
    }

    /**
     * Delete the given storage location informations. <br/>
     * Files reference are not deleted, to do so, use {@link this#deleteFiles(String, Boolean)}
     * @param storageLocationId
     * @throws EntityNotFoundException
     */
    public void delete(String storageLocationId) throws ModuleException {
        // Delete storage location plugin configuration
        Optional<PrioritizedStorage> pStorageLocation = pStorageService.search(storageLocationId);
        if (pStorageLocation.isPresent()) {
            // If a storage configuration is associated, delete it.
            pStorageService.delete(pStorageLocation.get().getId());
        }
        // Delete informations in storage locations
        storageLocationRepo.deleteByName(storageLocationId);
        storageMonitoringRepo.deleteAll();
        // Delete requests
        storageReqService.deleteByStorage(storageLocationId);
        deletionReqService.deleteByStorage(storageLocationId);
        cacheReqService.deleteByStorage(storageLocationId);
    }

    /**
     * Delete all referenced files of the give storage location
     * @param storageLocationId
     * @param forceDelete remove reference if physical file deletion fails.
     */
    public void deleteFiles(String storageLocationId, Boolean forceDelete) {
        deletionService.scheduleJob(storageLocationId, forceDelete);
    }

    /**
     * Copy files of a directory from one storage to an other
     * @param storageLocationId
     * @param destinationStorageId
     * @param pathToCopy
     */
    public void copyFiles(String storageLocationId, String destinationStorageId, String pathToCopy) {
        copyService.scheduleJob(storageLocationId, destinationStorageId, pathToCopy);
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

    public StorageLocationDTO configureLocation(StorageLocationDTO storageLocation) throws ModuleException {
        PrioritizedStorage newConf = pStorageService
                .create(storageLocation.getConfiguration().getStorageConfiguration());
        StorageLocationType type = newConf.getStorageType() == StorageType.ONLINE ? StorageLocationType.ONLINE
                : StorageLocationType.NEALINE;
        return StorageLocationDTO.build(storageLocation.getId(), type, 0L, 0L, 0L, 0L, 0L, newConf);
    }

    /**
     * @param storageLocation
     * @return
     * @throws ModuleException
     */
    public StorageLocationDTO updateLocationConfiguration(StorageLocationDTO storageLocation) throws ModuleException {
        PrioritizedStorage newConf = pStorageService.update(storageLocation.getConfiguration().getId(),
                                                            storageLocation.getConfiguration());
        StorageLocationType type = newConf.getStorageType() == StorageType.ONLINE ? StorageLocationType.ONLINE
                : StorageLocationType.NEALINE;
        return StorageLocationDTO.build(storageLocation.getId(), type, 0L, 0L, 0L, 0L, 0L, newConf);
    }

}
