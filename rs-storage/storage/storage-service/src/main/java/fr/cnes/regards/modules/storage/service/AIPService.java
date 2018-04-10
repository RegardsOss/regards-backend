/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.microservice.maintenance.MaintenanceException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.PreservationDescriptionInformation;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.notification.domain.NotificationType;
import fr.cnes.regards.modules.notification.domain.dto.NotificationDTO;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.AipDataFiles;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.CoupleAvailableError;
import fr.cnes.regards.modules.storage.domain.FileCorruptedException;
import fr.cnes.regards.modules.storage.domain.RejectedAip;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.domain.database.CachedFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.domain.database.DataStorageType;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.event.StorageAction;
import fr.cnes.regards.modules.storage.domain.event.StorageEventType;
import fr.cnes.regards.modules.storage.domain.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.domain.plugin.IAllocationStrategy;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.ISecurityDelegation;
import fr.cnes.regards.modules.storage.domain.plugin.IWorkingSubset;
import fr.cnes.regards.modules.storage.domain.plugin.WorkingSubsetWrapper;
import fr.cnes.regards.modules.storage.service.job.AbstractStoreFilesJob;
import fr.cnes.regards.modules.storage.service.job.DeleteDataFilesJob;
import fr.cnes.regards.modules.storage.service.job.StoreDataFilesJob;
import fr.cnes.regards.modules.storage.service.job.StoreMetadataFilesJob;
import fr.cnes.regards.modules.storage.service.job.UpdateDataFilesJob;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import fr.cnes.regards.modules.templates.service.TemplateServiceConfiguration;

