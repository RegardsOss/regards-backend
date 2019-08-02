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
package fr.cnes.regards.modules.storagelight.service.file.cache;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collection;
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
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storagelight.dao.ICacheFileRepository;
import fr.cnes.regards.modules.storagelight.domain.database.CacheFile;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.exception.StorageException;
import fr.cnes.regards.modules.storagelight.domain.plugin.INearlineStorageLocation;

/**
 * Service to manage temporary accessibility of {@link FileReference} stored with a {@link INearlineStorageLocation}
 * plugin.<br/>
 * When a file is requested by {@link #restore} method this service retrieve the file from the
 * nearline datastorage plugin and copy it into his internal cache (Local disk)<br/>
 * As the cache maximum size is limited, this service queues the file requests and handle them when it is possible<br/>
 *
 * Files in cache are purged when :
 * <ul>
 * <li>Files are outdated in cache ({@link CacheFile#getExpirationDate()} date is past.</li>
 * <li>Cache is full and no outdated files are in cache, then the older {@link CacheFile}s are deleted.</li>
 * </ul>
 *
 *
 * @author Sylvain VISSIERE-GUERINET
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class CacheService implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheService.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    @Lazy
    private CacheService self;

    /**
     * {@link ICacheFileRepository} instance
     */
    @Autowired
    private ICacheFileRepository cachedFileRepository;

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

    /**
     * Creates a new cache file if the checksum does not match an existing file.
     * If file already exists in cache, updates the associated information.
     * @param checksum
     * @param fileSize
     * @param location
     * @param expirationDate
     * @throws EntityAlreadyExistsException
     */
    public void addFile(String checksum, Long fileSize, URL location, OffsetDateTime expirationDate) {
        Optional<CacheFile> oCf = search(checksum);
        CacheFile cachedFile;
        if (!oCf.isPresent()) {
            cachedFile = new CacheFile(checksum, fileSize, location, expirationDate);
        } else {
            cachedFile = oCf.get();
            if (expirationDate.isAfter(cachedFile.getExpirationDate())) {
                cachedFile.setExpirationDate(expirationDate);
            }
            cachedFile.setFileSize(fileSize);
        }
        cachedFileRepository.save(cachedFile);
    }

    /**
     * Search for a file in cache with the given checksum.
     * @param checksum
     * @return {@link CacheFile}
     */
    public Optional<CacheFile> search(String checksum) {
        return cachedFileRepository.findOneByChecksum(checksum);
    }

    private void checkDiskDBCoherence(String tenant) throws IOException {
        runtimeTenantResolver.forceTenant(tenant);
        Page<CacheFile> shouldBeAvailableSet;
        Pageable page = PageRequest.of(0, filesIterationLimit, Direction.ASC, "id");
        do {
            shouldBeAvailableSet = cachedFileRepository.findAll(page);
            for (CacheFile shouldBeAvailable : shouldBeAvailableSet) {
                if (Files.notExists(Paths.get(shouldBeAvailable.getLocation().getPath()))) {
                    cachedFileRepository.delete(shouldBeAvailable);
                }
            }
            page = page.next();
        } while (shouldBeAvailableSet.hasNext());

        page = PageRequest.of(0, filesIterationLimit, Direction.ASC, "id");
        Page<CacheFile> availableFiles;
        do {
            availableFiles = cachedFileRepository.findAll(page);
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

    public Optional<CacheFile> getAvailable(FileReference fileReference) {
        Optional<CacheFile> ocf = cachedFileRepository.findOneByChecksum(fileReference.getMetaInfo().getChecksum());
        if (ocf.isPresent()) {
            return ocf;
        } else {
            return Optional.empty();
        }
    }

    public Set<FileReference> getAvailables(Set<FileReference> fileReferences) {
        Set<FileReference> availables = Sets.newHashSet();
        Set<String> checksums = fileReferences.stream().map(f -> f.getMetaInfo().getChecksum())
                .collect(Collectors.toSet());
        Set<CacheFile> cacheFiles = cachedFileRepository.findAllByChecksumIn(checksums);
        Set<String> cacheFileChecksums = cacheFiles.stream().map(f -> f.getChecksum()).collect(Collectors.toSet());
        for (FileReference f : fileReferences) {
            if (cacheFileChecksums.contains(f.getMetaInfo().getChecksum())) {
                availables.add(f);
            }
        }
        return availables;
    }

    /**
     * Return the current size of the cache in bytes.
     * @return {@link Long}
     */
    public Long getCacheSizeUsedBytes() {
        return cachedFileRepository.getTotalFileSize();
    }

    public int purge() {
        return purgeExpiredCachedFiles();
    }

    /**
     * Delete all out dated {@link CacheFile}s.<br/>
     */
    private int purgeExpiredCachedFiles() {
        int nbPurged = 0;
        LOGGER.debug("Deleting expired files from cache. Current date : {}", OffsetDateTime.now().toString());
        Pageable page = PageRequest.of(0, filesIterationLimit, Direction.ASC, "id");
        Page<CacheFile> files;
        do {
            files = cachedFileRepository.findByExpirationDateBefore(OffsetDateTime.now(), page);
            deleteCachedFiles(files.getContent());
            page = files.nextPageable();
            nbPurged = nbPurged + files.getNumberOfElements();
        } while (files.hasNext());
        return nbPurged;
    }

    /**
     * Delete all given {@link CacheFile}s.<br/>
     * <ul>
     * <li>1. Disk deletion of the physical files</li>
     * <li>2. Database deletion of the {@link CacheFile}s
     * </ul>
     * @param filesToDelete {@link Set}<{@link CacheFile}> to delete.
     */
    private void deleteCachedFiles(Collection<CacheFile> filesToDelete) {
        LOGGER.debug("Deleting {} files from cache.", filesToDelete.size());
        for (CacheFile cachedFile : filesToDelete) {
            if (cachedFile.getLocation() != null) {
                Path fileLocation = Paths.get(cachedFile.getLocation().getPath());
                if (fileLocation.toFile().exists()) {
                    try {
                        LOGGER.debug("Deletion of cached file {} (exp date={}). {}", cachedFile.getChecksum(),
                                     cachedFile.getExpirationDate().toString(), fileLocation);
                        Files.delete(fileLocation);
                        cachedFileRepository.delete(cachedFile);
                        LOGGER.debug("Cached file {} deleted (exp date={}). {}", cachedFile.getChecksum(),
                                     cachedFile.getExpirationDate().toString(), fileLocation);
                    } catch (NoSuchFileException e) {
                        // File does not exists, just log a warning and do delet file in db.
                        LOGGER.warn(e.getMessage(), e);
                        cachedFileRepository.delete(cachedFile);
                        LOGGER.debug("Cached file {} deleted (exp date={}).", cachedFile.getChecksum(),
                                     cachedFile.getExpirationDate().toString(), fileLocation);
                    } catch (IOException e) {
                        // File exists but is not deletable.
                        LOGGER.error(e.getMessage(), e);
                    }
                } else {
                    LOGGER.error("File to delete {} does not exists", fileLocation);
                    cachedFileRepository.delete(cachedFile);
                    LOGGER.debug("Cached file {} deleted (exp date={}). {}", cachedFile.getChecksum(),
                                 cachedFile.getExpirationDate().toString(), fileLocation);
                }
            } else {
                cachedFileRepository.delete(cachedFile);
                LOGGER.debug("Cached file {} deleted (exp date={}).", cachedFile.getChecksum(),
                             cachedFile.getExpirationDate().toString());
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

    /**
     * Calculate a file path in the cache system by creating a sub folder for each 2 character of its checksum.
     * @param fileChecksum
     * @return file path
     */
    public String getFilePath(String fileChecksum) {
        String filePath = "";
        int idx = 0;
        int subFolders = 0;
        while ((idx < fileChecksum.length()) && (subFolders < 6)) {
            filePath = Paths.get(filePath, fileChecksum.substring(idx, idx + 2)).toString();
            idx = idx + 2;
        }
        return Paths.get(getTenantCachePath().toString(), filePath, fileChecksum).toAbsolutePath().toString();
    }

    public Long getCacheAvailableSizeBytes() {
        Long currentCacheTotalSize = getCacheSizeUsedBytes();
        Long cacheMaxSizeInOctets = maxCacheSizeKo * 1024;
        return cacheMaxSizeInOctets - currentCacheTotalSize;
    }

}
