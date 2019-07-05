/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.storage.dao.ICachedFileRepository;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.CoupleAvailableError;
import fr.cnes.regards.modules.storage.domain.StorageException;
import fr.cnes.regards.modules.storage.domain.database.CachedFile;
import fr.cnes.regards.modules.storage.domain.database.CachedFileState;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;
import fr.cnes.regards.modules.storage.domain.event.DataFileEventState;
import fr.cnes.regards.modules.storage.domain.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IWorkingSubset;
import fr.cnes.regards.modules.storage.domain.plugin.WorkingSubsetWrapper;
import fr.cnes.regards.modules.storage.service.job.AbstractStoreFilesJob;
import fr.cnes.regards.modules.storage.service.job.RestorationJob;

/**
 * Service to manage temporary accessibility of {@link StorageDataFile} stored with a {@link INearlineDataStorage}
 * plugin.<br/>
 * When a file is requested by {@link #restore} method this service retrieve the file from the
 * nearline datastorage plugin and copy it into his internal cache (Local disk)<br/>
 * As the cache maximum size is limited, this service queues the file requests and handle them when it is possible<br/>
 *
 * Files in cache are purged when :
 * <ul>
 * <li>Files are outdated in cache ({@link CachedFile#getExpiration()} date is past.</li>
 * <li>Cache is full and no outdated files are in cache, then the older {@link CachedFile}s are deleted.</li>
 * </ul>
 *
 *
 * @author Sylvain VISSIERE-GUERINET
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class CachedFileService implements ICachedFileService, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedFileService.class);

    /**
     * {@link IPluginService} instance
     */
    @Autowired
    private IPluginService pluginService;

    /**
     * {@link ICachedFileRepository} instance
     */
    @Autowired
    private ICachedFileRepository cachedFileRepository;

    /**
     * Cache path origine for all tenants.
     */
    @Value("${regards.storage.cache.path}")
    private String globalCachePath;

    /**
     * Maximum cache size per tenant in ko.
     */
    @Value("${regards.storage.cache.size.limit.ko.per.tenant:500000000}")
    private Long maxCacheSizeKo;

    /**
     * Upper threshold per tenant in ko to trigger the purge of older files.
     */
    @Value("${regards.storage.cache.purge.upper.threshold.ko.per.tenant:450000000}")
    private Long cacheSizePurgeUpperThreshold;

    /**
     * Lower threshold per tenant in ko to trigger the purge of older files.
     */
    @Value("${regards.storage.cache.purge.lower.threshold.ko.per.tenant:400000000}")
    private Long cacheSizePurgeLowerThreshold;

    /**
     * Cache file minimum time to live
     */
    @Value("${regards.storage.cache.minimum.time.to.live.hours}")
    private Long cacheFilesMinTtl;

    /**
     * Number of created AIPs processed on each iteration by project
     */
    @Value("${regards.storage.cache.files.iteration.limit:100}")
    private Integer filesIterationLimit;

    /**
     * {@link IDataFileDao} instance
     */
    @Autowired
    private IDataFileDao dataFileDao;

    /**
     * {@link IRuntimeTenantResolver} instance
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Resolver to know all existing tenants of the current REGARDS instance.
     */
    @Autowired
    private ITenantResolver tenantResolver;

    /**
     * {@link IAuthenticationResolver} instance
     */
    @Autowired
    private IAuthenticationResolver authResolver;

    /**
     * {@link IJobInfoService} instance
     */
    @Autowired
    private IJobInfoService jobService;

    /**
     * {@link IPublisher} instance
     */
    @Autowired
    private IPublisher publisher;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    @Lazy
    private ICachedFileService self;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            initCacheFileSystem(tenant);
            try {
                checkDiskDBCoherence(tenant);
            } catch (IOException e) {
                LOGGER.error(String.format("Could not check if cache directory for tenant %s is dirty.", tenant), e);
            }
        }
    }

    private void checkDiskDBCoherence(String tenant) throws IOException {
        runtimeTenantResolver.forceTenant(tenant);
        Page<CachedFile> shouldBeAvailableSet;
        Pageable page = PageRequest.of(0, filesIterationLimit, Direction.ASC, "id");
        do {
            shouldBeAvailableSet = cachedFileRepository.findAllByState(CachedFileState.AVAILABLE, page);
            for (CachedFile shouldBeAvailable : shouldBeAvailableSet) {
                if (Files.notExists(Paths.get(shouldBeAvailable.getLocation().getPath()))) {
                    cachedFileRepository.delete(shouldBeAvailable);
                }
            }
            page = page.next();
        } while (shouldBeAvailableSet.hasNext());

        page = PageRequest.of(0, filesIterationLimit, Direction.ASC, "id");
        Page<CachedFile> availableFiles;
        do {
            availableFiles = cachedFileRepository.findAllByState(CachedFileState.AVAILABLE, page);
            Set<String> availableFilePaths = availableFiles.getContent().stream()
                    .map(availableFile -> availableFile.getLocation().getPath().toString()).collect(Collectors.toSet());
            Files.walk(getTenantCachePath())
                    .filter(path -> availableFilePaths.contains(path.toAbsolutePath().toString()))
                    .forEach(path -> notificationClient.notify(String
                            .format("File %s is present in cache directory while it shouldn't be. Please remove this file from the cache directory",
                                    path.toString()), "Dirty cache", NotificationLevel.WARNING,
                                                               DefaultRole.PROJECT_ADMIN));
            page = availableFiles.nextPageable();
        } while (availableFiles.hasNext());
        runtimeTenantResolver.clearTenant();
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    @EventListener
    public void processEvent(TenantConnectionReady event) {
        initCacheFileSystem(event.getTenant());
    }

    /**
     * Initialize the cache file system for the given tenant
     * @param tenant
     */
    protected void initCacheFileSystem(String tenant) {
        runtimeTenantResolver.forceTenant(tenant);
        Path tenantCachePath = getTenantCachePath();
        LOGGER.debug("Initializing cache file system for tenant {} in repository {}", tenant, tenantCachePath);
        // Check that the given cache storage path is available.
        File cachedPathFile = tenantCachePath.toFile();
        if (!cachedPathFile.exists()) {
            try {
                Files.createDirectories(tenantCachePath);
            } catch (IOException e) {
                throw new StorageException(e.getMessage(), e);
            }
        }
        if (!cachedPathFile.exists() || !cachedPathFile.isDirectory() || !cachedPathFile.canRead()
                || !cachedPathFile.canWrite()) {
            throw new StorageException(
                    String.format("Error initializing storage cache directory. %s is not a valid directory",
                                  tenantCachePath));
        }
        runtimeTenantResolver.clearTenant();
    }

    @Override
    public Optional<CachedFile> getAvailableCachedFile(String pChecksum) {
        Optional<CachedFile> ocf = cachedFileRepository.findOneByChecksum(pChecksum);
        if (ocf.isPresent() && CachedFileState.AVAILABLE.equals(ocf.get().getState())) {
            return ocf;
        } else {
            return Optional.empty();
        }
    }

    @Override
    public CoupleAvailableError restore(Set<StorageDataFile> dataFilesToRestore, OffsetDateTime cacheExpirationDate) {
        if (dataFilesToRestore.isEmpty()) {
            return new CoupleAvailableError(new HashSet<>(), new HashSet<>());
        }
        LOGGER.debug("CachedFileService : run restoration process for {} files.", dataFilesToRestore.size());
        long startChecksumExtraction = System.currentTimeMillis();
        // Get files already in cache
        Set<String> dataFilesToRestoreChecksums = dataFilesToRestore.stream().map(df -> df.getChecksum())
                .collect(Collectors.toSet());
        long endChecksumExtraction = System.currentTimeMillis();
        LOGGER.trace("Checksum extraction from {} dataFiles to restore took {} ms", dataFilesToRestore.size(),
                     endChecksumExtraction - startChecksumExtraction);
        LOGGER.trace("Looking for {} checksums to restore from cache.", dataFilesToRestoreChecksums.size());
        long startFindCachedFileByChecksum = System.currentTimeMillis();
        List<CachedFile> cachedFiles = cachedFileRepository
                .findAllByChecksumInOrderByLastRequestDateAsc(dataFilesToRestoreChecksums);
        long endFindCachedFileByChecksum = System.currentTimeMillis();
        LOGGER.trace("Finding {} cached file out of {} checksums from the DB took {} ms", cachedFiles.size(),
                     dataFilesToRestore.size(), endFindCachedFileByChecksum - startFindCachedFileByChecksum);
        // Update expiration to the new cacheExpirationDate if above the last one.
        long startExpirationDataUpdate = System.currentTimeMillis();
        long nbUpdate = 0;
        for (CachedFile cachedFile : cachedFiles) {
            if (cachedFile.getExpiration().compareTo(cacheExpirationDate) > 0) {
                cachedFile.setExpiration(cacheExpirationDate);
                cachedFileRepository.save(cachedFile);
                nbUpdate++;
            }
        }
        long endExpirationDataUpdate = System.currentTimeMillis();
        LOGGER.trace("Update Expiration date of {} cached file out of {} took {} ms", nbUpdate, cachedFiles.size(),
                     endExpirationDataUpdate - startExpirationDataUpdate);

        long startFindAlreadyAvailable = System.currentTimeMillis();
        // Get cached files available
        Set<String> availableCachedFileChecksums = cachedFiles.stream()
                .filter(cf -> CachedFileState.AVAILABLE.equals(cf.getState())).map(cf -> cf.getChecksum())
                .collect(Collectors.toSet());
        Set<StorageDataFile> alreadyAvailableData = dataFilesToRestore.stream()
                .filter(df -> availableCachedFileChecksums.contains(df.getChecksum())).collect(Collectors.toSet());
        long endFindAlreadyAvailable = System.currentTimeMillis();
        LOGGER.trace("{} StorageDataFiles are already available from the cache.", alreadyAvailableData.size());
        LOGGER.trace("Finding those already available StorageDataFile took {} ms",
                     endFindAlreadyAvailable - startFindAlreadyAvailable);
        long startFindAlreadyQueued = System.currentTimeMillis();
        // Get cached files queued
        Set<String> queuedCachedFileChecksums = cachedFiles.stream()
                .filter(cf -> CachedFileState.QUEUED.equals(cf.getState())).map(cf -> cf.getChecksum())
                .collect(Collectors.toSet());
        Set<StorageDataFile> queuedData = dataFilesToRestore.stream()
                .filter(df -> queuedCachedFileChecksums.contains(df.getChecksum())).collect(Collectors.toSet());
        long endFindAlreadyQueued = System.currentTimeMillis();
        LOGGER.trace("{} StorageDataFile are already queued.", queuedData.size());
        LOGGER.trace("Finding those already queued StorageDataFile took {} ms",
                     endFindAlreadyQueued - startFindAlreadyQueued);

        // Create the list of data files not handle by cache and needed to be restored
        Set<StorageDataFile> toRetrieve = Sets.newHashSet(dataFilesToRestore);
        // Remove all files already availables in cache.
        toRetrieve.removeAll(alreadyAvailableData);
        // Try to retrieve queued files if possible
        toRetrieve.addAll(queuedData);
        LOGGER.trace("Async call...");
        self.scheduleRestorationAsync(cacheExpirationDate, toRetrieve, runtimeTenantResolver.getTenant());
        LOGGER.trace("Async called!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        return new CoupleAvailableError(alreadyAvailableData, new HashSet<>());
    }

    @Async
    @MultitenantTransactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void scheduleRestorationAsync(OffsetDateTime cacheExpirationDate, Set<StorageDataFile> toRetrieve,
            String tenant) {
        runtimeTenantResolver.forceTenant(tenant);
        self.doScheduleRestorationAsync(cacheExpirationDate, toRetrieve);
        runtimeTenantResolver.clearTenant();
    }

    @Override
    public void doScheduleRestorationAsync(OffsetDateTime cacheExpirationDate, Set<StorageDataFile> toRetrieve) {
        long startDispatching = System.currentTimeMillis();
        // Dispatch each Datafile by storage plugin.
        Multimap<Long, StorageDataFile> toRetrieveByStorage = HashMultimap.create();
        for (StorageDataFile df : toRetrieve) {
            toRetrieveByStorage.put(computeDataStorageToUseToRetrieve(df.getPrioritizedDataStorages()), df);
        }
        long endDispatching = System.currentTimeMillis();
        LOGGER.trace("Dispatching {} StorageDataFile into {} DataStorages took {} ms", toRetrieve.size(),
                     toRetrieveByStorage.keySet().size(), endDispatching - startDispatching);
        long startScheduling = System.currentTimeMillis();
        Set<StorageDataFile> errors = Sets.newHashSet();
        for (Long storageConfId : toRetrieveByStorage.keySet()) {
            errors = scheduleDataFileRestoration(storageConfId, toRetrieveByStorage.get(storageConfId),
                                                 cacheExpirationDate);
        }
        long endScheduling = System.currentTimeMillis();
        LOGGER.trace("Scheduling jobs took {} ms", endScheduling - startScheduling);
        for (StorageDataFile error : errors) {
            handleRestorationFailure(error);
        }
    }

    private Long computeDataStorageToUseToRetrieve(Set<PrioritizedDataStorage> dataStorages) {
        PrioritizedDataStorage dataStorageToUse = dataStorages.stream().sorted().findFirst().get();
        return dataStorageToUse.getId();
    }

    @Override
    public void handleRestorationSuccess(StorageDataFile data, Path restorationPath) {
        // lets set the restorationPath to the cached file and change its state
        Optional<CachedFile> ocf = cachedFileRepository.findOneByChecksum(data.getChecksum());
        try {
            if (ocf.isPresent()) {
                CachedFile cf = ocf.get();
                cf.setLocation(restorationPath.toUri().toURL());
                cf.setState(CachedFileState.AVAILABLE);
                cf = cachedFileRepository.save(cf);
                LOGGER.debug("File {} is now available in cache until {}", cf.getChecksum(),
                             cf.getExpiration().toString());
                publisher.publish(new DataFileEvent(DataFileEventState.AVAILABLE, data.getChecksum()));
            } else {
                LOGGER.error("Restauration succeed but the file with checksum {} is not associated to any cached file is database.",
                             data.getChecksum());
                publisher.publish(new DataFileEvent(DataFileEventState.ERROR, data.getChecksum()));
            }
        } catch (MalformedURLException e) {
            // this should not happens
            LOGGER.error(e.getMessage(), e);
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public void handleRestorationFailure(StorageDataFile data) {
        // Delete cached file as restoraion failed.
        Optional<CachedFile> ocf = cachedFileRepository.findOneByChecksum(data.getChecksum());
        if (ocf.isPresent()) {
            CachedFile cf = ocf.get();
            cachedFileRepository.delete(cf);
            LOGGER.error("Error during cache file restoration {}", cf.getChecksum());
        } else {
            LOGGER.error("Restoration failed but the file with checksum {} is not associated to any cached file is database.",
                         data.getChecksum());
        }
        publisher.publish(new DataFileEvent(DataFileEventState.ERROR, data.getChecksum()));
    }

    /**
     * Return the current size of the cache in octets.
     * @return {@link Long}
     */
    private Long getCacheSizeUsedOctets() {
        // @formatter:off
        return cachedFileRepository.findAll()
                .stream()
                .filter(cf -> CachedFileState.AVAILABLE.equals(cf.getState())
                        || CachedFileState.RESTORING.equals(cf.getState()))
                .mapToLong(CachedFile::getFileSize)
                .sum();
        // @formatter:on
    }

    @Override
    public int purge() {
        return purgeExpiredCachedFiles() + purgeOlderCachedFiles();
    }

    /**
     * Delete all outdated {@link CachedFile}s.<br/>
     */
    private int purgeExpiredCachedFiles() {
        int nbPurged = 0;
        LOGGER.debug("Deleting expired files from cache. Current date : {}", OffsetDateTime.now().toString());
        Pageable page = PageRequest.of(0, filesIterationLimit, Direction.ASC, "id");
        Page<CachedFile> files;
        do {
            files = cachedFileRepository.findByExpirationBefore(OffsetDateTime.now(), page);
            deleteCachedFiles(files.getContent());
            page = files.nextPageable();
            nbPurged = nbPurged + files.getNumberOfElements();
        } while (files.hasNext());
        return nbPurged;
    }

    /**
     * Delete older {@link CachedFile}s if the {link {@link #cacheSizePurgeUpperThreshold}} is reached.<br/>
     * This method method deletes as many {@link CachedFile}s as needed to set the cache size under the
     * {@link #cacheSizePurgeLowerThreshold}.
     */
    private int purgeOlderCachedFiles() {
        int nbPurged = 0;
        // Calculate cache size
        Long cacheCurrentSize = getCacheSizeUsedOctets();
        Long cacheSizePurgeUpperThresholdInOctets = cacheSizePurgeUpperThreshold * 1024;
        Long cacheSizePurgeLowerThresholdInOctets = cacheSizePurgeLowerThreshold * 1024;
        // If cache is over upper threshold size then delete older files to reached the lower threshold.
        if (cacheCurrentSize > cacheSizePurgeUpperThresholdInOctets
                && cacheSizePurgeUpperThreshold > cacheSizePurgeLowerThreshold) {
            // If files are in queued mode, so delete older files if there minimum time to live (minTtl) is reached.
            // This limit is configurable is sprinf properties of the current microservice.
            if (cachedFileRepository.countByState(CachedFileState.QUEUED) > 0) {
                LOGGER.warn("Cache is overloaded.({}Mo) Deleting older files from cache to reached lower threshold ({}Mo). ",
                            cacheCurrentSize / (1024 * 1024), cacheSizePurgeLowerThresholdInOctets / (1024 * 1024));
                Long filesTotalSizeToDelete = cacheCurrentSize - cacheSizePurgeLowerThresholdInOctets;
                Pageable page = PageRequest.of(0, filesIterationLimit, Direction.ASC, "id");
                Page<CachedFile> allOlderDeletableCachedFiles;
                do {
                    allOlderDeletableCachedFiles = cachedFileRepository
                            .findByStateAndLastRequestDateBeforeOrderByLastRequestDateAsc(CachedFileState.AVAILABLE,
                                                                                          OffsetDateTime.now()
                                                                                                  .minusHours(this.cacheFilesMinTtl),
                                                                                          page);
                    Long fileSizesSum = 0L;
                    Set<CachedFile> filesToDelete = Sets.newHashSet();
                    Iterator<CachedFile> it = allOlderDeletableCachedFiles.iterator();
                    while (fileSizesSum < filesTotalSizeToDelete && it.hasNext()) {
                        CachedFile fileToDelete = it.next();
                        filesToDelete.add(fileToDelete);
                        fileSizesSum += fileToDelete.getFileSize();
                    }
                    deleteCachedFiles(filesToDelete);
                    page = allOlderDeletableCachedFiles.nextPageable();
                    nbPurged = nbPurged + filesToDelete.size();
                } while (allOlderDeletableCachedFiles.hasNext());
            }
        }
        return nbPurged;
    }

    @Override
    public int restoreQueued() {
        int nbScheduled = 0;
        Pageable page = PageRequest.of(0, filesIterationLimit, Direction.ASC, "id");
        Page<CachedFile> queuedFilesToCache;
        do {
            queuedFilesToCache = cachedFileRepository.findAllByState(CachedFileState.QUEUED, page);
            LOGGER.debug("{} queued files to restore in cache for tenant {}", queuedFilesToCache.getNumberOfElements(),
                         runtimeTenantResolver.getTenant());
            Set<String> checksums = queuedFilesToCache.getContent().stream().map(CachedFile::getChecksum)
                    .collect(Collectors.toSet());
            Set<StorageDataFile> dataFiles = dataFileDao.findAllByChecksumIn(checksums);
            // Set an expiration date minimum of 24hours
            restore(dataFiles, OffsetDateTime.now().plusDays(1));
            page = queuedFilesToCache.nextPageable();
            nbScheduled = nbScheduled + dataFiles.size();
        } while (queuedFilesToCache.hasNext());
        return nbScheduled;
    }

    /**
     * Delete all given {@link CachedFile}s.<br/>
     * <ul>
     * <li>1. Disk deletion of the physical files</li>
     * <li>2. Database deletion of the {@link CachedFile}s
     * </ul>
     * @param filesToDelete {@link Set}<{@link CachedFile}> to delete.
     */
    private void deleteCachedFiles(Collection<CachedFile> filesToDelete) {
        LOGGER.debug("Deleting {} files from cache.", filesToDelete.size());
        for (CachedFile cachedFile : filesToDelete) {
            if (cachedFile.getLocation() != null) {
                Path fileLocation = Paths.get(cachedFile.getLocation().getPath());
                if (fileLocation.toFile().exists()) {
                    try {
                        LOGGER.debug("Deletion of cached file {} (exp date={}). {}", cachedFile.getChecksum(),
                                     cachedFile.getExpiration().toString(), fileLocation);
                        Files.delete(fileLocation);
                        cachedFileRepository.delete(cachedFile);
                        LOGGER.debug("Cached file {} deleted (exp date={}). {}", cachedFile.getChecksum(),
                                     cachedFile.getExpiration().toString(), fileLocation);
                    } catch (NoSuchFileException e) {
                        // File does not exists, just log a warning and do delet file in db.
                        LOGGER.warn(e.getMessage(), e);
                        cachedFileRepository.delete(cachedFile);
                        LOGGER.debug("Cached file {} deleted (exp date={}).", cachedFile.getChecksum(),
                                     cachedFile.getExpiration().toString(), fileLocation);
                    } catch (IOException e) {
                        // File exists but is not deletable.
                        LOGGER.error(e.getMessage(), e);
                    }
                } else {
                    LOGGER.error("File to delete {} does not exists", fileLocation);
                    cachedFileRepository.delete(cachedFile);
                    LOGGER.debug("Cached file {} deleted (exp date={}). {}", cachedFile.getChecksum(),
                                 cachedFile.getExpiration().toString(), fileLocation);
                }
            } else {
                cachedFileRepository.delete(cachedFile);
                LOGGER.debug("Cached file {} deleted (exp date={}).", cachedFile.getChecksum(),
                             cachedFile.getExpiration().toString());
            }
        }
    }

    /**
     * Compute the {@link StorageDataFile}s restorable to not over the max cache size limit {@link #maxCacheSizeKo}.
     * @param dataFilesToRestore {@link StorageDataFile}s to restore
     * @param expirationDate {@link OffsetDateTime} Expiration date of the {@link CachedFile} to restore in cache.
     * @return the {@link StorageDataFile}s restorable
     */
    private Set<StorageDataFile> getRestorableDataFiles(Collection<StorageDataFile> dataFilesToRestore,
            OffsetDateTime expirationDate) {
        // 2.2 Caculate total size of files to restore
        Long totalFilesSize = dataFilesToRestore.stream().mapToLong(df -> df.getFileSize()).sum();
        Long currentCacheTotalSize = getCacheSizeUsedOctets();
        Long cacheMaxSizeInOctets = maxCacheSizeKo * 1024;
        Long availableCacheSize = cacheMaxSizeInOctets - currentCacheTotalSize;

        final Set<StorageDataFile> restorableFiles = Sets.newHashSet();
        // Check if there is enought space left in cache to restore all files into.
        if (totalFilesSize < availableCacheSize) {
            restorableFiles.addAll(dataFilesToRestore);
        } else {
            // There is no enought space left in cache to restore all files.
            // Return maximum number of files to restore.
            Set<String> checksums = Sets.newHashSet();
            Long totalFileSizesToHandle = 0L;
            for (StorageDataFile fileToRestore : dataFilesToRestore) {
                Long fileSize = fileToRestore.getFileSize();
                boolean fileAlreadyHandled = checksums.contains(fileToRestore.getChecksum());
                if (fileAlreadyHandled || totalFileSizesToHandle + fileSize < availableCacheSize) {
                    restorableFiles.add(fileToRestore);
                    if (!fileAlreadyHandled) {
                        checksums.add(fileToRestore.getChecksum());
                        totalFileSizesToHandle = totalFileSizesToHandle + fileSize;
                    }
                }
            }
        }

        // Initialize all files in cache
        for (StorageDataFile dataFileToRestore : dataFilesToRestore) {
            Optional<CachedFile> ocf = cachedFileRepository.findOneByChecksum(dataFileToRestore.getChecksum());
            // If cached file already exists do not store a new one.
            if (!ocf.isPresent()) {
                CachedFileState fileState = CachedFileState.QUEUED;
                if (restorableFiles.contains(dataFileToRestore)) {
                    fileState = CachedFileState.RESTORING;
                }
                CachedFile cf = cachedFileRepository.save(new CachedFile(dataFileToRestore, expirationDate, fileState));
                LOGGER.debug("New file queued for cache {} exp={}", cf.getChecksum(), cf.getExpiration().toString());
            }
        }

        return restorableFiles;
    }

    /**
     * Do schedule {@link StorageDataFile}s restoration {@link PluginConfiguration} to restore files.
     * @param pluginConfId {@link PluginConfiguration} Plugin to use to restore files.
     * @param dataFilesToRestore {@link StorageDataFile}s to restore.
     * @param expirationDate {@link OffsetDateTime} Expiration date of the {@link CachedFile} to restore in cache.
     * @return {@link StorageDataFile}s that can not be restored.
     */
    private Set<StorageDataFile> scheduleDataFileRestoration(Long pluginConfId,
            Collection<StorageDataFile> dataFilesToRestore, OffsetDateTime expirationDate) {
        LOGGER.debug("CachedFileService : Init restoration job for {} files.", dataFilesToRestore.size());
        Set<StorageDataFile> restorabledataFiles = getRestorableDataFiles(dataFilesToRestore, expirationDate);
        LOGGER.debug("CachedFileService : Schedule restoration job for {} files.", restorabledataFiles.size());
        Set<StorageDataFile> nonRestoredFiles = Sets.newHashSet();
        if (!restorabledataFiles.isEmpty()) {
            try {
                INearlineDataStorage<IWorkingSubset> storageToUse = pluginService.getPlugin(pluginConfId);

                // Prepare files to restore
                WorkingSubsetWrapper<IWorkingSubset> workingSubsets = storageToUse
                        .prepare(restorabledataFiles, DataStorageAccessModeEnum.RETRIEVE_MODE);
                // Check if the prepare step misses some files
                nonRestoredFiles = checkPrepareResult(restorabledataFiles, workingSubsets);
                // Scheduled restoration job
                scheduleRestorationJob(workingSubsets.getWorkingSubSets(), pluginConfId);
            } catch (ModuleException | PluginUtilsRuntimeException e) {
                LOGGER.error(e.getMessage(), e);
                nonRestoredFiles.addAll(restorabledataFiles);
            }
        }
        return nonRestoredFiles;
    }

    /**
     * Do schedule @{link RestorationJob}s for each {@link IWorkingSubset} (more specialy for
     * the associated {@link StorageDataFile}s) using the given {@link PluginConfiguration} to restore files.
     * @param workingSubsets Subsets containing {@link StorageDataFile}s to restore.
     * @param storageConfId {@link PluginConfiguration} Plugin to use to restore files.
     * @return {@link JobInfo}s of the scheduled jobs
     */
    private Set<JobInfo> scheduleRestorationJob(Set<IWorkingSubset> workingSubsets, Long storageConfId) {
        // lets instantiate every job for every DataStorage to use
        Set<JobInfo> jobs = Sets.newHashSet();
        for (IWorkingSubset workingSubset : workingSubsets) {
            // for each DataStorage we can have multiple WorkingSubSet to treat in parallel, lets storeAndCreate a job
            // for each
            // of them
            Set<JobParameter> parameters = Sets.newHashSet();
            parameters.add(new JobParameter(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, storageConfId));
            parameters.add(new JobParameter(AbstractStoreFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));
            JobInfo jobInfo = jobService
                    .createAsPending(new JobInfo(false, 0, parameters, getOwner(), RestorationJob.class.getName()));
            Path destination = Paths.get(getTenantCachePath().toString(), jobInfo.getId().toString());
            jobInfo.getParameters().add(new JobParameter(RestorationJob.DESTINATION_PATH_PARAMETER_NAME, destination));
            jobInfo.updateStatus(JobStatus.QUEUED);
            jobInfo = jobService.save(jobInfo);
            LOGGER.debug("New restoration job scheduled uuid={}", jobInfo.getId().toString());
            jobs.add(jobInfo);
        }
        return jobs;
    }

    /**
     * @param dataFilesToSubSet
     * @param workingSubSetWrapper
     * @return files contained into dataFilesToSubSet and not into workingSubSets
     */
    private Set<StorageDataFile> checkPrepareResult(Collection<StorageDataFile> dataFilesToSubSet,
            WorkingSubsetWrapper<IWorkingSubset> workingSubSetWrapper) {
        Set<StorageDataFile> result = Sets.newHashSet();
        Set<StorageDataFile> subSetDataFiles = workingSubSetWrapper.getWorkingSubSets().stream()
                .flatMap(wss -> wss.getDataFiles().stream()).collect(Collectors.toSet());
        if (subSetDataFiles.size() != dataFilesToSubSet.size()) {
            Set<StorageDataFile> notSubSetDataFiles = Sets.newHashSet(dataFilesToSubSet);
            notSubSetDataFiles.removeAll(subSetDataFiles);
            // lets check that the plugin did not forget to reject some files
            for (StorageDataFile notSubSetDataFile : notSubSetDataFiles) {
                if (!workingSubSetWrapper.getRejectedDataFiles().containsKey(notSubSetDataFile)) {
                    workingSubSetWrapper.addRejectedDataFile(notSubSetDataFile, null);
                }
            }
            Set<Map.Entry<StorageDataFile, String>> rejectedSet = workingSubSetWrapper.getRejectedDataFiles()
                    .entrySet();
            rejectedSet.stream().peek(entry -> LOGGER.error(String
                    .format("StorageDataFile %s with checksum %s could not be restored because it was not assign to a working subset by its DataStorage used to store it! Reason: %s",
                            entry.getKey().getId(), entry.getKey().getChecksum(), entry.getValue())))
                    .forEach(entry -> result.add(entry.getKey()));
        }
        return result;
    }

    private String getOwner() {
        return authResolver.getUser();
    }

    private Path getTenantCachePath() {
        String currentTenant = runtimeTenantResolver.getTenant();
        if (currentTenant == null) {
            LOGGER.error("Unable to define current tenant cache directory path, Tenant is not defined from the runtimeTenantResolver.");
            return null;
        }
        return Paths.get(globalCachePath, currentTenant);
    }
}