/**
 * Service to handle {@link AIP} and associated {@link StorageDataFile}s entities from all data straoge systems.<br/>
 * An {@link AIP} can be associated to many {@link StorageDataFile}s but only one of type {@link DataType#AIP}.<br/>
 * Available data storage systems are defined by the available {@link IDataStorage} plugins<br/>
 * Stored files can be stored with :
 * <ul>
 * <li>Online data storage plugins {@link IOnlineDataStorage} : Files are directly accessible for download</li>
 * <li>Nearline data storage plugins {@link INearlineDataStorage} : Files needs to be cached before download</li>
 * </ul>
 *
 * At startup, this service subscribe to all {@link DataStorageEvent}s to handle physical actions
 * (storeAndCreate, retrieve and deletion) on {@link StorageDataFile}s.<br/>
 * See {@link DataStorageEventHandler} class to understand more about actions done on physical files changes.<br/>
 * <br/>
 * This service also run scheduled actions :
 * <ul>
 * <li>{@link #storeMetadata} : This cron action executed every minutes handle
 * update of {@link AIP} state by looking for all associated {@link StorageDataFile} states.
 * An {@link AIP} is STORED when all his {@link StorageDataFile}s are STORED</li>
 * </ul>
 * <br/>
 * The cache system to make nearline files accessible is handled by the {@link ICachedFileService}.<br/>
 * @author Sylvain Vissiere-Guerinet
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class AIPService implements IAIPService, ApplicationListener<ApplicationReadyEvent> {

    /**
     * JSON files extension.
     */
    public static final String JSON_FILE_EXT = ".json";

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AIPService.class);

    /**
     * Forbidden aip access message
     */
    private static final String AIP_ACCESS_FORBIDDEN = "You do not have suffisent access right to get this aip.";

    /**
     * DAO to access {@link AIP} entities through the {@link AIPEntity} entities stored in db.
     */
    @Autowired
    private IAIPDao aipDao;

    /**
     * DAO to access {@link StorageDataFile} entities.
     */
    @Autowired
    private IDataFileDao dataFileDao;

    /**
     * AMQP Publisher.
     */
    @Autowired
    private IPublisher publisher;

    /**
     * AMQP Subscriber.
     */
    @Autowired
    private ISubscriber subscriber;

    /**
     * Service to retrieve and use Plugins more specificly the {@link IDataStorage} plugins.
     */
    @Autowired
    private IPluginService pluginService;

    /**
     * The AIP service uses JOBS to run asynchronous storeAndCreate actions.
     */
    @Autowired
    private IJobInfoService jobInfoService;

    /**
     * Resolver to know all existing tenants of the current REGARDS instance.
     */
    @Autowired
    private ITenantResolver tenantResolver;

    /**
     * {@link IRuntimeTenantResolver} instance
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * {@link IAuthenticationResolver} instance
     */
    @Autowired
    private IAuthenticationResolver authResolver;

    /**
     * {@link Gson} instance
     */
    @Autowired
    private Gson gson;

    /**
     * to get transactionnality inside scheduled
     */
    @Autowired
    private IAIPService self;

    /**
     * Service to manage avaibility of nearline files.
     */
    @Autowired
    private ICachedFileService cachedFileService;

    /**
     * Handler to manage {@link DataStorageEvent} events.
     */
    @Autowired
    private DataStorageEventHandler dataStorageEventHandler;

    /**
     * {@link Validator} instance
     */
    @Autowired
    private Validator validator;

    /**
     * {@link ITemplateService} instance
     */
    @Autowired
    private ITemplateService templateService;

    /**
     * {@link INotificationClient} instance
     */
    @Autowired
    private INotificationClient notificationClient;

    /**
     * {@link IWorkspaceService} instance
     */
    @Autowired
    private IWorkspaceService workspaceService;

    /**
     * The spring application name ~= microservice type
     */
    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private IPrioritizedDataStorageService prioritizedDataStorageService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override

    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Subscribe to events on {@link StorageDataFile} changes.
        subscriber.subscribeTo(DataStorageEvent.class, dataStorageEventHandler);
    }

    @PostConstruct
    public void init() {
        pluginService.addPluginPackage("fr.cnes.regards.modules.storage");
    }
    
    @Override
    public Set<UUID> storeAndCreate(Set<AIP> aips) throws ModuleException {
        LOG.trace("Entering method storeAndCreate(Set<AIP>) with {} aips", aips.size());
        Set<AIP> aipsInDb = Sets.newHashSet();
        Set<StorageDataFile> dataFilesToStore = Sets.newHashSet();
        // 1. Create each AIP into database with VALID state.
        // 2. Create each StorageDataFile of each AIP into database with PENDING state.
        for (AIP aip : aips) {
            // Can not create an existing AIP.
            aip.setState(AIPState.VALID);
            aip.addEvent(EventType.SUBMISSION.name(), "Submission to REGARDS");
            aipsInDb.add(aipDao.save(aip));
            Collection<StorageDataFile> dataFiles = dataFileDao.save(StorageDataFile.extractDataFiles(aip));
            dataFiles.forEach(df -> df.setState(DataFileState.PENDING));
            dataFilesToStore.addAll(dataFiles);
            // Notify system for new VALID AIP created.
            publisher.publish(new AIPEvent(aip));
        }
        LOG.trace("{} aips built {} data objects to storeAndCreate", aips.size(), dataFilesToStore.size());
        // change the state to PENDING
        for (AIP aip : aipsInDb) {
            aip.setState(AIPState.PENDING);
            aipDao.save(aip);
            // Notify system for AIP updated to PENDING state.
            publisher.publish(new AIPEvent(aip));
        }
        return store(dataFilesToStore);
    }

    /**
     * Actually run the storage logic: dispatching data files between data storages and scheduling the storage jobs.
     * @return job ids scheduled for the storage
     */
    protected Set<UUID> store(Set<StorageDataFile> dataFilesToStore) throws ModuleException {
        Multimap<Long, StorageDataFile> storageWorkingSetMap = dispatchAndCheck(dataFilesToStore);
        return scheduleStorage(storageWorkingSetMap, true);
    }

    /**
     * Dispatch given dataFilesToStore between {@link IDataStorage}s thanks to the active {@link IAllocationStrategy}
     * and check they all have been dispatched
     * @param dataFilesToStore {@link StorageDataFile} to store
     * @return dispatched {@link StorageDataFile}
     * @throws ModuleException
     */
    private Multimap<Long, StorageDataFile> dispatchAndCheck(Set<StorageDataFile> dataFilesToStore)
            throws ModuleException {
        IAllocationStrategy allocationStrategy = getAllocationStrategy();
        // Now lets ask to the strategy to dispatch dataFiles between possible DataStorages
        Multimap<Long, StorageDataFile> storageWorkingSetMap = allocationStrategy.dispatch(dataFilesToStore);
        LOG.trace("{} data objects has been dispatched between {} data storage by allocation strategy",
                  dataFilesToStore.size(), storageWorkingSetMap.keySet().size());
        // as we are trusty people, we check that the dispatch gave us back all DataFiles into the WorkingSubSets
        checkDispatch(dataFilesToStore, storageWorkingSetMap);
        // now that those who should be in error are handled, lets save into DB those to be stored (mainly because of
        // "notYetStoredBy")
        dataFileDao.save(storageWorkingSetMap.values());
        return storageWorkingSetMap;
    }

    @Override
    public Set<UUID> storeRetry(Set<String> aipIpIds) throws ModuleException {
        // lets get the data file which are in storage error state and ask for their storage, once again
        Set<AIP> failedAips = aipDao.findAllByIpIdIn(aipIpIds);
        Set<StorageDataFile> failedDataFiles = dataFileDao.findAllByStateAndAipIn(DataFileState.ERROR, failedAips);
        return store(failedDataFiles);
    }

    @Override
    public AvailabilityResponse loadFiles(AvailabilityRequest availabilityRequest) throws ModuleException {
        Set<String> requestedChecksums = availabilityRequest.getChecksums();
        Set<StorageDataFile> dataFiles = dataFileDao.findAllByChecksumIn(requestedChecksums);
        Set<String> errors = Sets.newHashSet();

        // 1. Check for invalid files.
        if (dataFiles.size() != requestedChecksums.size()) {
            Set<String> dataFilesChecksums = dataFiles.stream().map(df -> df.getChecksum()).collect(Collectors.toSet());
            Set<String> checksumNotFound = Sets.difference(requestedChecksums, dataFilesChecksums);
            errors.addAll(checksumNotFound);
            checksumNotFound.stream()
                    .forEach(cs -> LOG.error("File to restore with checksum {} is not stored by REGARDS.", cs));
        }

        Set<StorageDataFile> dataFilesWithAccess = checkLoadFilesAccessRights(dataFiles);

        errors.addAll(Sets.difference(dataFiles, dataFilesWithAccess).stream().map(df -> df.getChecksum())
                .collect(Collectors.toSet()));

        Set<StorageDataFile> onlineFiles = Sets.newHashSet();
        Set<StorageDataFile> nearlineFiles = Sets.newHashSet();

        // 2. Check for online files. Online files doesn't need to be stored in the cache
        // they can be accessed directly where they are stored.
        for (StorageDataFile df : dataFilesWithAccess) {
            if (df.getPrioritizedDataStorages() != null) {
                Optional<PrioritizedDataStorage> onlinePrioritizedDataStorageOpt = df.getPrioritizedDataStorages()
                        .stream().filter(pds -> pds.getDataStorageType().equals(DataStorageType.ONLINE)).findFirst();
                if (onlinePrioritizedDataStorageOpt.isPresent()) {
                    onlineFiles.add(df);
                } else {
                    nearlineFiles.add(df);
                }
            } else {
                LOG.error("File to restore {} has no storage plugin information. Restoration failed.", df.getId());
            }
        }
        // now lets ask the cache service to handle nearline restoration and give us the already available ones
        CoupleAvailableError nearlineAvailableAndError = cachedFileService
                .restore(nearlineFiles, availabilityRequest.getExpirationDate());
        for (StorageDataFile inError : nearlineAvailableAndError.getErrors()) {
            errors.add(inError.getChecksum());
        }
        // lets construct the result
        return new AvailabilityResponse(errors, onlineFiles, nearlineAvailableAndError.getAvailables());
    }

    private Set<StorageDataFile> checkLoadFilesAccessRights(Set<StorageDataFile> dataFiles) throws ModuleException {
        Set<StorageDataFile> dataFilesWithAccess = Sets.newHashSet(dataFiles);
        for (StorageDataFile df : dataFiles) {
            AIP aip = df.getAip();
            if (!getSecurityDelegationPlugin().hasAccess(aip.getId().toString())) {
                dataFilesWithAccess.remove(df);
            }
        }
        return dataFilesWithAccess;
    }

    @Override
    public Page<AIP> retrieveAIPs(AIPState pState, OffsetDateTime pFrom, OffsetDateTime pTo, Pageable pPageable)
            throws ModuleException {
        if (!getSecurityDelegationPlugin().hasAccessToListFeature()) {
            throw new EntityOperationForbiddenException("Only Admins can access this feature.");
        }
        if (pState != null) {
            if (pFrom != null) {
                if (pTo != null) {
                    return aipDao
                            .findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(pState, pFrom.minusNanos(1),
                                                                                        pTo.plusSeconds(1), pPageable);
                }
                return aipDao.findAllByStateAndSubmissionDateAfter(pState, pFrom.minusNanos(1), pPageable);
            }
            if (pTo != null) {
                return aipDao.findAllByStateAndLastEventDateBefore(pState, pTo.plusSeconds(1), pPageable);
            }
            return aipDao.findAllByState(pState, pPageable);
        }
        if (pFrom != null) {
            if (pTo != null) {
                return aipDao.findAllBySubmissionDateAfterAndLastEventDateBefore(pFrom.minusNanos(1),
                                                                                 pTo.plusSeconds(1), pPageable);
            }
            return aipDao.findAllBySubmissionDateAfter(pFrom.minusNanos(1), pPageable);
        }
        if (pTo != null) {
            return aipDao.findAllByLastEventDateBefore(pTo.plusSeconds(1), pPageable);
        }
        return aipDao.findAll(pPageable);
    }

    @Override
    public Page<AipDataFiles> retrieveAipDataFiles(AIPState state, Set<String> tags, OffsetDateTime fromLastUpdateDate,
            Pageable pageable) {
        // first lets get the page of aips
        // we have two cases: there is a date or not
        Page<AIP> aips;
        if (fromLastUpdateDate == null) {
            if(tags == null || tags.isEmpty()) {
                aips = aipDao.findAllByState(state, pageable);
            } else {
                aips = aipDao.findAllByStateAndTagsIn(state, tags, pageable);
            }
        } else {
            if(tags == null || tags.isEmpty()) {
                aips = aipDao.findAllByStateAndLastEventDateAfter(state, fromLastUpdateDate, pageable);
            } else {
                aips = aipDao.findAllByStateAndTagsInAndLastEventDateAfter(state, tags, fromLastUpdateDate, pageable);
            }
        }
        // now lets get the data files for each aip which are the aip metadata itself
        List<AipDataFiles> aipDataFiles = new ArrayList<>();
        for (AIP aip : aips.getContent()) {
            Set<StorageDataFile> dataFiles = dataFileDao.findAllByAip(aip);
            // lets eliminate the metadata data file
            dataFiles = dataFiles.stream().filter(dataFile -> dataFile.getDataType() != DataType.AIP)
                    .collect(Collectors.toSet());
            aipDataFiles.add(new AipDataFiles(aip, dataFiles.toArray(new StorageDataFile[dataFiles.size()])));
        }
        return new PageImpl<>(aipDataFiles, pageable, aips.getTotalElements());
    }

    @Override
    public Set<OAISDataObject> retrieveAIPFiles(UniformResourceName pIpId) throws ModuleException {
        Optional<AIP> aip = aipDao.findOneByIpId(pIpId.toString());
        if (aip.isPresent()) {
            if (!getSecurityDelegationPlugin().hasAccess(pIpId.toString())) {
                throw new EntityOperationForbiddenException(pIpId.toString(), AIP.class, AIP_ACCESS_FORBIDDEN);
            }
            Set<StorageDataFile> dataFiles = dataFileDao.findAllByAip(aip.get());
            return dataFiles.stream().map(df -> {
                OAISDataObject dataObject = new OAISDataObject();
                dataObject.setRegardsDataType(df.getDataType());
                dataObject.setUrls(df.getUrls());
                dataObject.setFilename(df.getName());
                dataObject.setFileSize(df.getFileSize());
                dataObject.setChecksum(df.getChecksum());
                dataObject.setAlgorithm(df.getAlgorithm());
                return dataObject;
            }).collect(Collectors.toSet());
        } else {
            throw new EntityNotFoundException(pIpId.toString(), AIP.class);
        }
    }

    @Override
    public List<String> retrieveAIPVersionHistory(UniformResourceName pIpId) {
        String ipIdWithoutVersion = pIpId.toString();
        ipIdWithoutVersion = ipIdWithoutVersion.substring(0, ipIdWithoutVersion.indexOf(":V"));
        Set<AIP> versions = aipDao.findAllByIpIdStartingWith(ipIdWithoutVersion);
        return versions.stream().map(a -> a.getId().toString()).collect(Collectors.toList());
    }

    /**
     * Check that all given {@link StorageDataFile}s are dispatch into the given {@link Multimap}.<br/>
     * If it's true, nothing is done.<br/>
     * If not, the associated {@link AIP}s of given {@link StorageDataFile}s are set to {@link AIPState#STORAGE_ERROR}
     * status.
     * @param dataFilesToStore {@link StorageDataFile}s
     * @param storageWorkingSetMap {@link Multimap}<{@link PluginConfiguration}, {@link StorageDataFile}>
     */
    private void checkDispatch(Set<StorageDataFile> dataFilesToStore,
            Multimap<Long, StorageDataFile> storageWorkingSetMap) {
        Set<StorageDataFile> dataFilesInSubSet = storageWorkingSetMap.entries().stream().map(entry -> entry.getValue())
                .collect(Collectors.toSet());
        if (dataFilesToStore.size() != dataFilesInSubSet.size()) {
            Set<StorageDataFile> notSubSetDataFiles = Sets.newHashSet(dataFilesToStore);
            notSubSetDataFiles.removeAll(dataFilesInSubSet);
            for (StorageDataFile prepareFailed : notSubSetDataFiles) {
                prepareFailed.setState(DataFileState.ERROR);
                AIP aip = prepareFailed.getAip();
                aip.setState(AIPState.STORAGE_ERROR);
                dataFileDao.save(prepareFailed);
                aipDao.save(aip);
                publisher.publish(new AIPEvent(aip));
            }
            // lets prepare the notification message
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("dataFiles", notSubSetDataFiles);
            dataMap.put("allocationStrategy", getAllocationStrategyConfiguration());
            // lets use the template service to get our message
            SimpleMailMessage email;
            try {
                email = templateService.writeToEmail(TemplateServiceConfiguration.NOT_DISPATCHED_DATA_FILES_CODE,
                                                     dataMap);
            } catch (EntityNotFoundException e) {
                throw new MaintenanceException(e.getMessage(), e);
            }
            notifyAdmins("Some file were not associated to a data storage", email.getText(), NotificationType.ERROR);
        }
    }

    /**
     * Use the notification module in admin to create a notification for admins
     */
    private void notifyAdmins(String title, String message, NotificationType type) {
        NotificationDTO notif = new NotificationDTO(message, Sets.newHashSet(),
                Sets.newHashSet(DefaultRole.ADMIN.name()), applicationName, title, type);
        notificationClient.createNotification(notif);
    }

    /**
     * This method schedules {@link StoreDataFilesJob} or {@link StoreMetadataFilesJob} to storeAndCreate given
     * {@link StorageDataFile}s.<br/>
     * A Job is scheduled for each {@link IWorkingSubset} of each {@link PluginConfiguration}.<br/>
     * @param storageWorkingSetMap List of {@link StorageDataFile} to storeAndCreate per {@link PluginConfiguration}.
     * @param storingData FALSE to storeAndCreate {@link DataType#AIP}, or TRUE for all other type of
     *            {@link StorageDataFile}.
     * @return List of {@link UUID} of jobs scheduled.
     */
    public Set<UUID> scheduleStorage(Multimap<Long, StorageDataFile> storageWorkingSetMap, boolean storingData)
            throws ModuleException {
        Set<UUID> jobIds = Sets.newHashSet();
        for (Long dataStorageConfId : storageWorkingSetMap.keySet()) {
            Set<IWorkingSubset> workingSubSets = getWorkingSubsets(storageWorkingSetMap.get(dataStorageConfId),
                                                                   dataStorageConfId,
                                                                   DataStorageAccessModeEnum.STORE_MODE);
            LOG.trace("Preparing a job for each working subsets");
            // lets instantiate every job for every DataStorage to use
            for (IWorkingSubset workingSubset : workingSubSets) {
                // for each DataStorage we can have multiple WorkingSubSet to treat in parallel, lets storeAndCreate a
                // job for
                // each of them
                Set<JobParameter> parameters = Sets.newHashSet();
                parameters.add(new JobParameter(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, dataStorageConfId));
                parameters.add(new JobParameter(AbstractStoreFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));
                if (storingData) {
                    jobIds.add(jobInfoService.createAsQueued(new JobInfo(false, 0, parameters, authResolver.getUser(),
                            StoreDataFilesJob.class.getName())).getId());
                } else {
                    jobIds.add(jobInfoService.createAsQueued(new JobInfo(false, 0, parameters, authResolver.getUser(),
                            StoreMetadataFilesJob.class.getName())).getId());
                }

            }
        }
        // now that files are given to the jobs, lets remove the source url so once stored we only have the good urls
        Collection<StorageDataFile> storageDataFiles = storageWorkingSetMap.values();
        storageDataFiles.forEach(file -> file.setUrls(new HashSet<>()));
        dataFileDao.save(storageDataFiles);
        return jobIds;
    }

    /**
     * Call the {@link IDataStorage} plugins associated to the given {@link PluginConfiguration}s to create
     * {@link IWorkingSubset} of {@link StorageDataFile}s.
     * @param dataFilesToSubSet List of {@link StorageDataFile} to prepare.
     * @param dataStorageConfId {@link PluginConfiguration}
     * @param accessMode
     * @return {@link IWorkingSubset}s
     */
    protected Set<IWorkingSubset> getWorkingSubsets(Collection<StorageDataFile> dataFilesToSubSet,
            Long dataStorageConfId, DataStorageAccessModeEnum accessMode) throws ModuleException {
        IDataStorage<IWorkingSubset> storage = pluginService.getPlugin(dataStorageConfId);
        LOG.trace("Getting working subsets for data storage of id {}", dataStorageConfId);
        WorkingSubsetWrapper workingSubsetWrapper = storage.prepare(dataFilesToSubSet, accessMode);
        Set<IWorkingSubset> workingSubSets = workingSubsetWrapper.getWorkingSubSets();
        LOG.trace("{} data objects were dispatched into {} working subsets", dataFilesToSubSet.size(),
                  workingSubSets.size());
        // as we are trusty people, we check that the prepare gave us back all DataFiles into the WorkingSubSets
        Set<StorageDataFile> subSetDataFiles = workingSubSets.stream().flatMap(wss -> wss.getDataFiles().stream())
                .collect(Collectors.toSet());
        if (subSetDataFiles.size() != dataFilesToSubSet.size()) {
            Set<StorageDataFile> notSubSetDataFiles = Sets.newHashSet(dataFilesToSubSet);
            notSubSetDataFiles.removeAll(subSetDataFiles);
            // lets check that the plugin did not forget to reject some files
            for (StorageDataFile notSubSetDataFile : notSubSetDataFiles) {
                if (!workingSubsetWrapper.getRejectedDataFiles().containsKey(notSubSetDataFile)) {
                    workingSubsetWrapper.addRejectedDataFile(notSubSetDataFile, null);
                }
            }
            Set<Map.Entry<StorageDataFile, String>> rejectedSet = workingSubsetWrapper.getRejectedDataFiles()
                    .entrySet();
            for (Map.Entry<StorageDataFile, String> rejected : rejectedSet) {
                rejected.getKey().setState(DataFileState.ERROR);
                AIP aip = rejected.getKey().getAip();
                aip.setState(AIPState.STORAGE_ERROR);
                dataFileDao.save(rejected.getKey());
                aipDao.save(aip);
                publisher.publish(new AIPEvent(aip));
            }
            // lets prepare the notification message
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("dataFilesMap", workingSubsetWrapper.getRejectedDataFiles());
            // lets use the template service to get our message
            SimpleMailMessage email;
            try {
                email = templateService.writeToEmail(TemplateServiceConfiguration.NOT_SUBSETTED_DATA_FILES_CODE,
                                                     dataMap);
            } catch (EntityNotFoundException e) {
                throw new MaintenanceException(e.getMessage(), e);
            }
            notifyAdmins("Some file were not handled by a data storage", email.getText(), NotificationType.ERROR);
        }
        return workingSubSets;
    }

    /**
     * Retrieve the only one activated allocation strategy {@link IAllocationStrategy} plugin.
     * @return {@link IAllocationStrategy}
     * @throws ModuleException if many {@link IAllocationStrategy} are active.
     */
    private IAllocationStrategy getAllocationStrategy() throws ModuleException {
        PluginConfiguration activeAllocationStrategy = getAllocationStrategyConfiguration();
        try {
            return pluginService.getPlugin(activeAllocationStrategy.getId());
        } catch (PluginUtilsRuntimeException e) {
            LOG.error(e.getMessage(), e);
            notifyAdmins("Allocation Strategy miss configured", e.getMessage(), NotificationType.ERROR);
            throw e;
        }
    }

    private PluginConfiguration getAllocationStrategyConfiguration() {
        // Lets retrieve active configurations of IAllocationStrategy
        List<PluginConfiguration> allocationStrategies = pluginService
                .getPluginConfigurationsByType(IAllocationStrategy.class);
        List<PluginConfiguration> activeAllocationStrategies = allocationStrategies.stream().filter(pc -> pc.isActive())
                .collect(Collectors.toList());
        // System can only handle one active configuration of IAllocationStrategy
        if (activeAllocationStrategies.size() != 1) {
            IllegalStateException e = new IllegalStateException(
                    "The application needs one and only one active configuration of "
                            + IAllocationStrategy.class.getName());
            notifyAdmins("No active Allocation Strategy", e.getMessage(), NotificationType.ERROR);
            LOG.error(e.getMessage(), e);
            throw e;
        }
        return activeAllocationStrategies.get(0);
    }

    private ISecurityDelegation getSecurityDelegationPlugin() throws ModuleException {
        // Lets retrieve active configurations of IAllocationStrategy
        List<PluginConfiguration> securityDelegations = pluginService
                .getPluginConfigurationsByType(ISecurityDelegation.class);
        List<PluginConfiguration> activeSecurityDelegations = securityDelegations.stream().filter(pc -> pc.isActive())
                .collect(Collectors.toList());
        // System can only handle one active configuration of IAllocationStrategy
        if (activeSecurityDelegations.size() != 1) {
            IllegalStateException e = new IllegalStateException(
                    "The application needs one and only one active configuration of "
                            + ISecurityDelegation.class.getName());
            LOG.error(e.getMessage(), e);
            throw e;
        }
        return pluginService.getPlugin(activeSecurityDelegations.get(0).getId());
    }

    @Override
    public void scheduleStorageMetadata(Set<StorageDataFile> metadataToStore) {
        try {
            Multimap<Long, StorageDataFile> storageWorkingSetMap = dispatchAndCheck(metadataToStore);
            scheduleStorage(storageWorkingSetMap, false);
            // to avoid making jobs for the same metadata all the time, lets change the metadataToStore AIP state to
            // STORING_METADATA
            for (StorageDataFile dataFile : metadataToStore) {
                AIP aip = dataFile.getAip();
                aip.setState(AIPState.STORING_METADATA);
                aipDao.save(aip);
                publisher.publish(new AIPEvent(aip));
            }
        } catch (ModuleException e) {
            LOG.error(e.getMessage(), e);
            notifyAdmins("Could not schedule metadata storage",
                         "Metadata storage could not be realized because an error occured. Please check the logs",
                         NotificationType.ERROR);
        }
    }

    /**
     * This cron action executed every minutes handle update of {@link AIP} state by
     * looking for all associated {@link StorageDataFile} states. An {@link AIP} is STORED
     * when all his {@link StorageDataFile}s are STORED.
     */
    @Scheduled(fixedDelayString = "${regards.storage.check.aip.metadata.delay:60000}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void storeMetadata() {
        LOG.debug(" ------------------------> Update AIP storage informations - START<---------------------------- ");
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            Set<StorageDataFile> metadataToStore = Sets.newHashSet();
            // first lets get AIP that are not fully stored(at least metadata are not stored)
            metadataToStore.addAll(self.prepareNotFullyStored());
            if (metadataToStore.isEmpty()) {
                LOG.debug("No updated metadata files to storeAndCreate.");
            } else {
                LOG.debug("Scheduling {} updated metadata files for storage.", metadataToStore.size());
                // now that we know all the metadata that should be stored, lets schedule their storage!
                self.scheduleStorageMetadata(metadataToStore);
            }
        }
        LOG.debug(" ------------------------> Update AIP storage informations - END <---------------------------- ");
    }

    @Override
    public Set<StorageDataFile> prepareNotFullyStored() {
        Set<StorageDataFile> metadataToStore = Sets.newHashSet();
        Set<AIP> notFullyStored = aipDao.findAllByStateInService(AIPState.PENDING);
        // first lets handle the case where every dataFiles of an AIP are successfully stored.
        for (AIP aip : notFullyStored) {
            Set<StorageDataFile> storedDataFile = dataFileDao.findAllByStateAndAip(DataFileState.STORED, aip);
            if (storedDataFile.containsAll(StorageDataFile.extractDataFiles(aip))) {
                // that means all StorageDataFile of this AIP has been stored, lets prepare the metadata storage,
                // first we need to write the metadata into a file
                StorageDataFile meta;
                try {
                    meta = writeMetaToWorkspace(aip);
                    // now if we have a meta to store, lets add it
                    meta.setState(DataFileState.PENDING);
                    dataFileDao.save(meta);
                    metadataToStore.add(meta);
                } catch (IOException | FileCorruptedException e) {
                    // if we don't have a meta to storeAndCreate that means a problem happened and we set the aip to
                    // STORAGE_ERROR
                    LOG.error(e.getMessage(), e);
                    aip.setState(AIPState.STORAGE_ERROR);
                    aipDao.save(aip);
                    publisher.publish(new AIPEvent(aip));
                }
            }
        }
        return metadataToStore;
    }

    @Override
    public List<RejectedAip> applyRetryChecks(Set<String> aipIpIds) {
        List<RejectedAip> rejectedAips = Lists.newArrayList();
        for (String ipId : aipIpIds) {
            List<String> rejectionReasons = Lists.newArrayList();
            if (!aipDao.findOneByIpId(ipId).isPresent()) {
                rejectionReasons.add(String.format("AIP with ip id %s does not exists", ipId));
                rejectedAips.add(new RejectedAip(ipId, rejectionReasons));
            }
        }
        return rejectedAips;
    }

    @Override
    public List<RejectedAip> applyCreationChecks(AIPCollection aips) {
        List<RejectedAip> rejectedAips = Lists.newArrayList();
        for (AIP aip : aips.getFeatures()) {
            // each aip can be rejected for multiple reasons, lets aggregate them into a string
            boolean rejected = false;
            List<String> rejectionReasons = Lists.newArrayList();
            String ipId = aip.getId().toString();
            // first of all lets see if there already is an aip with this ip id into the database
            if (aipDao.findOneByIpId(ipId).isPresent()) {
                rejectionReasons.add(String.format("AIP with ip id %s already exists", ipId));
                rejected = true;
            }
            Errors errors = new BeanPropertyBindingResult(aip, "aip");
            validator.validate(aip, errors);
            if (errors.hasErrors()) {
                errors.getAllErrors().forEach(oe -> rejectionReasons.add(oe.getDefaultMessage()));
                // now lets handle validation issues
                rejected = true;
            }
            if (rejected) {
                rejectedAips.add(new RejectedAip(ipId, rejectionReasons));
            }
        }
        return rejectedAips;
    }

    @Override
    public Set<AIP> retrieveAipsByTag(String tag) {
        return aipDao.findAllByTags(tag);
    }

    @Override
    public List<Event> retrieveAIPHistory(UniformResourceName ipId) throws ModuleException {
        Optional<AIP> aip = aipDao.findOneByIpId(ipId.toString());
        if (aip.isPresent()) {
            if (!getSecurityDelegationPlugin().hasAccess(ipId.toString())) {
                throw new EntityOperationForbiddenException(ipId.toString(), AIP.class, AIP_ACCESS_FORBIDDEN);
            }
            return aip.get().getHistory();
        } else {
            throw new EntityNotFoundException(ipId.toString(), AIP.class);
        }
    }

    @Override
    public Set<AIP> retrieveAipsBulk(Set<String> ipIds) {
        return aipDao.findAllByIpIdIn(ipIds);
    }

    @Override
    public AIP retrieveAip(String ipId) throws EntityNotFoundException {
        return aipDao.findOneByIpId(ipId).orElseThrow(() -> new EntityNotFoundException(ipId, AIP.class));
    }

    @Override
    public AIP updateAip(String ipId, AIP updated)
            throws EntityNotFoundException, EntityInconsistentIdentifierException, EntityOperationForbiddenException {
        Optional<AIP> oldAipOpt = aipDao.findOneByIpId(ipId);
        // first lets check for issues
        if (!oldAipOpt.isPresent()) {
            throw new EntityNotFoundException(ipId, AIP.class);
        }
        AIP oldAip = oldAipOpt.get();
        if ((oldAip.getState() != AIPState.UPDATED) && (oldAip.getState() != AIPState.STORED)) {
            throw new EntityOperationForbiddenException(ipId, AIP.class, "update while aip metadata are being stored!");
        }
        if (updated.getId() == null) {
            throw new EntityNotFoundException("give updated AIP has no id!");
        }
        if (!oldAip.getId().toString().equals(updated.getId().toString())) {
            throw new EntityInconsistentIdentifierException(ipId, updated.getId().toString(), AIP.class);
        }
        LOG.debug(String.format("[METADATA UPDATE] updating metadata of aip %s", ipId));
        // now that requirement are meant, lets update the old one
        AIPBuilder updatingBuilder = new AIPBuilder(oldAip);
        // Only PDI and descriptive information can be updated
        PreservationDescriptionInformation updatedPdi = updated.getProperties().getPdi();
        // Provenance Information
        // first lets merge the events
        updatingBuilder.getPDIBuilder()
                .addProvenanceInformationEvents(updated.getHistory().toArray(new Event[updated.getHistory().size()]));
        // second lets merge other provenance informations
        Map<String, Object> additionalProvenanceInfoMap;
        if ((additionalProvenanceInfoMap = updatedPdi.getProvenanceInformation().getAdditional()) != null) {
            for (Map.Entry<String, Object> additionalProvenanceEntry : additionalProvenanceInfoMap.entrySet()) {
                updatingBuilder.getPDIBuilder()
                        .addAdditionalProvenanceInformation(additionalProvenanceEntry.getKey(),
                                                            additionalProvenanceEntry.getValue());
            }
        }
        // third lets handle those "special" provenance information
        updatingBuilder.getPDIBuilder().setFacility(updatedPdi.getProvenanceInformation().getFacility());
        updatingBuilder.getPDIBuilder().setDetector(updatedPdi.getProvenanceInformation().getDetector());
        updatingBuilder.getPDIBuilder().setFilter(updatedPdi.getProvenanceInformation().getFilter());
        updatingBuilder.getPDIBuilder().setInstrument(updatedPdi.getProvenanceInformation().getInstrument());
        updatingBuilder.getPDIBuilder().setProposal(updatedPdi.getProvenanceInformation().getProposal());
        // Context Information
        // first tags
        updatingBuilder.getPDIBuilder().addTags(updated.getTags().toArray(new String[updated.getTags().size()]));
        // now the rest of them
        Map<String, Object> contextInformationMap;
        if ((contextInformationMap = updatedPdi.getContextInformation()) != null) {
            for (Map.Entry<String, Object> contextEntry : contextInformationMap.entrySet()) {
                // tags have their specific handling
                if (!contextEntry.getKey().equals(PreservationDescriptionInformation.CONTEXT_INFO_TAGS_KEY)) {
                    updatingBuilder.getPDIBuilder().addContextInformation(contextEntry.getKey(),
                                                                          contextEntry.getValue());
                }
            }
        }
        // reference information
        Map<String, String> referenceInformationMap;
        if ((referenceInformationMap = updatedPdi.getReferenceInformation()) != null) {
            for (Map.Entry<String, String> refEntry : referenceInformationMap.entrySet()) {
                // tags have their specific handling
                updatingBuilder.getPDIBuilder().addContextInformation(refEntry.getKey(), refEntry.getValue());
            }
        }
        // fixity information
        Map<String, Object> fixityInformationMap;
        if ((fixityInformationMap = updatedPdi.getFixityInformation()) != null) {
            for (Map.Entry<String, Object> fixityEntry : fixityInformationMap.entrySet()) {
                // tags have their specific handling
                updatingBuilder.getPDIBuilder().addContextInformation(fixityEntry.getKey(), fixityEntry.getValue());
            }
        }
        // Access Right information
        updatingBuilder.getPDIBuilder()
                .setAccessRightInformation(updatedPdi.getAccessRightInformation().getLicence(),
                                           updatedPdi.getAccessRightInformation().getDataRights(),
                                           updatedPdi.getAccessRightInformation().getPublicReleaseDate());
        // descriptive information
        Map<String, Object> descriptiveInformationMap;
        if ((descriptiveInformationMap = updated.getProperties().getDescriptiveInformation()) != null) {
            for (Map.Entry<String, Object> descriptiveEntry : descriptiveInformationMap.entrySet()) {
                updatingBuilder.addDescriptiveInformation(descriptiveEntry.getKey(), descriptiveEntry.getValue());
            }
        }
        // now that all updates are set into the builder, lets build and save the updatedAip. Update event is added
        // once the metadata are stored
        AIP updatedAip = updatingBuilder.build();
        updatedAip.setState(AIPState.UPDATED);
        LOG.debug(String.format("[METADATA UPDATE] Update of aip %s metadata done", ipId));
        LOG.trace(String.format("[METADATA UPDATE] Updated aip : %s", gson.toJson(updatedAip)));
        return aipDao.save(updatedAip);
    }

    @Override
    public Set<StorageDataFile> deleteAip(String ipId) throws ModuleException {
        Optional<AIP> toBeDeletedOpt = aipDao.findOneByIpId(ipId);
        Set<StorageDataFile> notSuppressible = Sets.newHashSet();
        if (toBeDeletedOpt.isPresent()) {
            AIP toBeDeleted = toBeDeletedOpt.get();
            Set<StorageDataFile> dataFilesToDelete = Sets.newHashSet();
            // check if data file are use by any other aip
            Set<StorageDataFile> dataFiles = dataFileDao.findAllByAip(toBeDeleted);
            for (StorageDataFile dataFile : dataFiles) {
                if (dataFile.getState().equals(DataFileState.ERROR)) {
                    // we do not do remove immediately because the aip metadata has to be updated first
                    // and the logic is already implemented into DataStorageEventHandler
                    publisher.publish(new DataStorageEvent(dataFile, StorageAction.DELETION,
                            StorageEventType.SUCCESSFULL, null));
                } else {
                    if (dataFile.getState().equals(DataFileState.PENDING)) {
                        notSuppressible.add(dataFile);
                    } else {
                        // we order deletion of a file if and only if no other aip references the same file
                        Set<StorageDataFile> dataFilesWithSameFile = dataFileDao
                                .findAllByChecksumIn(Sets.newHashSet(dataFile.getChecksum()));
                        // well lets remove ourselves of course!
                        dataFilesWithSameFile.remove(dataFile);
                        if (dataFilesWithSameFile.isEmpty()) {
                            // add to datafiles that should be removed
                            dataFilesToDelete.add(dataFile);
                        } else {
                            // if other datafiles are referencing a file, we just remove the data file from the
                            // database.
                            // we do not do remove immediately because the aip metadata has to be updated first
                            // and the logic is already implemented into DataStorageEventHandler
                            publisher.publish(new DataStorageEvent(dataFile, StorageAction.DELETION,
                                                                   StorageEventType.SUCCESSFULL, null));
                        }
                    }
                }
            }
            // schedule removal of data and metadata
            AIPBuilder toBeDeletedBuilder = new AIPBuilder(toBeDeleted);
            toBeDeletedBuilder
                    .addEvent(EventType.DELETION.name(),
                              "AIP deletion was requested, AIP is considered deleted until its removal from archives",
                              OffsetDateTime.now());
            toBeDeleted = toBeDeletedBuilder.build();
            toBeDeleted.setState(AIPState.DELETED);
            aipDao.save(toBeDeleted);
            scheduleDeletion(dataFilesToDelete);
            return notSuppressible;
        }
        return notSuppressible;
    }

    @Override
    public Set<StorageDataFile> deleteAipFromSip(String sipIpId) throws ModuleException {
        Set<StorageDataFile> notSuppressible = new HashSet<>();
        for (AIP aip : aipDao.findAllBySipId(sipIpId)) {
            notSuppressible.addAll(deleteAip(aip.getId().toString()));
        }
        return notSuppressible;
    }

    @Override
    public void addTags(String ipId, Set<String> tagsToAdd)
            throws EntityNotFoundException, EntityInconsistentIdentifierException, EntityOperationForbiddenException {
        AIP toUpdate = retrieveAip(ipId);
        AIPBuilder updateBuilder = new AIPBuilder(toUpdate);
        updateBuilder.addTags(tagsToAdd.toArray(new String[tagsToAdd.size()]));
        toUpdate = updateBuilder.build();
        updateAip(ipId, toUpdate);
    }

    @Override
    public void removeTags(String ipId, Set<String> tagsToRemove)
            throws EntityNotFoundException, EntityInconsistentIdentifierException, EntityOperationForbiddenException {
        AIP toUpdate = retrieveAip(ipId);
        AIPBuilder updateBuilder = new AIPBuilder(toUpdate);
        updateBuilder.removeTags(tagsToRemove.toArray(new String[tagsToRemove.size()]));
        toUpdate = updateBuilder.build();
        updateAip(ipId, toUpdate);
    }

    private Set<UUID> scheduleDeletion(Set<StorageDataFile> dataFilesToDelete) throws ModuleException {
        // when we delete DataFiles, we have to get the DataStorages to use thanks to DB informations
        Multimap<Long, StorageDataFile> deletionWorkingSetMultimap = HashMultimap.create();
        for (StorageDataFile toDelete : dataFilesToDelete) {
            toDelete.getPrioritizedDataStorages()
                    .forEach(dataStorage -> deletionWorkingSetMultimap.put(dataStorage.getId(), toDelete));
        }
        Set<UUID> jobIds = Sets.newHashSet();
        for (Long dataStorageConfId : deletionWorkingSetMultimap.keySet()) {
            Set<IWorkingSubset> workingSubSets = getWorkingSubsets(deletionWorkingSetMultimap.get(dataStorageConfId),
                                                                   dataStorageConfId,
                                                                   DataStorageAccessModeEnum.DELETION_MODE);
            LOG.trace("Preparing a deletion job for each working subsets");
            // lets instantiate every job for every DataStorage to use
            for (IWorkingSubset workingSubset : workingSubSets) {
                // for each DataStorage we can have multiple WorkingSubSet to treat in parallel, lets storeAndCreate a
                // job for
                // each of them
                Set<JobParameter> parameters = Sets.newHashSet();
                parameters.add(new JobParameter(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, dataStorageConfId));
                parameters.add(new JobParameter(AbstractStoreFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));
                jobIds.add(jobInfoService.createAsQueued(new JobInfo(false, 0, parameters, authResolver.getUser(),
                        DeleteDataFilesJob.class.getName())).getId());

            }
        }
        return jobIds;
    }

    /**
     * Write on disk the asscoiated metadata file of the given {@link AIP}.
     * @param aip {@link AIP}
     * @return {@link StorageDataFile} of the {@link AIP} metadata file.
     * @throws IOException Impossible to write {@link AIP} metadata file to disk.
     */
    private StorageDataFile writeMetaToWorkspace(AIP aip) throws IOException, FileCorruptedException {

        StorageDataFile metadataAipFile;
        String checksumAlgorithm = "MD5";
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance(checksumAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        String toWrite = gson.toJson(aip);
        String checksum = ChecksumUtils.getHexChecksum(md5.digest(toWrite.getBytes(StandardCharsets.UTF_8)));
        String metadataName = checksum + JSON_FILE_EXT;
        workspaceService.setIntoWorkspace(new ByteArrayInputStream(toWrite.getBytes(StandardCharsets.UTF_8)),
                                          metadataName);
        try (InputStream is = workspaceService.retrieveFromWorkspace(metadataName)) {
            String fileChecksum = ChecksumUtils.computeHexChecksum(is, checksumAlgorithm);
            if (fileChecksum.equals(checksum)) {
                URL urlToMetadata = new URL("file", "localhost",
                        workspaceService.getFilePath(metadataName).toAbsolutePath().toString());
                metadataAipFile = new StorageDataFile(Sets.newHashSet(urlToMetadata), checksum, checksumAlgorithm,
                        DataType.AIP, urlToMetadata.openConnection().getContentLengthLong(),
                        new MimeType("application", "json"), aip, aip.getId().toString() + JSON_FILE_EXT, null);
            } else {
                workspaceService.removeFromWorkspace(metadataName);
                LOG.error(String
                        .format("Storage of AIP metadata(%s) into workspace(%s) failed. Computed checksum once stored does not "
                                + "match expected one", aip.getId().toString(),
                                workspaceService.getMicroserviceWorkspace()));
                throw new FileCorruptedException(String
                        .format("File has been corrupted during storage into workspace. Checksums before(%s) and after (%s) are"
                                + " different", checksum, fileChecksum));
            }
        } catch (NoSuchAlgorithmException e) {
            // Delete written file
            LOG.error(e.getMessage(), e);
            workspaceService.removeFromWorkspace(metadataName);
            // this exception should never be thrown as it comes from the same algorithm then at the beginning
            throw new IOException(e);
        }
        return metadataAipFile;
    }

    /*
     * Non javadoc, but explanatory: due to settings only interfaces are proxyfied by spring, so we need to use a self
     * reference on the interface to profit from transaction management from spring. This is a self reference because
     * AIPService is annotated @Service with default component scope which is "spring' SINGLETON
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Scheduled(fixedDelayString = "${regards.storage.update.aip.metadata.delay:7200000}") // 2 hours
    @Override
    public void updateAlreadyStoredMetadata() {
        // Then lets get AIP that should be stored again after an update
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            LOG.debug(String.format("[METADATA UPDATE DAEMON] Starting to prepare update jobs for tenant %s", tenant));
            Set<UpdatableMetadataFile> metadataToUpdate = self.prepareUpdatedAIP();
            if (!metadataToUpdate.isEmpty()) {
                self.scheduleStorageMetadataUpdate(metadataToUpdate);
            }
            LOG.debug(String.format("[METADATA UPDATE DAEMON] Update jobs for tenant %s have been scheduled", tenant));
        }
    }

    /**
     * Prepare all AIP in UPDATED state in order to create and store the new AIP metadata file (descriptor file)
     * asscoiated.<br/>
     * After an AIP is updated in database, this method write the new {@link StorageDataFile} of the AIP metadata on
     * disk
     * and return the list of created {@link StorageDataFile} mapped to the old {@link StorageDataFile} of the updated
     * AIPs.
     * @return {@link Set}<{@link UpdatableMetadataFile}> The list of {@link StorageDataFile} to storeAndCreate.
     */
    @Override
    public Set<UpdatableMetadataFile> prepareUpdatedAIP() {
        Set<UpdatableMetadataFile> result = Sets.newHashSet();
        Set<AIP> updatedAips = aipDao.findAllByStateService(AIPState.UPDATED);
        for (AIP updatedAip : updatedAips) {
            // Store the associated dataFile.
            Optional<StorageDataFile> optionalExistingAIPMetadataFile = dataFileDao.findByAipAndType(updatedAip,
                                                                                                     DataType.AIP);
            if (optionalExistingAIPMetadataFile.isPresent()) {
                // Create new AIP file (descriptor file) for the given updated AIP.
                StorageDataFile existingAIPMetadataFile = optionalExistingAIPMetadataFile.get();
                StorageDataFile newAIPMetadataFile;
                try {
                    // To ensure that at any time there is only one StorageDataFile of AIP type, we do not create
                    // a new StorageDataFile for the newAIPMetadataFile.
                    // The newAIPMetadataFile get the id of the old one and so only replace it when it is stored.
                    newAIPMetadataFile = writeMetaToWorkspace(updatedAip);
                    newAIPMetadataFile.setId(existingAIPMetadataFile.getId());
                    result.add(new UpdatableMetadataFile(existingAIPMetadataFile, newAIPMetadataFile));
                } catch (IOException | FileCorruptedException e) {
                    LOG.error(e.getMessage(), e);
                    // if we don't have a meta to storeAndCreate that means a problem happened and we set the aip to
                    // STORAGE_ERROR
                    updatedAip.setState(AIPState.STORAGE_ERROR);
                    aipDao.save(updatedAip);
                    publisher.publish(new AIPEvent(updatedAip));
                }
            } else {
                String message = String.format("Unable to update AIP metadata for AIP %s as there no existing one",
                                               updatedAip.getId());
                LOG.warn(message);
                notifyAdmins("AIP metadata could not be updated", message, NotificationType.INFO);
            }
        }
        return result;
    }

    @Override
    public void scheduleStorageMetadataUpdate(Set<UpdatableMetadataFile> metadataToUpdate) {
        try {
            // we need to listen to those jobs event for two things: cleaning the workspace and update AIP state
            doScheduleStorageMetadataUpdate(metadataToUpdate);
            // to avoid making jobs for the same metadata all the time, lets change the metadataToStore AIP state to
            // STORING_METADATA
            Set<AIP> aips = metadataToUpdate.stream().map(oldNew -> oldNew.getNewOne().getAip())
                    .collect(Collectors.toSet());
            for (AIP aip : aips) {
                LOG.debug("[UPDATING AIP METADATA] AIP {} is in STORING_METADATA state", aip.getId().toString());
                aip.setState(AIPState.STORING_METADATA);
                aipDao.save(aip);
                publisher.publish(new AIPEvent(aip));
            }
        } catch (ModuleException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * Schedule new {@link UpdateDataFilesJob}s to storeAndCreate given updated {@link StorageDataFile}.
     *
     * To do so, this method dispatch all {@link StorageDataFile} of new AIP metadata files to storeAndCreate
     * by {@link PluginConfiguration} of storage plugin used to storeAndCreate associated {@link StorageDataFile}
     * of old AIP metadata.<br/>
     * Then, a new job si scheduled for each {@link IWorkingSubset} returned by
     * the associated {@link PluginConfiguration}s.<br/>
     * @param metadataToUpdate List of {@link StorageDataFile} of new AIP metadata files mapped to old ones.
     * @return {@link Set}<{@link UUID}> List of all Jobs id scheduled.
     */
    private Set<UUID> doScheduleStorageMetadataUpdate(Set<UpdatableMetadataFile> metadataToUpdate)
            throws ModuleException {
        Set<UUID> jobIds = Sets.newHashSet();
        // This is an update so we don't use the allocation strategy and we directly use the PluginConf used to
        // store the file.
        // Lets construct the Multimap<PluginConf, StorageDataFile> allowing us to then create the IWorkingSubSets
        Multimap<Long, StorageDataFile> toPrepareMap = HashMultimap.create();
        for (UpdatableMetadataFile oldNew : metadataToUpdate) {
            Set<PrioritizedDataStorage> oldDataStorages = oldNew.getOldOne().getPrioritizedDataStorages();
            oldNew.getNewOne().setNotYetStoredBy(((Number) oldDataStorages.size()).longValue());
            dataFileDao.save(oldNew.getNewOne());
            for (PrioritizedDataStorage oldDataStorage : oldDataStorages) {
                toPrepareMap.put(oldDataStorage.getId(), oldNew.getNewOne());
            }
        }
        // now lets work with workingSubsets
        Set<JobInfo> jobsToSchedule = Sets.newHashSet();
        for (Long dataStorageConfId : toPrepareMap.keySet()) {
            Set<IWorkingSubset> workingSubsets = getWorkingSubsets(toPrepareMap.get(dataStorageConfId),
                                                                   dataStorageConfId,
                                                                   DataStorageAccessModeEnum.STORE_MODE);
            for (IWorkingSubset workingSubset : workingSubsets) {
                // for each workingSubset lets get the corresponding old metadata to remove
                Set<StorageDataFile> oldOneCorrespondingToWorkingSubset = metadataToUpdate.stream()
                        .filter(oldNew -> workingSubset.getDataFiles().contains(oldNew.getNewOne()))
                        .map(oldNew -> oldNew.getOldOne()).collect(Collectors.toSet());
                Set<JobParameter> parameters = Sets.newHashSet();
                parameters.add(new JobParameter(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, dataStorageConfId));
                parameters.add(new JobParameter(AbstractStoreFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));
                parameters.add(new JobParameter(UpdateDataFilesJob.OLD_DATA_FILES_PARAMETER_NAME,
                        oldOneCorrespondingToWorkingSubset
                                .toArray(new StorageDataFile[oldOneCorrespondingToWorkingSubset.size()])));
                jobIds.add(jobInfoService.createAsQueued(new JobInfo(false, 0, parameters, authResolver.getUser(),
                        UpdateDataFilesJob.class.getName())).getId());
            }
        }
        // now that files are given to the jobs, lets remove the source url so once stored we only have the good urls
        Collection<StorageDataFile> storageDataFiles = toPrepareMap.values();
        storageDataFiles.forEach(file -> file.setUrls(new HashSet<>()));
        dataFileDao.save(storageDataFiles);
        return jobIds;
    }

    @Override
    public Pair<StorageDataFile, InputStream> getAIPDataFile(String pAipId, String pChecksum)
            throws ModuleException, IOException {
        // First find the AIP
        Optional<AIP> oaip = aipDao.findOneByIpId(pAipId);
        if (oaip.isPresent()) {
            AIP aip = oaip.get();
            if (!getSecurityDelegationPlugin().hasAccess(pAipId)) {
                throw new EntityOperationForbiddenException(pAipId, AIP.class, AIP_ACCESS_FORBIDDEN);
            }
            // Now get requested StorageDataFile
            Set<StorageDataFile> aipDataFiles = dataFileDao.findAllByAip(aip);
            Optional<StorageDataFile> odf = aipDataFiles.stream().filter(df -> pChecksum.equals(df.getChecksum()))
                    .findFirst();
            if (odf.isPresent()) {
                StorageDataFile dataFile = odf.get();
                if (dataFile.getPrioritizedDataStorages() != null) {
                    // first let see if this file is stored on an online data storage and lets get the most prioritized
                    Optional<PrioritizedDataStorage> onlinePrioritizedDataStorageOpt = dataFile
                            .getPrioritizedDataStorages().stream()
                            .filter(pds -> pds.getDataStorageType().equals(DataStorageType.ONLINE) && pds.getDataStorageConfiguration().isActive()).sorted()
                            .findFirst();
                    if (onlinePrioritizedDataStorageOpt.isPresent()) {
                        InputStream dataFileIS = ((IOnlineDataStorage) pluginService
                                .getPlugin(onlinePrioritizedDataStorageOpt.get().getId())).retrieve(dataFile);
                        return Pair.of(dataFile, dataFileIS);
                    } else {
                        // Check if file is available from cache
                        Optional<CachedFile> ocf = cachedFileService.getAvailableCachedFile(pChecksum);
                        if (ocf.isPresent()) {
                            return Pair.of(dataFile, new FileInputStream(ocf.get().getLocation().getPath()));
                        } else {
                            return null;
                        }
                    }
                } else {
                    throw new EntityNotFoundException("Storage plugin used to store datafile is unknown.");
                }
            } else {
                throw new EntityNotFoundException(pChecksum, StorageDataFile.class);
            }

        } else {
            throw new EntityNotFoundException(pAipId, AIP.class);
        }
    }
}
