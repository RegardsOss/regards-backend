/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.filecatalog.service.location;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.fileaccess.dto.StorageLocationConfigurationDto;
import fr.cnes.regards.modules.filecatalog.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.filecatalog.dao.IStorageLocationMonitoringRepository;
import fr.cnes.regards.modules.filecatalog.dao.IStorageLocationRepository;
import fr.cnes.regards.modules.filecatalog.dao.result.StorageLocationMonitoringResult;
import fr.cnes.regards.modules.filecatalog.dao.result.StorageLocationPendingFilesMonitoringResult;
import fr.cnes.regards.modules.filecatalog.domain.StorageLocation;
import fr.cnes.regards.modules.filecatalog.domain.StorageLocationMonitoring;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Thibaud Michaudel
 **/
@Service
public class StorageLocationMonitoringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageLocationMonitoringService.class);

    private final IStorageLocationMonitoringRepository storageLocationMonitoringRepository;

    private final IStorageLocationRepository storageLocationRepository;

    private final IFileReferenceRepository fileReferenceRepository;

    private final StorageLocationService storageLocationService;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final INotificationClient notificationClient;

    @Value("${regards.storage.data.storage.threshold.percent:70}")
    private Integer threshold;

    @Value("${regards.storage.data.storage.critical.threshold.percent:90}")
    private Integer criticalThreshold;

    public StorageLocationMonitoringService(IStorageLocationMonitoringRepository storageLocationMonitoringRepository,
                                            IStorageLocationRepository storageLocationRepository,
                                            IFileReferenceRepository fileReferenceRepository,
                                            StorageLocationService storageLocationService,
                                            IRuntimeTenantResolver runtimeTenantResolver,
                                            INotificationClient notificationClient) {
        this.storageLocationMonitoringRepository = storageLocationMonitoringRepository;
        this.storageLocationRepository = storageLocationRepository;
        this.fileReferenceRepository = fileReferenceRepository;
        this.storageLocationService = storageLocationService;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.notificationClient = notificationClient;
    }

    /**
     * Monitor all storage locations to calculate information about stored files.
     * Either recalculates everything if reset is true, or just the new FileReferences otherwise.
     * In any cases, recalculate the number of pending files (that may have increased or decreased).
     */
    @MultitenantTransactional
    public void monitorStorageLocations(Boolean reset) throws ModuleException {
        LOGGER.trace("Starting locations monitor process (reset={})", reset.toString());
        OffsetDateTime monitoringDate = OffsetDateTime.now();
        // Retrieve last monitoring process
        StorageLocationMonitoring storageLocationMonitoring = getStorageLocationMonitoring(reset, monitoringDate);

        long start = System.currentTimeMillis();

        Map<String, StorageLocation> storageLocations = storageLocationRepository.findAll()
                                                                                 .stream()
                                                                                 .collect(Collectors.toMap(
                                                                                     StorageLocation::getName,
                                                                                     Function.identity()));

        monitorStorageLocationsSpace(storageLocations, storageLocationMonitoring, monitoringDate);
        monitorPendingFiles(storageLocations);

        storageLocationRepository.saveAll(storageLocations.values());
        long finish = System.currentTimeMillis();
        storageLocationMonitoring.setLastMonitoringDuration(finish - start);
        storageLocationMonitoring.setLastMonitoringDate(monitoringDate);
        storageLocationMonitoring.setRunning(false);
        storageLocationMonitoringRepository.save(storageLocationMonitoring);
    }

    /**
     * Monitor all storage locations to calculate information about stored files.
     * May complete the storageLocationsMap with new StorageLocations.
     */
    private void monitorStorageLocationsSpace(Map<String, StorageLocation> storageLocations,
                                              StorageLocationMonitoring storageLocationMonitoring,
                                              OffsetDateTime monitoringDate) throws ModuleException {
        List<StorageLocationMonitoringResult> aggregations = aggregateFilesSizePerStorage(storageLocationMonitoring.getLastFileReferenceIdMonitored());
        for (StorageLocationMonitoringResult monitoringResult : aggregations) {
            // Retrieve associated storageLocation if exists
            StorageLocation storageLocation = storageLocations.computeIfAbsent(monitoringResult.storage(),
                                                                               StorageLocation::new);
            storageLocation.setLastUpdateDate(monitoringDate);
            storageLocation.setTotalSizeOfReferencedFilesInKo(storageLocation.getTotalSizeOfReferencedFilesInKo() + (
                monitoringResult.usedSize()
                / 1024));
            storageLocation.setNumberOfReferencedFiles(storageLocation.getNumberOfReferencedFiles()
                                                       + monitoringResult.numberOfFiles());

            if ((storageLocationMonitoring.getLastFileReferenceIdMonitored() == null)
                || (storageLocationMonitoring.getLastFileReferenceIdMonitored()
                    < monitoringResult.lastFileReferenceId())) {
                storageLocationMonitoring.setLastFileReferenceIdMonitored(monitoringResult.lastFileReferenceId());
            }

            // Check for occupation ratio limit reached
            Optional<StorageLocationConfigurationDto> oConfiguration = storageLocationService.getStorageLocationConfiguration(
                monitoringResult.storage());
            // If no plugin configuration is found, it's a virtual storage location with no limits
            if (oConfiguration.isPresent()) {
                StorageLocationConfigurationDto storageLocationConfiguration = oConfiguration.get();
                if (storageLocationConfiguration.getAllocatedSizeInKo() != null
                    && storageLocationConfiguration.getAllocatedSizeInKo() > 0L) {
                    Double ratio = (Double.valueOf(storageLocation.getTotalSizeOfReferencedFilesInKo())
                                    / (storageLocationConfiguration.getAllocatedSizeInKo())) * 100;
                    if (ratio >= criticalThreshold) {
                        String message = String.format(
                            "Storage location %s has reach its disk usage critical threshold. %nActual occupation: %.2f%%, critical threshold: %s%%",
                            storageLocation.getName(),
                            ratio,
                            criticalThreshold);
                        LOGGER.error(message);
                        notifyAdmins(String.format("Data storageLocation %s is full", storageLocation.getName()),
                                     message,
                                     NotificationLevel.ERROR,
                                     MimeTypeUtils.TEXT_PLAIN);
                        MaintenanceManager.setMaintenance(runtimeTenantResolver.getTenant());
                    } else if (ratio >= threshold) {
                        String message = String.format("Storage location %s has reach its "
                                                       + "disk usage threshold. %nActual occupation: %.2f%%, threshold: %s%%",
                                                       storageLocation.getName(),
                                                       ratio,
                                                       criticalThreshold);
                        LOGGER.warn(message);
                        notifyAdmins(String.format("Data storageLocation %s is almost full", storageLocation.getName()),
                                     message,
                                     NotificationLevel.WARNING,
                                     MimeTypeUtils.TEXT_PLAIN);
                    } else {
                        LOGGER.trace("Storage location {} monitoring done with no warnings.",
                                     storageLocation.getName());
                    }
                } else {
                    LOGGER.warn(
                        "[STORAGE LOCATION] Ratio calculation for {} storageLocation disabled cause storageLocation allowed size is not configured.",
                        storageLocation.getName());
                }
            }
        }
    }

    private @NotNull StorageLocationMonitoring getStorageLocationMonitoring(Boolean reset,
                                                                            OffsetDateTime monitoringDate) {
        StorageLocationMonitoring storageLocationMonitoring = storageLocationMonitoringRepository.findById(0L)
                                                                                                 .orElse(new StorageLocationMonitoring(
                                                                                                     true,
                                                                                                     null,
                                                                                                     null,
                                                                                                     null));
        if (reset && (storageLocationMonitoring.getId() != null)) {
            storageLocationMonitoringRepository.delete(storageLocationMonitoring);
            storageLocationRepository.resetAll(monitoringDate);
            storageLocationMonitoring = new StorageLocationMonitoring(true, null, null, null);
        }
        storageLocationMonitoring.setRunning(true);
        storageLocationMonitoringRepository.save(storageLocationMonitoring);
        return storageLocationMonitoring;
    }

    /**
     * Calculate the total file size by adding fileSize of each
     * {@link fr.cnes.regards.modules.filecatalog.domain.FileReference FileReference} with an id over the given id.
     */
    private List<StorageLocationMonitoringResult> aggregateFilesSizePerStorage(Long lastReferencedFileId) {
        if (lastReferencedFileId != null) {
            return fileReferenceRepository.getTotalFileSizeAggregation(lastReferencedFileId);
        } else {
            return fileReferenceRepository.getTotalFileSizeAggregation();
        }
    }

    private void monitorPendingFiles(Map<String, StorageLocation> storageLocations) {
        long start = System.currentTimeMillis();
        LOGGER.debug("Start monitoring storage pending files ...");
        List<StorageLocationPendingFilesMonitoringResult> pendingAggregations = fileReferenceRepository.getPendingFilesAggregation();
        for (StorageLocationPendingFilesMonitoringResult monitoringResult : pendingAggregations) {
            StorageLocation storageLocation = storageLocations.computeIfAbsent(monitoringResult.storage(),
                                                                               StorageLocation::new);
            storageLocation.setNumberOfPendingFiles(monitoringResult.numberOfPendingFiles());
        }
        LOGGER.debug("Monitoring of storage pending files done in {}ms", System.currentTimeMillis() - start);
    }

    private void notifyAdmins(String title, String message, NotificationLevel type, MimeType mimeType) {
        notificationClient.notify(message, title, type, mimeType, DefaultRole.ADMIN);
    }
}
