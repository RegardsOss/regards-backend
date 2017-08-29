/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.file.utils.ChecksumUtils;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatusInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.SecurityUtils;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.DataObject;
import fr.cnes.regards.modules.storage.domain.FileType;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.domain.event.AIPValid;
import fr.cnes.regards.modules.storage.plugin.IWorkingSubset;
import fr.cnes.regards.modules.storage.service.job.StoreDataFilesJob;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Service
public class AIPService implements IAIPService {

    private final IAIPDao dao;

    private final IPublisher publisher;

    private final DataStorageManager storageManager;

    private PluginService pluginService;

    private IJobInfoService jobInfoService;

    private ITenantResolver runtimeTenantResolver;

    private Gson gson;

    public AIPService(IAIPDao dao, IPublisher pPublisher, DataStorageManager pStorageManager,
            PluginService pluginService) {
        this.dao = dao;
        publisher = pPublisher;
        storageManager = pStorageManager;
        this.pluginService = pluginService;
    }

    /**
     * AIP has been validated by Controller REST. Validation of an AIP is only to check if network has not corrupted
     * informations.(There is another validation point when each file is stocked as file are only downloaded by
     * asynchronous task)
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Override
    public Set<UUID> create(Set<AIP> aips) throws ModuleException {
        // save into DB as valid
        //TODO: check with rs-ingest if ipIds are already set or not
        Set<AIP> aipsInDb = Sets.newHashSet();
        for (AIP aip : aips) {
            aip.setState(AIPState.VALID);
            aipsInDb.add(dao.save(aip));
            // Publish AIP_VALID
            publisher.publish(new AIPValid(aip));
        }
        // Lets retrieve active configurations of IAllocationStrategy
        List<PluginConfiguration> allocationStrategies = pluginService
                .getPluginConfigurationsByType(IAllocationStrategy.class);
        List<PluginConfiguration> activeAllocationStrategies = allocationStrategies.stream().filter(pc -> pc.isActive())
                .collect(Collectors.toList());
        // System can only handle one active configuration of IAllocationStrategy
        if (activeAllocationStrategies.size() != 1) {
            IllegalStateException e = new IllegalStateException(
                    "The application needs one and only one active configuration of " + IAllocationStrategy.class
                            .getName());
            throw e;
        }
        IAllocationStrategy allocationStrategy = pluginService.getPlugin(activeAllocationStrategies.get(0));
        // now lets ask to the strategy to dispatch dataFiles into WorkingSet for each possible DataStorage
        Set<DataFile> dataFilesToHandle = Sets.newHashSet();
        aipsInDb.forEach(aip -> dataFilesToHandle.addAll(DataFile.extractDataFiles(aip)));
        Multimap<PluginConfiguration, IWorkingSubset> storageWorkingSetMap = allocationStrategy
                .dispatch(dataFilesToHandle);

        Set<JobInfo> jobsToSchedule = Sets.newHashSet();
        for (PluginConfiguration dataStorageConf : storageWorkingSetMap.keySet()) {
            //lets instantiate every job for every DataStorage to use
            for (IWorkingSubset workingSubset : storageWorkingSetMap.get(dataStorageConf)) {
                //for each DataStorage we can have multiple WorkingSubSet to treat in parallel, lets create a job for each of them
                Set<JobParameter> parameters = Sets.newHashSet();
                parameters.add(new JobParameter(StoreDataFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, dataStorageConf));
                parameters.add(new JobParameter(StoreDataFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));
                jobsToSchedule.add(new JobInfo(0, parameters, getOwner(), StoreDataFilesJob.class.getName(),
                                               new JobStatusInfo()));
            }
        }

        // scheduleJob for files just give to the job the AIP ipId or id
        Set<UUID> jobIds = null;
        for (JobInfo job : jobsToSchedule) {
            jobIds.add(jobInfoService.create(job).getId());
        }
        // change the state to PENDING
        for (AIP aip : aipsInDb) {
            aip.setState(AIPState.PENDING);
            dao.save(aip);
        }

        return jobIds;
        // stockage du descriptif sur fichier
        // TODO: job synchrone?
        // ouverture du flux sur le Workspace
        // ecriture sur workspace
    }

    /**
     * Schedule to be executed every minute after the end of the last invocation.
     * Waiting one minute in the worst case to eventually store the aip metadata seems acceptable and it might allow us to treat multiple aip metadata
     */
    @Scheduled(fixedDelay = 60000)
    public void storeMetadata() {
        //first lets get AIP that are not fully stored(at least metadata are not stored)
        Set<AIP> notFullyStored = dao.findAllByStateService(AIPState.PENDING);
        Set<DataFile> metadataToStore = Sets.newHashSet();
        for (AIP aip : notFullyStored) {
//            Set<DataFile> storedDataFile = dataFileDao.findAllByStateAndByAip(DataFileState.STORED, aip);
//            if (storedDataFile.containsAll(DataFile.extractDataFiles(aip))) {
//                // that means all DataFile of this AIP has been stored, lets prepare the metadata storage
//                // first we need to right the metadata into a file
//                String toWrite = gson.toJson(aip);
//                String checksum = ChecksumUtils.getHexChecksum(
//                        MessageDigest.getInstance("MD5").digest(toWrite.getBytes(StandardCharsets.UTF_8)));
//                BufferedWriter writer = Files.newBufferedWriter(Paths.get(workspace, checksum + ".json"));
//                writer.write(toWrite);
//                writer.flush();
//                writer.close();
//                //lets check the storage
//                DigestInputStream dis = new DigestInputStream(Files.newInputStream(Paths.get(workspace)),
//                                                              MessageDigest.getInstance("MD5"));
//                while (dis.read() != -1) {
//                }
//                String fileChecksum = ChecksumUtils.getHexChecksum(dis.getMessageDigest().digest());
//                // then we create a DataFile with originUrl set to the created file
//                DataFile meta = new DataFile(new URL("file", "localhost", Paths.get(workspace, checksum + ".json")),
//                                             "MD5", DataType.AIP, checksum, size, new MimeType("application", "json"));
//                metadataToStore.add(meta);
//            }
        }
        //now that we know all the metadata that should be stored, lets schedule their storage!
    }

