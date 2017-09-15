package fr.cnes.regards.modules.storage.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.SecurityUtils;
import fr.cnes.regards.modules.storage.dao.ICachedFileRepository;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.database.CachedFile;
import fr.cnes.regards.modules.storage.domain.database.CoupleAvailableError;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.IWorkingSubset;
import fr.cnes.regards.modules.storage.service.job.AbstractStoreFilesJob;
import fr.cnes.regards.modules.storage.service.job.RestorationJob;
import fr.cnes.regards.plugins.utils.PluginUtilsRuntimeException;

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

    @Autowired
    private IDataFileDao dataFileDao;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IJobInfoService jobService;

    @Override
    public CoupleAvailableError restore(Set<DataFile> nearlineFiles, OffsetDateTime expirationDate) {
        //first of all, lets get the files that are already in cache
        Set<String> nearlineFileChecksums = nearlineFiles.stream().map(df -> df.getChecksum())
                .collect(Collectors.toSet());
        Set<CachedFile> alreadyAvailable = cachedFileRepository.findAllByChecksumIn(nearlineFileChecksums);
        //now lets update the expiration date
        for (CachedFile cachedFile : alreadyAvailable) {
            if (cachedFile.getExpiration().compareTo(expirationDate) > 0) {
                cachedFile.setExpiration(expirationDate);
                cachedFileRepository.save(cachedFile);
            }
        }
        // lets get the remaining files
        Set<DataFile> alreadyAvailableData = dataFileDao
                .findAllByChecksumIn(alreadyAvailable.stream().map(cf -> cf.getChecksum()).collect(Collectors.toSet()));
        Set<DataFile> toRetrieve = Sets.newHashSet(nearlineFiles);
        toRetrieve.removeAll(alreadyAvailableData);
        // now that we know what should be retrieved from the data storage, lets regroup the DataFiles by data storage
        Multimap<PluginConfiguration, DataFile> toRetrieveByStorage = HashMultimap.create();
        for (DataFile df : toRetrieve) {
            toRetrieveByStorage.put(df.getDataStorageUsed(), df);
        }
        Set<DataFile> errors = Sets.newHashSet();
        for (PluginConfiguration storageConf : toRetrieveByStorage.keySet()) {
            INearlineDataStorage storageToUse = null;
            Collection<DataFile> toRetrieveFiles = toRetrieveByStorage.get(storageConf);
            try {
                storageToUse = pluginService.getPlugin(storageConf.getId());
            } catch (ModuleException | PluginUtilsRuntimeException e) {
                LOG.error(e.getMessage(), e);
                errors.addAll(toRetrieveFiles);
                //TODO: notify
            }
            Set<IWorkingSubset> workingSubsets = storageToUse
                    .prepare(toRetrieveFiles, DataStorageAccessModeEnum.RETRIEVE_MODE);
            // now lets build the restoration job for each working subset
            scheduleRestorationJob(workingSubsets, storageConf);
        }
        return new CoupleAvailableError(alreadyAvailableData, errors);
    }

    private void scheduleRestorationJob(Set<IWorkingSubset> workingSubsets, PluginConfiguration storageConf) {

        //lets instantiate every job for every DataStorage to use
        for (IWorkingSubset workingSubset : workingSubsets) {
            //for each DataStorage we can have multiple WorkingSubSet to treat in parallel, lets create a job for each of them
            Set<JobParameter> parameters = Sets.newHashSet();
            parameters.add(new JobParameter(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, storageConf));
            parameters.add(new JobParameter(AbstractStoreFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));
            Path destination = Paths.get(cachePath, runtimeTenantResolver.getTenant());
            parameters.add(new JobParameter(RestorationJob.DESTINATION_PATH_PARAMETER_NAME, destination));
            jobService.createAsQueued(new JobInfo(0, parameters, getOwner(), RestorationJob.class.getName()));
        }

    }

    private String getOwner() {
        return SecurityUtils.getActualUser();
    }
}
