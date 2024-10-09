/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.filecatalog.dto.StorageLocationDto;
import fr.cnes.regards.modules.fileaccess.dto.StorageType;
import fr.cnes.regards.modules.fileaccess.plugin.domain.INearlineStorageLocation;
import fr.cnes.regards.modules.storage.dao.ICacheFileRepository;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import fr.cnes.regards.modules.storage.domain.database.CacheFile;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.service.StorageJobsPriority;
import fr.cnes.regards.modules.storage.service.cache.job.CacheCleanJob;
import org.apache.commons.lang3.StringUtils;
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

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service to manage temporary accessibility of {@link FileReference} stored with a {@link INearlineStorageLocation}
 * plugin.<br/>
 * When a file is requested by restore method this service retrieve the file from the
 * nearline datastorage plugin and copy it into its internal cache (Local disk) or into its external cache.<br/>
 * As the internal cache maximum size is limited, this service queues the file requests and handle them when it is
 * possible<br/>
 * <p>
 * Files in internal cache are purged when :
 * <ul>
 * <li>Files are outdated in cache ({@link CacheFile#getExpirationDate()} date is past.</li>
 * <li>Cache is full and no outdated files are in cache, then the older {@link CacheFile}s are deleted.</li>
 * </ul>
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
    private int BULK_SIZE;

    @Autowired
    private ICacheFileRepository cacheFileRepository;

    @Autowired
    private IDynamicTenantSettingService dynamicTenantSettingService;

    @Autowired
    private IJobInfoService jobService;

    /**
     * Creates a new cache file in database if the checksum does not match an existing file.
     * If file already exists in internal or external cache, updates the associated information in database.
     */
    public void addFile(String checksum,
                        Long fileSize,
                        String fileName,
                        MimeType mimeType,
                        String type,
                        URL location,
                        OffsetDateTime expirationDate,
                        Set<String> groupIds,
                        @Nullable String externalCachePlugin) {
        Optional<CacheFile> cacheFileOptional = findByChecksum(checksum);
        CacheFile cacheFile;
        if (!cacheFileOptional.isPresent()) {
            // To be created in database
            cacheFile = createCacheFile(checksum,
                                        fileSize,
                                        fileName,
                                        mimeType,
                                        type,
                                        location,
                                        expirationDate,
                                        groupIds,
                                        externalCachePlugin);
        } else {
            // To be updated in database
            cacheFile = cacheFileOptional.get();
            if (expirationDate.isAfter(cacheFile.getExpirationDate())) {
                cacheFile.setExpirationDate(expirationDate);
            }
            cacheFile.setFileSize(fileSize);
        }
        cacheFileRepository.save(cacheFile);
    }

    /**
     * Creates a new cache file (internal cache or external cache).
     */
    private CacheFile createCacheFile(String checksum,
                                      Long fileSize,
                                      String fileName,
                                      MimeType mimeType,
                                      String type,
                                      URL location,
                                      OffsetDateTime expirationDate,
                                      Set<String> groupIds,
                                      @Nullable String externalCachePlugin) {
        if (StringUtils.isBlank(externalCachePlugin)) {
            // Internal cache
            return CacheFile.buildFileInternalCache(checksum,
                                                    fileSize,
                                                    fileName,
                                                    mimeType,
                                                    location,
                                                    expirationDate,
                                                    groupIds,
                                                    type);
        } else {
            // External cache
            return CacheFile.buildFileExternalCache(checksum,
                                                    fileSize,
                                                    fileName,
                                                    mimeType,
                                                    location,
                                                    expirationDate,
                                                    groupIds,
                                                    type,
                                                    externalCachePlugin);
        }
    }

    /**
     * Retrieve a file from the cache by its checksum.
     *
     * @return {@link CacheFile}
     */
    @MultitenantTransactional(readOnly = true)
    public Optional<CacheFile> findByChecksum(String checksum) {
        return cacheFileRepository.findOneByChecksum(checksum);
    }

    /**
     * Check coherence between database and physical files in internal cache.
     * If physical file does not exist in internal cache, delete entity in database.
     */
    public void checkDiskDBCoherence() {
        Page<CacheFile> shouldBeAvailableSet;
        Pageable page = PageRequest.of(0, BULK_SIZE, Direction.ASC, "id");
        Set<Long> toDelete = new HashSet<>();

        do {
            shouldBeAvailableSet = cacheFileRepository.findAllByInternalCacheTrue(page);
            for (CacheFile shouldBeAvailable : shouldBeAvailableSet) {
                Path path = Paths.get(shouldBeAvailable.getLocation().getPath());
                if (Files.notExists(path)) {
                    LOGGER.warn("Dirty internal cache file in database : {}", path);
                    toDelete.add(shouldBeAvailable.getId());
                }
            }
            if (toDelete.size() > 10_000) {
                // Do deletion and restart and page 0
                cacheFileRepository.deleteAllById(toDelete);
                toDelete.clear();
                page = PageRequest.of(0, BULK_SIZE, Direction.ASC, "id");
            } else {
                page = page.next();
            }
        } while (shouldBeAvailableSet.hasNext());
        cacheFileRepository.deleteAllById(toDelete);
    }

    /**
     * Retrieve a list of file from the cache by their checksums.
     *
     * @return {@link CacheFile}
     */
    public Set<CacheFile> getCacheFiles(Set<String> checksums) {
        return cacheFileRepository.findAllByChecksumIn(checksums);
    }

    /**
     * Retrieve all {@link FileReference}s available in cache. Cache file groupIds list is updated if the file exists.
     *
     * @param groupId new availability request business identifier. This id is added to the already existing cache files.
     * @return {@link FileReference}s available
     */
    public Map<FileReference, CacheFile> getAndUpdateFileCacheIfExists(Set<FileReference> fileReferences,
                                                                       String groupId) {
        Map<FileReference, CacheFile> availables = new HashMap<>();
        Set<String> checksums = fileReferences.stream()
                                              .map(f -> f.getMetaInfo().getChecksum())
                                              .collect(Collectors.toSet());
        Set<CacheFile> cacheFiles = cacheFileRepository.findAllByChecksumIn(checksums);
        Map<String, CacheFile> cacheFilesByChecksum = cacheFiles.stream().peek(cf -> {
            // Add new request id to the cache file
            cf.addGroupId(groupId);
            cacheFileRepository.save(cf);
        }).collect(Collectors.toMap(CacheFile::getChecksum, Function.identity()));
        for (FileReference f : fileReferences) {
            if (cacheFilesByChecksum.containsKey(f.getMetaInfo().getChecksum())) {
                availables.put(f, cacheFilesByChecksum.get(f.getMetaInfo().getChecksum()));
            }
        }
        return availables;
    }

    /**
     * Delete files in database from cache :
     * <ul>
     *     <li>If force mode is true, so all files are deleted in the internal and external cache.</li>
     *     <li>If force mode is false, so all out dated files are deleted in the internal cache.</li>
     * </ul>
     */
    public int purge(boolean forceMode) {
        int nbPurged = 0;
        if (forceMode) {
            LOGGER.debug("Deleting all (force mode activated) files from cache. Current date : {}",
                         OffsetDateTime.now());
        } else {
            LOGGER.debug("Deleting expired files from cache. Current date : {}", OffsetDateTime.now());
        }
        Pageable page = PageRequest.of(0, BULK_SIZE, Direction.ASC, "id");
        Page<CacheFile> files;
        do {
            if (forceMode) {
                files = cacheFileRepository.findAll(page);
            } else {
                files = cacheFileRepository.findByExpirationDateBeforeAndInternalCacheTrue(OffsetDateTime.now(), page);
            }
            deleteCachedFiles(files.getContent());
            nbPurged = nbPurged + files.getNumberOfElements();
        } while (files.hasNext());
        return nbPurged;
    }

    /**
     * Delete all given {@link CacheFile}s.<br/>
     * <ul>
     * <li>1. Disk deletion of the physical files in internal cache</li>
     * <li>2. Database deletion of the {@link CacheFile}s
     * </ul>
     *
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
                    cacheFileRepository.delete(cachedFile);
                    LOGGER.debug(" [CACHE FILE DELETION SUCCESS] Cached file {} deleted (exp date={}). {}",
                                 cachedFile.getChecksum(),
                                 cachedFile.getExpirationDate().toString(),
                                 fileLocation);
                } catch (NoSuchFileException e) {
                    // File does not exists, just log a warning and do delete file in db.
                    LOGGER.warn(e.getMessage(), e);
                    cacheFileRepository.delete(cachedFile);
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
                cacheFileRepository.delete(cachedFile);
                LOGGER.debug("[CACHE FILE DELETION SUCCESS] Cached file {} deleted (exp date={}). {}",
                             cachedFile.getChecksum(),
                             cachedFile.getExpirationDate().toString(),
                             fileLocation);
            }
        } else {
            cacheFileRepository.delete(cachedFile);
            LOGGER.debug("[CACHE FILE DELETION SUCCESS] Cached file {} deleted (exp date={}).",
                         cachedFile.getChecksum(),
                         cachedFile.getExpirationDate().toString());
        }
    }

    /**
     * Retrieve the path of the internal cache for te curent tenant.
     */
    public Path getTenantCachePath() {
        return dynamicTenantSettingService.read(StorageSetting.CACHE_PATH_NAME)
                                          .map(settingDto -> (Path) settingDto.getValue())
                                          .orElseThrow(() -> new RsRuntimeException(
                                              "Tenant cache path has not been initialized"));
    }

    /**
     * Calculate a file path in the internal cache system by creating a sub folder for each 2 character of its checksum.
     *
     * @return file path
     */
    public String getFilePath(String fileChecksum) {
        return Paths.get(getCacheDirectoryPath(fileChecksum), fileChecksum).toString();
    }

    /**
     * Calculate a file path in the internal cache system by creating a sub folder for each 2 character of its checksum.
     *
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
     * Return the current size of the used internal cache in Bytes.
     */
    @MultitenantTransactional(readOnly = true)
    public Long getCacheSizeUsedBytes() {
        return cacheFileRepository.getTotalFileSizeInternalCache();
    }

    /**
     * Return the current size of the used internal cache in Kilo-bytes.
     */
    public Long getCacheSizeUsedKBytes() {
        return getCacheSizeUsedBytes() / 1024;
    }

    /**
     * Return the free space of the current tenant internal cache in Bytes.
     */
    public Long getFreeSpaceInBytes() {
        return getMaxCacheSizeBytes() - getCacheSizeUsedBytes();
    }

    /**
     * Return the limit size of internal cache in Bytes.
     */
    public Long getMaxCacheSizeBytes() {
        return getMaxCacheSizeKo() * 1024;
    }

    /**
     * Return the maximum size of internal cache in Kilo-octets.
     */
    private Long getMaxCacheSizeKo() {
        return dynamicTenantSettingService.read(StorageSetting.CACHE_MAX_SIZE_NAME)
                                          .map(settingDto -> (Long) settingDto.getValue())
                                          .orElseThrow(() -> new RsRuntimeException(
                                              "Max internal cache size setting has not been initialized"));
    }

    /**
     * Build the storage location of internal cache :
     * <ul>
     *     <li>this location allows to physically delete files</li>
     *     <li>the number of files stored into internal cache</li>
     *     <li>the size of used internal cache in Kilo-bytes</li>
     * </ul>
     */
    public StorageLocationDto buildStorageLocation() {
        StorageLocationConfiguration conf = new StorageLocationConfiguration(CACHE_NAME, null, getMaxCacheSizeKo());
        conf.setStorageType(StorageType.CACHE);

        return StorageLocationDto.build(CACHE_NAME, conf.toDto())
                                 .withAllowPhysicalDeletion()
                                 .withFilesInformation(cacheFileRepository.countCacheFileByInternalCacheTrue(),
                                                       0,
                                                       getCacheSizeUsedKBytes());
    }

    /**
     * Is the internal cache empty ?
     */
    @MultitenantTransactional(readOnly = true)
    public boolean isCacheEmpty() {
        return cacheFileRepository.countCacheFileByInternalCacheTrue() == 0;
    }

    public void scheduleCacheCleanUp(String jobOwner, boolean forceDelete) {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(CacheCleanJob.FORCE_PARAMETER_NAME, forceDelete));
        if (jobService.retrieveJobsCount(CacheCleanJob.class.getName(),
                                         JobStatus.PENDING,
                                         JobStatus.RUNNING,
                                         JobStatus.QUEUED,
                                         JobStatus.TO_BE_RUN) == 0) {
            jobService.createAsQueued(new JobInfo(false,
                                                  StorageJobsPriority.CACHE_PURGE,
                                                  parameters,
                                                  jobOwner,
                                                  CacheCleanJob.class.getName()));
        }
    }
}