    private String getOwner() {
        return SecurityUtils.getActualUser();
    }

    /**
     * two {@link OffsetDateTime} are here considered equals to the second
     */
    @Override
    public Page<AIP> retrieveAIPs(AIPState pState, OffsetDateTime pFrom, OffsetDateTime pTo,
            Pageable pPageable) { // NOSONAR
        if (pState != null) {
            if (pFrom != null) {
                if (pTo != null) {
                    return dao.findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(pState, pFrom.minusNanos(1),
                                                                                          pTo.plusSeconds(1),
                                                                                          pPageable);
                }
                return dao.findAllByStateAndSubmissionDateAfter(pState, pFrom.minusNanos(1), pPageable);
            }
            if (pTo != null) {
                return dao.findAllByStateAndLastEventDateBefore(pState, pTo.plusSeconds(1), pPageable);
            }
            return dao.findAllByState(pState, pPageable);
        }
        if (pFrom != null) {
            if (pTo != null) {
                return dao.findAllBySubmissionDateAfterAndLastEventDateBefore(pFrom.minusNanos(1), pTo.plusSeconds(1),
                                                                              pPageable);
            }
            return dao.findAllBySubmissionDateAfter(pFrom.minusNanos(1), pPageable);
        }
        if (pTo != null) {
            return dao.findAllByLastEventDateBefore(pTo.plusSeconds(1), pPageable);
        }
        //        return dao.findAll(pPageable);
        return null;
    }

    @Override
    public List<DataObject> retrieveAIPFiles(UniformResourceName pIpId) throws EntityNotFoundException {
        //        AIP aip = dao.findOneByIpIdWithDataObjects(pIpId.toString());
        //        if (aip == null) {
        //            throw new EntityNotFoundException(pIpId.toString(), AIP.class);
        //        }
        //        return aip.getDataObjects();
        return null;
    }

    @Override
    public List<String> retrieveAIPVersionHistory(UniformResourceName pIpId) {
        String ipIdWithoutVersion = pIpId.toString();
        ipIdWithoutVersion = ipIdWithoutVersion.substring(0, ipIdWithoutVersion.indexOf(":V"));
        Set<AIP> versions = dao.findAllByIpIdStartingWith(ipIdWithoutVersion);
        return versions.stream().map(a -> a.getIpId()).collect(Collectors.toList());
    }

}
