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
package fr.cnes.regards.modules.storage.service.cache;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.storage.dao.ICacheFileRepository;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import fr.cnes.regards.modules.storage.domain.database.CacheFile;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineStorageLocation;
import fr.cnes.regards.modules.storage.domain.plugin.StorageType;
import fr.cnes.regards.modules.storage.service.StorageJobsPriority;
import fr.cnes.regards.modules.storage.service.cache.job.CacheCleanJob;

/**
 * Service to manage temporary accessibility of {@link FileReference} stored with a {@link INearlineStorageLocation}
 * plugin.<br/>
 * When a file is requested by restore method this service retrieve the file from the
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
public class CacheService {

    public static final String CACHE_NAME = "internal-cache";

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheService.class);

    @Value("${regards.storage.cache.schedule.purge.bulk.size:500}")
    private final int BULK_SIZE = 500;

    @Autowired
    private ICacheFileRepository cachedFileRepository;

    @Autowired
    private IDynamicTenantSettingService dynamicTenantSettingService;

    @Autowired
    private IJobInfoService jobService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Creates a new cache file if the checksum does not match an existing file.
     * If file already exists in cache, updates the associated information.
     */
    public void addFile(String checksum, Long fileSize, String fileName, MimeType mimeType, String type, URL location,
            OffsetDateTime expirationDate, String groupId) {
        Optional<CacheFile> oCf = search(checksum);
        CacheFile cachedFile;
        if (!oCf.isPresent()) {
            cachedFile = new CacheFile(checksum, fileSize, fileName, mimeType, location, expirationDate, groupId, type);
        } else {
            cachedFile = oCf.get();
            if (expirationDate.isAfter(cachedFile.getExpirationDate())) {
                cachedFile.setExpirationDate(expirationDate);
            }
            cachedFile.setFileSize(fileSize);
        }
        cachedFileRepository.save(cachedFile);
    }

    public Optional<CacheFile> search(String checksum) {
        return cachedFileRepository.findOneByChecksum(checksum);
    }

    /**
     * Check coherence between database and physical files in cache location.
     */
    public void checkDiskDBCoherence() {
        Page<CacheFile> shouldBeAvailableSet;
        Pageable page = PageRequest.of(0, BULK_SIZE, Direction.ASC, "id");
        Set<Long> toDelete = Sets.newHashSet();
        do {
            shouldBeAvailableSet = cachedFileRepository.findAll(page);
            for (CacheFile shouldBeAvailable : shouldBeAvailableSet) {
                Path path = Paths.get(shouldBeAvailable.getLocation().getPath());
                if (Files.notExists(path)) {
                    LOGGER.warn("Dirty cache file in database : {}", path);
                    toDelete.add(shouldBeAvailable.getId());
                }
            }
            if (toDelete.size() > 10_000) {
                // Do deletion and restart and page 0
                toDelete.forEach(id -> cachedFileRepository.deleteById(id));
                toDelete.clear();
                page = PageRequest.of(0, BULK_SIZE, Direction.ASC, "id");
            } else {
                page = page.next();
            }
        } while (shouldBeAvailableSet.hasNext());
        toDelete.forEach(id -> cachedFileRepository.deleteById(id));
    }

    /**
     * Retrieve a file from the cache by is checksum.
     * @return {@link CacheFile}
     */
    public Optional<CacheFile> getCacheFile(String checksum) {
        return cachedFileRepository.findOneByChecksum(checksum);
    }

    /**
     * Retrieve all {@link FileReference}s available in cache.
     * @param groupId new availability request business identifier. This id is added to the already existing cache files.
     * @return {@link FileReference}s available
     */
    public Set<FileReference> getFilesAvailableInCache(Set<FileReference> fileReferences, String groupId) {
        Set<FileReference> availables = Sets.newHashSet();
        Set<String> checksums = fileReferences.stream().map(f -> f.getMetaInfo().getChecksum())
                .collect(Collectors.toSet());
        Set<CacheFile> cacheFiles = cachedFileRepository.findAllByChecksumIn(checksums);
        Set<String> cacheFileChecksums = cacheFiles.stream().map(cf -> {
            // Add new request id to the cache file
            cf.addGroupId(groupId);
            cachedFileRepository.save(cf);
            return cf.getChecksum();
        }).collect(Collectors.toSet());
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

    public Long getCacheSizeUsedKB() {
        return cachedFileRepository.getTotalFileSize() / 1024;
    }

    /**
     * Delete all out dated {@link CacheFile}s.<br/>
     */
    public int purge() {
        int nbPurged = 0;
        LOGGER.debug("Deleting expired files from cache. Current date : {}", OffsetDateTime.now());
        Pageable page = PageRequest.of(0, BULK_SIZE, Direction.ASC, "id");
        Page<CacheFile> files;
        do {
            files = cachedFileRepository.findByExpirationDateBefore(OffsetDateTime.now(), page);
            deleteCachedFiles(files.getContent());
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
    public void deleteCachedFiles(Collection<CacheFile> filesToDelete) {
        LOGGER.debug("Deleting {} files from cache.", filesToDelete.size());
        filesToDelete.forEach(this::delete);
    }

    public void delete(CacheFile cachedFile) {
        if (cachedFile.getLocation() != null) {
            Path fileLocation = Paths.get(cachedFile.getLocation().getPath());
            if (fileLocation.toFile().exists()) {
                try {
                    LOGGER.trace("Deletion of cached file {} (exp date={}). {}",
                                 cachedFile.getChecksum(),
                                 cachedFile.getExpirationDate().toString(),
                                 fileLocation);
                    Files.delete(fileLocation);
                    cachedFileRepository.delete(cachedFile);
                    LOGGER.debug(" [CACHE FILE DELETION SUCCESS] Cached file {} deleted (exp date={}). {}",
                                 cachedFile.getChecksum(),
                                 cachedFile.getExpirationDate().toString(),
                                 fileLocation);
                } catch (NoSuchFileException e) {
                    // File does not exists, just log a warning and do delete file in db.
                    LOGGER.warn(e.getMessage(), e);
                    cachedFileRepository.delete(cachedFile);
                    LOGGER.debug("[CACHE FILE DELETION SUCCESS] Cached file {} deleted (exp date={}). {}",
                                 cachedFile.getChecksum(),
                                 cachedFile.getExpirationDate().toString(),
                                 fileLocation);
                } catch (IOException e) {
                    // File exists but is not deletable.
                    LOGGER.error(e.getMessage(), e);
                }
            } else {
                LOGGER.error("File to delete {} does not exists", fileLocation);
                cachedFileRepository.delete(cachedFile);
                LOGGER.debug("[CACHE FILE DELETION SUCCESS] Cached file {} deleted (exp date={}). {}",
                             cachedFile.getChecksum(),
                             cachedFile.getExpirationDate().toString(),
                             fileLocation);
            }
        } else {
            cachedFileRepository.delete(cachedFile);
            LOGGER.debug("[CACHE FILE DELETION SUCCESS] Cached file {} deleted (exp date={}).",
                         cachedFile.getChecksum(),
                         cachedFile.getExpirationDate().toString());
        }
    }

    /**
     * Retrieve the path of the cache for te curent tenant.
     */
    public Path getTenantCachePath() {
        return dynamicTenantSettingService.read(StorageSetting.CACHE_PATH_NAME)
                .orElseThrow(() -> new RsRuntimeException("Tenant cache path has not been initialized")).getValue();
    }

    /**
     * Calculate a file path in the cache system by creating a sub folder for each 2 character of its checksum.
     * @return file path
     */
    public String getFilePath(String fileChecksum) {
        return Paths.get(getCacheDirectoryPath(fileChecksum), fileChecksum).toString();
    }

    /**
     * Calculate a file path in the cache system by creating a sub folder for each 2 character of its checksum.
     * @return file path
     */
    public String getCacheDirectoryPath(String fileChecksum) {
        String filePath = "";
        int idx = 0;
        int subFolders = 0;
        while ((idx < (fileChecksum.length() - 1)) && (subFolders < 3)) {
            filePath = Paths.get(filePath, fileChecksum.substring(idx, idx + 2)).toString();
            idx = idx + 2;
            subFolders++;
        }
        return Paths.get(getTenantCachePath().toString(), filePath).toAbsolutePath().toString();
    }

    /**
     * Return the free space in Bytes of the current tenant cache.
     */
    public Long getFreeSpaceInBytes() {
        Long currentCacheTotalSize = getCacheSizeUsedBytes();
        Long cacheMaxSizeInOctets = getMaxCacheSizeKo() * 1024;
        return cacheMaxSizeInOctets - currentCacheTotalSize;
    }

    public Long getCacheSizeLimit() {
        return getMaxCacheSizeKo() * 1024;
    }

    private Long getMaxCacheSizeKo() {
        return dynamicTenantSettingService.read(StorageSetting.CACHE_MAX_SIZE_NAME)
                .orElseThrow(() -> new RsRuntimeException("Max cache size setting has not been initialized"))
                .getValue();
    }

    public long getTotalCachedFiles() {
        return cachedFileRepository.count();
    }

    public StorageLocationDTO toStorageLocation() {
        StorageLocationConfiguration conf = new StorageLocationConfiguration(CACHE_NAME, null, getMaxCacheSizeKo());
        conf.setStorageType(StorageType.CACHE);
        return new StorageLocationDTO(CACHE_NAME,
                                      getTotalCachedFiles(),
                                      getCacheSizeUsedKB(),
                                      0L,
                                      0L,
                                      false,
                                      false,
                                      false,
                                      conf,
                                      true);
    }

    public boolean isCacheEmpty() {
        return cachedFileRepository.count() == 0;
    }

    public void scheduleCacheCleanUp(String jobOwner) {
        Set<JobParameter> parameters = Sets.newHashSet();
        if (jobService.retrieveJobsCount(CacheCleanJob.class.getName(), JobStatus.PENDING, JobStatus.RUNNING,
                                         JobStatus.QUEUED, JobStatus.TO_BE_RUN) == 0) {
            jobService.createAsQueued(new JobInfo(false, StorageJobsPriority.CACHE_PURGE, parameters,
                                                  jobOwner, CacheCleanJob.class.getName()));
        }
    }
}
