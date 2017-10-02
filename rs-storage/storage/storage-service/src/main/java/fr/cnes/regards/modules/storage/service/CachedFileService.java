/*
 * LICENSE_PLACEHOLDER
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
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.dao.ICachedFileRepository;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.StorageException;
import fr.cnes.regards.modules.storage.domain.database.CachedFile;
import fr.cnes.regards.modules.storage.domain.database.CachedFileState;
import fr.cnes.regards.modules.storage.domain.database.CoupleAvailableError;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;
import fr.cnes.regards.modules.storage.domain.event.DataFileEventState;
import fr.cnes.regards.modules.storage.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.IWorkingSubset;
import fr.cnes.regards.modules.storage.service.job.AbstractStoreFilesJob;
import fr.cnes.regards.modules.storage.service.job.RestorationJob;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
public class CachedFileService implements ICachedFileService {

    private static final Logger LOG = LoggerFactory.getLogger(CachedFileService.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private ICachedFileRepository cachedFileRepository;

    @Value("${regards.storage.cache.path}")
    private String cachePath;

    @Value("${regards.storage.cache.size.limit.ko:500000000}")
    private Long maxCacheSizeKo;

    @Value("${regards.storage.cache.purge.upper.threshold.ko:450000000}")
    private Long cacheSizePurgeUpperThreshold;

    @Value("${regards.storage.cache.purge.lower.threshold.ko:400000000}")
    private Long cacheSizePurgeLowerThreshold;

    @Autowired
    private IDataFileDao dataFileDao;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobService;

    @Autowired
    private IPublisher publisher;

    @PostConstruct
    public void checkValidity() {
        // Check that the given cache storage path is available.
        File cachedPathFile = Paths.get(cachePath).toFile();
        if (!cachedPathFile.exists() || !cachedPathFile.isDirectory() || !cachedPathFile.canRead()
                || !cachedPathFile.canWrite()) {
            throw new StorageException(String
                    .format("Error initializing storage cache directory. %s is not a valid directory", cachePath));
        }
    }

    @Override
    public CoupleAvailableError restore(Set<DataFile> dataFilesToRestore, OffsetDateTime cacheExpirationDate) {
        LOG.debug("CachedFileService : run restoration process for {} files.", dataFilesToRestore.size());
        // Get files already in cache
        Set<String> dataFilesToRestoreChecksums = dataFilesToRestore.stream().map(df -> df.getChecksum())
                .collect(Collectors.toSet());
        Set<CachedFile> cachedFiles = cachedFileRepository.findAllByChecksumIn(dataFilesToRestoreChecksums);
        Set<DataFile> alreadyCachedData = dataFileDao
                .findAllByChecksumIn(cachedFiles.stream().map(cf -> cf.getChecksum()).collect(Collectors.toSet()));
        // Update expiration to the new cacheExpirationDate if above the last one.
        for (CachedFile cachedFile : cachedFiles) {
            if (cachedFile.getExpiration().compareTo(cacheExpirationDate) > 0) {
                cachedFile.setExpiration(cacheExpirationDate);
                cachedFileRepository.save(cachedFile);
            }
        }

        // Get cached files available
        Set<CachedFile> availableCachedFiles = cachedFiles.stream()
                .filter(cf -> CachedFileState.AVAILABLE.equals(cf.getState())).collect(Collectors.toSet());
        Set<DataFile> alreadyAvailableData = dataFileDao.findAllByChecksumIn(availableCachedFiles.stream()
                .map(cf -> cf.getChecksum()).collect(Collectors.toSet()));
        // Get cached files queued
        Set<CachedFile> queuedCachedFiles = cachedFiles.stream()
                .filter(cf -> CachedFileState.QUEUED.equals(cf.getState())).collect(Collectors.toSet());
        Set<DataFile> queuedData = dataFileDao.findAllByChecksumIn(queuedCachedFiles.stream()
                .map(cf -> cf.getChecksum()).collect(Collectors.toSet()));

        // Create the list of data files not handle by cache and needed to be restored
        Set<DataFile> toRetrieve = Sets.newHashSet(dataFilesToRestore);
        // Remove all files already availables in cache.
        toRetrieve.removeAll(alreadyCachedData);
        // Try to retrieve queued files if possible
        toRetrieve.addAll(queuedData);

        // Dispatch each Datafile by storage plugin.
        Multimap<PluginConfiguration, DataFile> toRetrieveByStorage = HashMultimap.create();
        for (DataFile df : toRetrieve) {
            toRetrieveByStorage.put(df.getDataStorageUsed(), df);
        }
        Set<DataFile> errors = Sets.newHashSet();
        for (PluginConfiguration storageConf : toRetrieveByStorage.keySet()) {
            scheduleDataFileRestoration(storageConf, toRetrieveByStorage.get(storageConf), cacheExpirationDate);
        }
        return new CoupleAvailableError(alreadyAvailableData, errors);
    }

    /**
     * Periodicly check the cache total size and delete expired files or/and older files if needed.
     * Default : scheduled to be run every 5minutes.
     */
    @Scheduled(fixedRateString = "${regards.cache.cleanup.rate.ms:300000}")
    public void cleanCache() {
        LOG.debug(" -----------------> Clean cache START <-----------------------");
        purgeExpiredCachedFiles();
        purgeOlderCachedFiles();
        LOG.debug(" -----------------> Clean cache END <-----------------------");
    }

    /**
     * Periodicly tries to restore all {@link CachedFile}s in {@link CachedFileState#QUEUED} status.
     * Default : scheduled to be run every 2minutes.
     */
    @Scheduled(fixedRateString = "${regards.cache.restore.queued.rate.ms:120000}")
    public void checkForCachedFilesQueuedToRestore() {
        Set<CachedFile> queuedFilesToCache = cachedFileRepository.findByState(CachedFileState.QUEUED);
        Set<String> checksums = queuedFilesToCache.stream().map(CachedFile::getChecksum).collect(Collectors.toSet());
        Set<DataFile> dataFiles = dataFileDao.findAllByChecksumIn(checksums);
        // Set an expiration date minimum of 24hours
        restore(dataFiles, OffsetDateTime.now().plusDays(1));
    }

    @Override
    public void handleRestorationSuccess(DataFile data, Path restorationPath) {
        // lets set the restorationPath to the cached file and change its state
        Optional<CachedFile> ocf = cachedFileRepository.findOneByChecksum(data.getChecksum());
        try {
            if (ocf.isPresent()) {
                CachedFile cf = ocf.get();
                cf.setLocation(restorationPath.toUri().toURL());
                cf.setState(CachedFileState.AVAILABLE);
                cf = cachedFileRepository.save(cf);
                LOG.debug("File {} is now available in cache until {}", cf.getChecksum(),
                          cf.getExpiration().toString());
                publisher.publish(new DataFileEvent(DataFileEventState.AVAILABLE, data.getChecksum()));
            } else {
                LOG.error("Restauration succeed but the file with checksum {} is not associated to any cached file is database.",
                          data.getChecksum());
            }
        } catch (MalformedURLException e) {
            // this should not happens
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleRestorationFailure(DataFile data) {
        // Delete cached file as restoraion failed.
        Optional<CachedFile> ocf = cachedFileRepository.findOneByChecksum(data.getChecksum());
        if (ocf.isPresent()) {
            CachedFile cf = ocf.get();
            cachedFileRepository.delete(cf);
            LOG.error("Error during cache file restoration {}", cf.getChecksum());
            publisher.publish(new DataFileEvent(DataFileEventState.ERROR, data.getChecksum()));
        } else {
            LOG.error("Restauration fails but the file with checksum {} is not associated to any cached file is database.",
                      data.getChecksum());
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

    /**
     * Delete all outdated {@link CachedFile}s.<br/>
     */
    private void purgeExpiredCachedFiles() {
        LOG.debug("Deleting expired files from cache. Current date : {}", OffsetDateTime.now().toString());
        deleteCachedFiles(cachedFileRepository.findByExpirationBefore(OffsetDateTime.now()));
    }

    /**
     * Delete older {@link CachedFile}s if the {link {@link #cacheSizePurgeUpperThreshold}} is reached.<br/>
     * This method method deletes as many {@link CachedFile}s as needed to set the cache size under the {@link #cacheSizePurgeLowerThreshold}.
     */
    private void purgeOlderCachedFiles() {
        // Calculate chache size
        Long cacheCurrentSize = getCacheSizeUsedOctets();
        Long cacheSizePurgeUpperThresholdInOctets = cacheSizePurgeUpperThreshold * 1024;
        Long cacheSizePurgeLowerThresholdInOctets = cacheSizePurgeLowerThreshold * 1024;
        // If cache is over upper threshold size then delete older files to reached the lower threshold.
        if ((cacheCurrentSize > cacheSizePurgeUpperThresholdInOctets)
                && (cacheSizePurgeUpperThreshold > cacheSizePurgeLowerThreshold)) {
            LOG.debug("Cache is overloaded.({}Mo) Deleting older files from cache", cacheCurrentSize / (1024 * 1024));
            Long filesTotalSizeToDelete = cacheCurrentSize - cacheSizePurgeLowerThresholdInOctets;
            Set<CachedFile> allCachedFiles = cachedFileRepository
                    .findByStateOrderByLastRequestDateAsc(CachedFileState.AVAILABLE);
            Long fileSizesSum = 0L;
            Set<CachedFile> filesToDelete = Sets.newHashSet();
            Iterator<CachedFile> it = allCachedFiles.iterator();
            while ((fileSizesSum < filesTotalSizeToDelete) && it.hasNext()) {
                CachedFile fileToDelete = it.next();
                filesToDelete.add(fileToDelete);
                fileSizesSum += fileToDelete.getFileSize();
            }
            deleteCachedFiles(filesToDelete);
        }
    }

    /**
     * Delete all given {@link CachedFile}s.<br/>
     * <ul>
     * <li> 1. Disk deletion of the physical files</li>
     * <li> 2. Database deletion of the {@link CachedFile}s
     * </ul>
     * @param filesToDelete {@link Set}<{@link CachedFile}> to delete.
     */
    private void deleteCachedFiles(Set<CachedFile> filesToDelete) {
        LOG.debug("Deleting {} files from cache.", filesToDelete.size());
        for (CachedFile cachedFile : filesToDelete) {
            if (cachedFile.getLocation() != null) {
                Path fileLocation = Paths.get(cachedFile.getLocation().getPath());
                if (fileLocation.toFile().exists()) {
                    try {
                        LOG.debug("Deletion of cached file {} (exp date={}). {}", cachedFile.getChecksum(),
                                  cachedFile.getExpiration().toString(), fileLocation);
                        Files.delete(fileLocation);
                        cachedFileRepository.delete(cachedFile);
                        LOG.debug("Cached file {} deleted (exp date={}). {}", cachedFile.getChecksum(),
                                  cachedFile.getExpiration().toString(), fileLocation);
                    } catch (NoSuchFileException e) {
                        // File does not exists, just log a warning and do delet file in db.
                        LOG.warn(e.getMessage(), e);
                        cachedFileRepository.delete(cachedFile);
                        LOG.debug("Cached file {} deleted (exp date={}).", cachedFile.getChecksum(),
                                  cachedFile.getExpiration().toString(), fileLocation);
                    } catch (IOException e) {
                        // File exists but is not deletable.
                        LOG.error(e.getMessage(), e);
                    }
                } else {
                    LOG.error("File to delete {} does not exists", fileLocation);
                    cachedFileRepository.delete(cachedFile);
                    LOG.debug("Cached file {} deleted (exp date={}). {}", cachedFile.getChecksum(),
                              cachedFile.getExpiration().toString(), fileLocation);
                }
            } else {
                cachedFileRepository.delete(cachedFile);
                LOG.debug("Cached file {} deleted (exp date={}).", cachedFile.getChecksum(),
                          cachedFile.getExpiration().toString());
            }
        }
    }

    /**
     * Caclulate the {@link DataFile}s restorable to not over the max cache size limit {@link #maxCacheSizeKo}.
     * @param dataFilesToRestore {@link DataFile}s to restore
     * @param expirationDate {@link OffsetDateTime} Expiration date of the {@link CachedFile} to restore in cache.
     * @return the {@link DataFile}s restorable
     */
    private Set<DataFile> getRestorableDataFiles(Collection<DataFile> dataFilesToRestore,
            OffsetDateTime expirationDate) {
        // 2.2 Caculate total size of files to restore
        Long totalFilesSize = dataFilesToRestore.stream().mapToLong(df -> df.getFileSize()).sum();
        Long currentCacheTotalSize = getCacheSizeUsedOctets();
        Long cacheMaxSizeInOctets = maxCacheSizeKo * 1024;
        Long availableCacheSize = cacheMaxSizeInOctets - currentCacheTotalSize;

        final Set<DataFile> restorableFiles = Sets.newHashSet();
        // Check if there is enought space left in cache to restore all files into.
        if (totalFilesSize < availableCacheSize) {
            restorableFiles.addAll(dataFilesToRestore);
        } else {
            // There is no enought space left in cache to restore all files.
            // Return maximum number of files to restore.
            Set<String> checksums = Sets.newHashSet();
            Long totalFileSizesToHandle = 0L;
            for (DataFile fileToRestore : dataFilesToRestore) {
                Long fileSize = fileToRestore.getFileSize();
                boolean fileAlreadyHandled = checksums.contains(fileToRestore.getChecksum());
                if (fileAlreadyHandled || ((totalFileSizesToHandle + fileSize) < availableCacheSize)) {
                    restorableFiles.add(fileToRestore);
                    if (!fileAlreadyHandled) {
                        checksums.add(fileToRestore.getChecksum());
                        totalFileSizesToHandle = totalFileSizesToHandle + fileSize;
                    }
                }
            }
        }

        // Initialize all files in cache
        for (DataFile dataFileToRestore : dataFilesToRestore) {
            Optional<CachedFile> ocf = cachedFileRepository.findOneByChecksum(dataFileToRestore.getChecksum());
            // If cached file already exists do not create a new one.
            if (!ocf.isPresent()) {
                CachedFileState fileState = CachedFileState.QUEUED;
                if (restorableFiles.contains(dataFileToRestore)) {
                    fileState = CachedFileState.RESTORING;
                }
                CachedFile cf = cachedFileRepository.save(new CachedFile(dataFileToRestore, expirationDate, fileState));
                LOG.debug("New file queued for cache {} exp={}", cf.getChecksum(), cf.getExpiration().toString());
            }
        }

        return restorableFiles;
    }

    /**
     * Do schedule {@link DataFile}s restoration {@link PluginConfiguration} to restore files.
     * @param pluginConf {@link PluginConfiguration} Plugin to use to restore files.
     * @param dataFilesToRestore {@link DataFile}s to restore.
     * @param expirationDate {@link OffsetDateTime} Expiration date of the {@link CachedFile} to restore in cache.
     * @return {@link DataFile}s that can not be restored.
     */
    private Set<DataFile> scheduleDataFileRestoration(PluginConfiguration pluginConf,
            Collection<DataFile> dataFilesToRestore, OffsetDateTime expirationDate) {
        LOG.debug("CachedFileService : Init restoration job for {} files.", dataFilesToRestore.size());
        Set<DataFile> restorabledataFiles = getRestorableDataFiles(dataFilesToRestore, expirationDate);
        LOG.debug("CachedFileService : Schedule restoration job for {} files.", restorabledataFiles.size());
        Set<DataFile> nonRestoredFiles = Sets.newHashSet();
        if (!restorabledataFiles.isEmpty()) {
            try {
                INearlineDataStorage<IWorkingSubset> storageToUse = pluginService.getPlugin(pluginConf.getId());

                // Prepare files to restore
                Set<IWorkingSubset> workingSubsets = storageToUse.prepare(restorabledataFiles,
                                                                          DataStorageAccessModeEnum.RETRIEVE_MODE);
                // Check if the prepare step misses some files
                nonRestoredFiles = checkPrepareResult(restorabledataFiles, workingSubsets);
                // Scheduled restoration job
                scheduleRestorationJob(workingSubsets, pluginConf);
            } catch (ModuleException e) {
                LOG.error(e.getMessage(), e);
                nonRestoredFiles.addAll(restorabledataFiles);
            }
        }
        return nonRestoredFiles;
    }

    /**
     * Do schedule @{link RestorationJob}s for each {@link IWorkingSubset} (more specialy for
     * the associated {@link DataFile}s) using the given {@link PluginConfiguration} to restore files.
     * @param workingSubsets Subsets containing {@link DataFile}s to restore.
     * @param pluginConf {@link PluginConfiguration} Plugin to use to restore files.
     * @return {@link JobInfo}s of the scheduled jobs
     */
    private Set<JobInfo> scheduleRestorationJob(Set<IWorkingSubset> workingSubsets, PluginConfiguration storageConf) {
        // lets instantiate every job for every DataStorage to use
        Set<JobInfo> jobs = Sets.newHashSet();
        for (IWorkingSubset workingSubset : workingSubsets) {
            // for each DataStorage we can have multiple WorkingSubSet to treat in parallel, lets create a job for each
            // of them
            Set<JobParameter> parameters = Sets.newHashSet();
            parameters.add(new JobParameter(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, storageConf));
            parameters.add(new JobParameter(AbstractStoreFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));
            JobInfo jobInfo = jobService
                    .createAsPending(new JobInfo(0, parameters, getOwner(), RestorationJob.class.getName()));
            Path destination = Paths.get(cachePath, runtimeTenantResolver.getTenant(), jobInfo.getId().toString());
            jobInfo.getParameters().add(new JobParameter(RestorationJob.DESTINATION_PATH_PARAMETER_NAME, destination));
            jobInfo.updateStatus(JobStatus.QUEUED);
            jobInfo = jobService.save(jobInfo);
            LOG.debug("New restoration job scheduled uuid={}", jobInfo.getId().toString());
            jobs.add(jobInfo);
        }
        return jobs;
    }

    /**
     * @param dataFilesToSubSet
     * @param workingSubSets
     * @return files contained into dataFilesToSubSet and not into workingSubSets
     */
    private Set<DataFile> checkPrepareResult(Collection<DataFile> dataFilesToSubSet,
            Set<IWorkingSubset> workingSubSets) {
        Set<DataFile> result = Sets.newHashSet();
        Set<DataFile> subSetDataFiles = workingSubSets.stream().flatMap(wss -> wss.getDataFiles().stream())
                .collect(Collectors.toSet());
        if (subSetDataFiles.size() != dataFilesToSubSet.size()) {
            Set<DataFile> notSubSetDataFiles = Sets.newHashSet(dataFilesToSubSet);
            notSubSetDataFiles.removeAll(subSetDataFiles);
            notSubSetDataFiles.stream()
                    .peek(df -> LOG.error(
                                          String.format("DataFile %s with checksum %s could not be restored because it was not assign to a working subset by its DataStorage used to store it!",
                                                        df.getId(), df.getChecksum())))
                    .forEach(result::add);
        }
        return result;
    }

    private String getOwner() {
        return authResolver.getUser();
    }
}
