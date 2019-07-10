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
package fr.cnes.regards.modules.storagelight.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Iterator;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storagelight.dao.ICachedFileRepository;
import fr.cnes.regards.modules.storagelight.domain.database.CachedFile;
import fr.cnes.regards.modules.storagelight.domain.database.CachedFileState;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.exception.StorageException;

/**
 * Service to manage temporary accessibility of {@link FileReference} stored with a {@link INearlineDataStorage}
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
public class CachedFileService implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedFileService.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    @Lazy
    private CachedFileService self;

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

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)

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

    public Optional<CachedFile> getAvailableCachedFile(String pChecksum) {
        Optional<CachedFile> ocf = cachedFileRepository.findOneByChecksum(pChecksum);
        if (ocf.isPresent() && CachedFileState.AVAILABLE.equals(ocf.get().getState())) {
            return ocf;
        } else {
            return Optional.empty();
        }
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
        if ((cacheCurrentSize > cacheSizePurgeUpperThresholdInOctets)
                && (cacheSizePurgeUpperThreshold > cacheSizePurgeLowerThreshold)) {
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
                    while ((fileSizesSum < filesTotalSizeToDelete) && it.hasNext()) {
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

    private Path getTenantCachePath() {
        String currentTenant = runtimeTenantResolver.getTenant();
        if (currentTenant == null) {
            LOGGER.error("Unable to define current tenant cache directory path, Tenant is not defined from the runtimeTenantResolver.");
            return null;
        }
        return Paths.get(globalCachePath, currentTenant);
    }
}
