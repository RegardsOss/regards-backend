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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.util.Pair;
import org.springframework.hateoas.PagedResources;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.microservice.maintenance.MaintenanceException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.PreservationDescriptionInformation;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.notification.domain.NotificationLevel;
import fr.cnes.regards.modules.storage.dao.AIPQueryGenerator;
import fr.cnes.regards.modules.storage.dao.AIPSessionSpecifications;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IAIPEntityRepository;
import fr.cnes.regards.modules.storage.dao.IAIPSessionRepository;
import fr.cnes.regards.modules.storage.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.dao.IPrioritizedDataStorageRepository;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.AIPPageWithDataStorages;
import fr.cnes.regards.modules.storage.domain.AIPSessionBuilder;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.AIPWithDataStorageIds;
import fr.cnes.regards.modules.storage.domain.AipDataFiles;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.CoupleAvailableError;
import fr.cnes.regards.modules.storage.domain.RejectedAip;
import fr.cnes.regards.modules.storage.domain.RejectedSip;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.database.AIPUpdateRequest;
import fr.cnes.regards.modules.storage.domain.database.CachedFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.domain.database.DataStorageType;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.event.StorageAction;
import fr.cnes.regards.modules.storage.domain.event.StorageEventType;
import fr.cnes.regards.modules.storage.domain.exception.InvalidDatastoragePluginConfException;
import fr.cnes.regards.modules.storage.domain.job.AIPQueryFilters;
import fr.cnes.regards.modules.storage.domain.job.AddAIPTagsFilters;
import fr.cnes.regards.modules.storage.domain.job.RemoveAIPTagsFilters;
import fr.cnes.regards.modules.storage.domain.job.UpdateAIPsTagJobType;
import fr.cnes.regards.modules.storage.domain.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.domain.plugin.DispatchErrors;
import fr.cnes.regards.modules.storage.domain.plugin.IAllocationStrategy;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.ISecurityDelegation;
import fr.cnes.regards.modules.storage.domain.plugin.IWorkingSubset;
import fr.cnes.regards.modules.storage.domain.plugin.WorkingSubsetWrapper;
import fr.cnes.regards.modules.storage.service.job.AbstractStoreFilesJob;
import fr.cnes.regards.modules.storage.service.job.DeleteAIPsJob;
import fr.cnes.regards.modules.storage.service.job.DeleteDataFilesJob;
import fr.cnes.regards.modules.storage.service.job.DeleteFilesFromDataStorageJob;
import fr.cnes.regards.modules.storage.service.job.StorageJobsPriority;
import fr.cnes.regards.modules.storage.service.job.StoreDataFilesJob;
import fr.cnes.regards.modules.storage.service.job.StoreMetadataFilesJob;
import fr.cnes.regards.modules.storage.service.job.UpdateAIPsTagJob;
import fr.cnes.regards.modules.storage.service.job.WriteAIPMetadataJob;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import fr.cnes.regards.modules.templates.service.TemplateServiceConfiguration;

/**
 * Service to handle {@link AIP} and associated {@link StorageDataFile}s entities from all data storage systems.<br/>
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
 * <li>storeMetadata : This cron action executed every minutes handle
 * update of {@link AIP} state by looking for all associated {@link StorageDataFile} states.
 * An {@link AIP} is STORED when all its {@link StorageDataFile}s are STORED</li>
 * </ul>
 * <br/>
 * The cache system to make nearline files accessible is handled by the {@link ICachedFileService}.<br/>
 * @author Sylvain Vissiere-Guerinet
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class AIPService implements IAIPService {

    private static final String DEFAULT_SESSION_ID = "default";

    /**
     * Class logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AIPService.class);

    /**
     * Forbidden aip access message
     */
    private static final String AIP_ACCESS_FORBIDDEN = "You do not have suffisent access right to get this aip.";

    /**
     * Number of created AIPs processed on each iteration by project
     */
    @Value("${regards.storage.aips.iteration.limit:100}")
    private Integer aipIterationLimit;

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
     * Service to manage avaibility of nearline files.
     */
    @Autowired
    private ICachedFileService cachedFileService;

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

    @Autowired
    private EntityManager em;

    @Autowired
    private IAIPSessionRepository aipSessionRepository;

    @Autowired
    private IAIPEntityRepository aipEntityRepository;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepo;

    @Autowired
    private IPrioritizedDataStorageRepository prioritizedDataStorageRepo;

    @Override
    public AIP save(AIP aip, boolean publish) {
        // Create the session if it's not already existing
        try {
            AIPSession aipSession = getSession(aip.getSession(), true);

            AIP daoAip = aipDao.save(aip, aipSession);
            if (publish) {
                publisher.publish(new AIPEvent(daoAip));
            }
            em.flush();
            em.clear();
            return daoAip;
        } catch (EntityNotFoundException e) {
            // this exception cannot be thrown as getSession(something, true) will create said session
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public List<RejectedAip> validateAndStore(AIPCollection aips) throws ModuleException {

        // Validate AIPs
        List<RejectedAip> rejectedAips = new ArrayList<>();
        Set<AIP> validAips = validate(aips, rejectedAips);

        // Store valid AIPs
        for (AIP aip : validAips) {
            storeValidAip(aip, false);
        }

        return rejectedAips;
    }

    private void storeValidAip(AIP aip, boolean publish) {
        aip.setState(AIPState.VALID);
        aip.addEvent(EventType.SUBMISSION.name(), "Submission to REGARDS");
        save(aip, publish);

        // Extract data files
        AIPSession aipSession;
        try {
            aipSession = getSession(aip.getSession(), false);
        } catch (EntityNotFoundException e) {
            // this exception cannot be thrown as save will create said session
            throw new RsRuntimeException(e);
        }
        Set<StorageDataFile> dataFiles = StorageDataFile.extractDataFiles(aip, aipSession);
        dataFiles.forEach(df -> {
            df.setState(DataFileState.PENDING);
            df.getOriginUrls().clear();
            df.getOriginUrls().addAll(df.getUrls());
            dataFileDao.save(df);
        });
        // To avoid performance problems due to hibernate cache size. We flush entity manager after each entity to save.
        em.flush();
        em.clear();
    }

    @Override
    public void validateAndStore(AIP aip) {
        List<String> rejectionReasons = Lists.newArrayList();
        if (validate(aip, rejectionReasons)) {
            storeValidAip(aip, true);
        } else {
            aip.setState(AIPState.REJECTED);
            AIPEvent event = new AIPEvent(aip);
            StringJoiner joiner = new StringJoiner(" ");
            rejectionReasons.forEach(r -> joiner.add(r));
            event.setFailureCause(joiner.toString());
            publisher.publish(event);
        }

    }

    /**
     * Validate submitted AIPs
     * @param aips AIP collection to validate
     * @param rejectedAips invalid AIPs
     * @return valid AIPs
     */
    private Set<AIP> validate(AIPCollection aips, List<RejectedAip> rejectedAips) {

        Set<AIP> validAips = new HashSet<>();

        for (AIP aip : aips.getFeatures()) {
            // each aip can be rejected for multiple reasons, lets aggregate them into a string
            List<String> rejectionReasons = Lists.newArrayList();
            if (validate(aip, rejectionReasons)) {
                validAips.add(aip);
            } else {
                rejectedAips.add(new RejectedAip(aip.getId().toString(), rejectionReasons));
            }
        }
        return validAips;
    }

    /**
     * Validate a single AIP
     * @param aip the AIP to validate
     * @param rejectionReasons reasons for rejection if not valid
     * @return true is AIP is valid
     */
    private boolean validate(AIP aip, List<String> rejectionReasons) {
        boolean validated = true;
        // first of all lets see if there already is an aip with this ip id into the database
        String ipId = aip.getId().toString();
        if (aipDao.findOneByAipId(ipId).isPresent()) {
            rejectionReasons.add(String.format("AIP with ip id %s already exists.", ipId));
            validated = false;
        }
        Errors errors = new BeanPropertyBindingResult(aip, "aip");
        validator.validate(aip, errors);
        if (errors.hasErrors()) {
            errors.getFieldErrors().forEach(oe -> rejectionReasons
                    .add(String.format("Property %s is invalid: %s.", oe.getField(), oe.getDefaultMessage())));
            // now lets handle validation issues
            validated = false;
        }
        return validated;
    }

    @Override
    public Page<AIP> storePage(Pageable page) throws ModuleException {
        LOGGER.trace("[STORE] Start.");
        Page<AIP> createdAips = aipDao.findAllByState(AIPState.VALID, page);
        if (createdAips.getNumberOfElements() > 0) {
            LOGGER.trace("[STORE] {} aip in valid state", createdAips.getTotalElements());
            Set<StorageDataFile> dataFilesToStore = Sets.newHashSet();
            for (AIP aip : createdAips) {
                // Retrieve data files to store
                Collection<StorageDataFile> dataFiles;
                if (aip.isRetry()) {
                    dataFiles = dataFileDao.findAllByStateAndAip(DataFileState.ERROR, aip);
                    for (StorageDataFile retryDataFile : dataFiles) {
                        retryDataFile.getUrls().clear();
                        retryDataFile.getUrls().addAll(retryDataFile.getOriginUrls());
                    }
                } else {
                    dataFiles = dataFileDao.findAllByStateAndAip(DataFileState.PENDING, aip);
                }

                if (dataFiles.size() > 0) {
                    aip.setState(AIPState.PENDING);
                } else {
                    aip.setState(AIPState.DATAFILES_STORED);
                }
                aip.setRetry(false);
                aipDao.updateAIPStateAndRetry(aip);
                dataFilesToStore.addAll(dataFiles);
                publisher.publish(new AIPEvent(aip));
            }
            // Dispatch and check data files
            Multimap<Long, StorageDataFile> storageWorkingSetMap = dispatchAndCheck(dataFilesToStore);
            // Schedule storage jobs
            LOGGER.trace("[STORE] Schedule storage for {} datafiles.", storageWorkingSetMap.entries().size());
            scheduleStorage(storageWorkingSetMap, true);
            LOGGER.trace("[STORE] Schedule Done.", storageWorkingSetMap.entries().size());
        }
        LOGGER.trace("[STORE] End.");
        return createdAips;
    }

    @Override
    public long storeMetadata() {
        LOGGER.trace("[METADATA STORE] Start.");
        // first lets get AIP which all files are stored. So those AIP are ready to write the metadata file.
        Set<AIP> metadataToStore = getMetadataFilesToStore();
        // now that we know all the metadata that should be stored, lets schedule their storage!
        if (!metadataToStore.isEmpty()) {
            LOGGER.debug("[METADATA STORE] Scheduling {} new metadata to be write.", metadataToStore.size());
            scheduleWriteMetadata(metadataToStore);
        } else {
            LOGGER.trace("[METADATA STORE] No new metadata file to store.");
        }
        LOGGER.trace("[METADATA STORE] End.");
        return metadataToStore.size();
    }

    private void scheduleWriteMetadata(Set<AIP> metadataToStore) {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(WriteAIPMetadataJob.AIP_IDS_TO_WRITE_METADATA,
                metadataToStore.stream().map(aip -> aip.getId().toString()).collect(Collectors.toSet())));
        jobInfoService.createAsQueued(new JobInfo(false, StorageJobsPriority.WRITING_METADATA_JOB, parameters,
                authResolver.getUser(), WriteAIPMetadataJob.class.getName()));
        for (AIP aip : metadataToStore) {
            aip.setState(AIPState.WRITING_METADATA);
            try {
                aipDao.save(aip, getSession(aip.getSession(), false));
            } catch (EntityNotFoundException e) {
                // this exception should not be thrown now as the aip already exists and so does the session
                throw new RsRuntimeException(e);
            }
        }
    }

    /**
     * Dispatch given dataFilesToStore between {@link IDataStorage}s thanks to the active {@link IAllocationStrategy}
     * and check they all have been dispatched
     * @param dataFilesToStore {@link StorageDataFile} to store
     * @return dispatched {@link StorageDataFile}
     */
    private Multimap<Long, StorageDataFile> dispatchAndCheck(Set<StorageDataFile> dataFilesToStore)
            throws ModuleException {
        IAllocationStrategy allocationStrategy = getAllocationStrategy();
        // Now lets ask to the strategy to dispatch dataFiles between possible DataStorages
        DispatchErrors dispatchErrors = new DispatchErrors();
        Multimap<Long, StorageDataFile> storageWorkingSetMap = allocationStrategy.dispatch(dataFilesToStore,
                                                                                           dispatchErrors);
        LOGGER.debug("[STORE] {} data objects has been dispatched between {} data storage by allocation strategy",
                     dataFilesToStore.size(), storageWorkingSetMap.keySet().size());
        // as we are trusty people, we check that the dispatch gave us back all DataFiles into the WorkingSubSets
        LOGGER.trace("[STORE] Check missing files from dispatch results ...");
        checkDispatch(dataFilesToStore, storageWorkingSetMap, dispatchErrors);
        LOGGER.trace("[STORE] Check missing files from dispatch results. OK");
        // now that those who should be in error are handled,  lets set notYetStoredBy and save data files
        for (StorageDataFile df : storageWorkingSetMap.values()) {
            df.increaseNotYetStoredBy();
        }
        LOGGER.trace("[STORE] Saving files ...");
        // Save dataFiles
        for (StorageDataFile file : storageWorkingSetMap.values()) {
            dataFileDao.save(file);
            em.flush();
            em.clear();
        }
        LOGGER.trace("[STORE] Files saved.");
        return storageWorkingSetMap;
    }

    @Override
    public void storeRetry(Set<String> aipIpIds) throws ModuleException {
        // lets get the data file which are in storage error state and ask for their storage, once again
        Set<AIP> failedAips = aipDao.findAllByAipIdIn(aipIpIds);
        for (AIP aip : failedAips) {
            if (AIPState.STORAGE_ERROR.equals(aip.getState())) {
                aip.setState(AIPState.VALID);
                aip.setRetry(true);
                save(aip, false);
            }
        }
    }

    @Override
    public AvailabilityResponse loadFiles(AvailabilityRequest availabilityRequest) throws ModuleException {
        // lets define result variables
        Set<StorageDataFile> onlineFiles = Sets.newHashSet();
        CoupleAvailableError nearlineAvailableAndError = new CoupleAvailableError(new HashSet<>(), new HashSet<>());
        Set<String> errors = Sets.newHashSet();

        Set<String> requestedChecksums = availabilityRequest.getChecksums();
        // Until proven otherwise, none of requested checksums are handled by REGARDS.
        Set<String> checksumNotFound = Sets.newHashSet(requestedChecksums);
        // Same for accesses
        Set<String> checksumsWithoutAccess = Sets.newHashSet(requestedChecksums);
        Pageable page = PageRequest.of(0, 500, Sort.Direction.ASC, "id");
        Page<StorageDataFile> dataFilePage = dataFileDao.findPageByStateAndChecksumIn(DataFileState.STORED,
                                                                                      requestedChecksums, page);
        while (dataFilePage.hasContent()) {

            Set<StorageDataFile> dataFiles = Sets.newHashSet(dataFilePage.getContent());
            // 1. Check for invalid files.
            // Because we only have a page of data file here, we must intersect the ones missing with the ones we have not found before too.
            if (dataFilePage.getTotalElements() != requestedChecksums.size()) {
                Set<String> dataFilesChecksumsForThisPage = dataFiles.stream().map(StorageDataFile::getChecksum)
                        .collect(Collectors.toSet());
                Set<String> checksumNotFoundForThisPage = Sets.difference(requestedChecksums,
                                                                          dataFilesChecksumsForThisPage);
                checksumNotFound = Sets.intersection(checksumNotFound, checksumNotFoundForThisPage);
            }

            Set<StorageDataFile> dataFilesWithAccess = checkLoadFilesAccessRights(dataFiles);

            // Once we know to which file we have access, lets set the others in error.
            // As a file can be associated to multiple AIP, we have to compare their checksums.
            Set<String> checksumsWithoutAccessForThisPage = Sets
                    .difference(dataFiles.stream().map(StorageDataFile::getChecksum).collect(Collectors.toSet()),
                                dataFilesWithAccess.stream().map(StorageDataFile::getChecksum)
                                        .collect(Collectors.toSet()));
            checksumsWithoutAccess = Sets.intersection(checksumsWithoutAccess, checksumsWithoutAccessForThisPage);

            Set<StorageDataFile> nearlineFiles = Sets.newHashSet();

            // 2. Check for online files. Online files don't need to be stored in the cache
            // they can be accessed directly where they are stored.
            for (StorageDataFile df : dataFilesWithAccess) {
                if ((df.getPrioritizedDataStorages() != null) && !df.getPrioritizedDataStorages().isEmpty()) {
                    Optional<PrioritizedDataStorage> onlinePrioritizedDataStorageOpt = df.getPrioritizedDataStorages()
                            .stream().filter(pds -> pds.getDataStorageType().equals(DataStorageType.ONLINE))
                            .findFirst();
                    if (onlinePrioritizedDataStorageOpt.isPresent()) {
                        onlineFiles.add(df);
                    } else {
                        nearlineFiles.add(df);
                    }
                } else {
                    LOGGER.error("File to restore {} has no storage plugin information. Restoration failed.",
                                 df.getId());
                }
            }
            // now lets ask the cache service to handle nearline restoration and give us the already available ones
            nearlineAvailableAndError = cachedFileService.restore(nearlineFiles,
                                                                  availabilityRequest.getExpirationDate());
            for (StorageDataFile inError : nearlineAvailableAndError.getErrors()) {
                errors.add(inError.getChecksum());
            }

            // Before getting the next page, lets evict actual entities from cache
            em.flush();
            em.clear();
            // now that hibernate cache has been cleared, lets get the next page
            page = page.next();
            dataFilePage = dataFileDao.findPageByChecksumIn(requestedChecksums, page);

        }
        // the if is needed here too because otherwise checksumNotFound initially being all requested checksums,
        // everything is considered not found
        if (dataFilePage.getTotalElements() != requestedChecksums.size()) {
            // lets logs not found now that we know that remaining checksums are not handled by REGARDS
            errors.addAll(checksumNotFound);
            checksumNotFound
                    .forEach(cs -> LOGGER.error("File to restore with checksum {} is not stored by REGARDS.", cs));
        }
        // same for accesses
        checksumsWithoutAccess.forEach(cs -> LOGGER.error("User {} does not have access to file with checksum {}.",
                                                          authResolver.getUser(), cs));
        errors.addAll(checksumsWithoutAccess);
        // lets construct the result
        return new AvailabilityResponse(errors, onlineFiles, nearlineAvailableAndError.getAvailables());
    }

    private Set<StorageDataFile> checkLoadFilesAccessRights(Set<StorageDataFile> dataFiles) throws ModuleException {
        // Creating a multimap of { aip -> files } to remove all files from not authorized AIPs
        Collector<StorageDataFile, HashMultimap<UniformResourceName, StorageDataFile>, HashMultimap<UniformResourceName, StorageDataFile>> multimapCollector = Collector
                .of(HashMultimap::create, (hashMultimap, df) -> hashMultimap.put(df.getAip().getId(), df),
                    (hashMultimap, hashMultimap2) -> {
                        hashMultimap.putAll(hashMultimap2);
                        return hashMultimap;
                    });
        // Apply multimapCollector in parallel
        Multimap<UniformResourceName, StorageDataFile> aipIdsMap = dataFiles.parallelStream()
                .collect(multimapCollector);
        ISecurityDelegation securityDelegationPlugin = getSecurityDelegationPlugin();
        // Check security...
        Set<UniformResourceName> urnsWithAccess = securityDelegationPlugin.hasAccess(aipIdsMap.keySet());
        if (urnsWithAccess.size() != aipIdsMap.keySet().size()) {
            aipIdsMap.keySet().removeIf(uniformResourceName -> !urnsWithAccess.contains(uniformResourceName));
        }
        return ImmutableSet.copyOf(aipIdsMap.values());
    }

    @Override
    public Page<AIP> retrieveAIPs(AIPState state, OffsetDateTime from, OffsetDateTime to, List<String> tags,
            String session, String providerId, Set<Long> storedOn, Pageable pageable) throws ModuleException {
        if (!getSecurityDelegationPlugin().hasAccessToListFeature()) {
            throw new EntityOperationForbiddenException("Only Admins can access this feature.");
        }
        return aipDao.findAll(AIPQueryGenerator.searchAIPContainingAllTags(state, from, to, tags, session, providerId,
                                                                           null, null, storedOn),
                              pageable);
    }

    @Override
    public AIPPageWithDataStorages retrieveAIPWithDataStorageIds(AIPQueryFilters filters, Pageable pageable)
            throws ModuleException {
        // In all this method usage of list is mandatory to keep order

        if (!getSecurityDelegationPlugin().hasAccessToListFeature()) {
            throw new EntityOperationForbiddenException("Only Admins can access this feature.");
        }
        String aipQueryWithoutPage = AIPQueryGenerator
                .searchAIPIdContainingAllTags(filters.getState(), filters.getFrom(), filters.getTo(), filters.getTags(),
                                              filters.getSession(), filters.getProviderId(), filters.getAipIds(),
                                              filters.getAipIdsExcluded(), filters.getStoredOn());
        String aipQuery = aipQueryWithoutPage + " LIMIT " + pageable.getPageSize() + " OFFSET " + pageable.getOffset();
        // first lets get information for this page

        String sqlQuery = "select id from {h-schema}t_data_file sdf where sdf.aip_ip_id IN (" + aipQuery
                + ") order by sdf.aip_ip_id";
        Query q = em.createNativeQuery(sqlQuery);
        @SuppressWarnings("unchecked")
        List<Long> dataFileIds = q.getResultList().stream().mapToLong(r -> ((BigInteger) r).longValue()).boxed()
                .collect(Collectors.toList());
        List<StorageDataFile> dataFiles = dataFileDao.findAllById(dataFileIds);
        // lets sort everything by aip, maps with object as key does not work as espected, lets use 2 map with same key to achieve our goal
        Map<String, AIP> aipIdAipMap = new HashMap<>();
        HashMultimap<String, Long> aipIdDataStorageIdsMap = HashMultimap.create();
        for (StorageDataFile sdf : dataFiles) {
            //lets get all data storages id
            aipIdAipMap.put(sdf.getAipEntity().getAipId(), sdf.getAip());
            sdf.getPrioritizedDataStorages().stream()
                    .forEach(pds -> aipIdDataStorageIdsMap.put(sdf.getAipEntity().getAipId(), pds.getId()));
        }
        // we have all storage data file needed to make aip with data storage id
        List<AIPWithDataStorageIds> content = new ArrayList<>();
        for (String aipId : aipIdAipMap.keySet()) {
            content.add(new AIPWithDataStorageIds(aipIdAipMap.get(aipId), aipIdDataStorageIdsMap.get(aipId)));
        }
        // now lets get information for metadata
        String pdsIdQuery = "SELECT data_storage_conf_id FROM {h-schema}ta_data_file_plugin_conf WHERE data_file_id IN "
                + "(SELECT id FROM {h-schema}t_data_file WHERE aip_ip_id IN (" + aipQueryWithoutPage + "))";
        q = em.createNativeQuery(pdsIdQuery);
        @SuppressWarnings("unchecked")
        List<Long> dataStorageIds = q.getResultList().stream().mapToLong(r -> ((BigInteger) r).longValue()).boxed()
                .collect(Collectors.toList());
        Set<PrioritizedDataStorage> dataStorages = prioritizedDataStorageRepo.findAllByIdIn(dataStorageIds);

        return new AIPPageWithDataStorages(dataStorages, content, new PagedResources.PageMetadata(content.size(),
                pageable.getPageNumber(), aipDao.countByQuery(aipQueryWithoutPage)));
    }

    @Override
    public Page<AipDataFiles> retrieveAIPDataFiles(AIPState state, Set<String> tags, OffsetDateTime fromLastUpdateDate,
            Pageable pageable) {
        // first lets get the page of aips
        // we have two cases: there is a date or not
        Page<AIP> aips;
        if (fromLastUpdateDate == null) {
            if ((tags == null) || tags.isEmpty()) {
                aips = aipDao.findAllByState(state, pageable);
            } else {
                aips = aipDao.findAll(
                                      AIPQueryGenerator.searchAIPContainingAtLeastOneTag(state, null, null,
                                                                                         new ArrayList<>(tags), null,
                                                                                         null, null, null, null),
                                      pageable);
            }
        } else {
            aips = aipDao.findAll(AIPQueryGenerator.searchAIPContainingAtLeastOneTag(state, fromLastUpdateDate, null,
                                                                                     new ArrayList<>(tags), null, null,
                                                                                     null, null, null),
                                  pageable);
        }
        // Associate data files with their AIP (=> multimap)
        List<AipDataFiles> aipDataFiles = new ArrayList<>();
        Multimap<AIP, StorageDataFile> multimap = HashMultimap.create();
        for (StorageDataFile storageDataFile : dataFileDao.findAllByAipIn(aips.getContent())) {
            // Don't take AIP data type (which in fact is AIP metadata)
            if (storageDataFile.getDataType() != DataType.AIP) {
                multimap.put(storageDataFile.getAip(), storageDataFile);
            }
        }
        // Build AipDataFiles objects (an AipDataFiles is a sort of Pair<AIP, Set<Datafiles>>)
        for (Map.Entry<AIP, Collection<StorageDataFile>> entry : multimap.asMap().entrySet()) {
            aipDataFiles.add(new AipDataFiles(entry.getKey(), entry.getValue()));
        }
        return new PageImpl<>(aipDataFiles, pageable, aips.getTotalElements());
    }

    @Override
    public Set<OAISDataObject> retrieveAIPFiles(UniformResourceName pIpId) throws ModuleException {
        Set<StorageDataFile> storageDataFiles = retrieveAIPDataFiles(pIpId);
        return storageDataFiles.stream().map(df -> {
            OAISDataObject dataObject = new OAISDataObject();
            dataObject.setRegardsDataType(df.getDataType());
            dataObject.setUrls(df.getUrls());
            dataObject.setFilename(df.getName());
            dataObject.setFileSize(df.getFileSize());
            dataObject.setChecksum(df.getChecksum());
            dataObject.setAlgorithm(df.getAlgorithm());
            return dataObject;
        }).collect(Collectors.toSet());
    }

    @Override
    public Set<StorageDataFile> retrieveAIPDataFiles(UniformResourceName pIpId) throws ModuleException {
        Optional<AIP> aip = aipDao.findOneByAipId(pIpId.toString());
        if (aip.isPresent()) {
            if (!getSecurityDelegationPlugin().hasAccess(pIpId.toString())) {
                throw new EntityOperationForbiddenException(pIpId.toString(), AIP.class, AIP_ACCESS_FORBIDDEN);
            }
            return dataFileDao.findAllByAip(aip.get());
        } else {
            throw new EntityNotFoundException(pIpId.toString(), AIP.class);
        }
    }

    @Override
    public List<String> retrieveAIPVersionHistory(UniformResourceName pIpId) {
        List<String> versions = Lists.newArrayList();
        String ipIdWithoutVersion = pIpId.toString();
        ipIdWithoutVersion = ipIdWithoutVersion.substring(0, ipIdWithoutVersion.indexOf(":V"));
        Pageable page = PageRequest.of(0, aipIterationLimit, Direction.ASC, "id");
        Page<AIP> aips;
        do {
            aips = aipDao.findAllByIpIdStartingWith(ipIdWithoutVersion, page);
            page = aips.nextPageable();
            versions.addAll(aips.getContent().stream().map(a -> a.getId().toString()).collect(Collectors.toList()));
        } while (aips.hasNext());
        return versions;
    }

    /**
     * Check that all given {@link StorageDataFile}s are dispatch into the given {@link Multimap}.<br/>
     * If it's true, nothing is done.<br/>
     * If not, the associated {@link AIP}s of given {@link StorageDataFile}s are set to {@link AIPState#STORAGE_ERROR}
     * status.
     * @param dataFilesToStore {@link StorageDataFile}s
     * @param storageWorkingSetMap {@link Multimap}<{@link PluginConfiguration}, {@link StorageDataFile}>
     * @param dispatchErrors {@link DispatchErrors} errors during files dispatch
     */
    private void checkDispatch(Set<StorageDataFile> dataFilesToStore,
            Multimap<Long, StorageDataFile> storageWorkingSetMap, DispatchErrors dispatchErrors) {
        Set<StorageDataFile> dataFilesInSubSet = storageWorkingSetMap.entries().stream().map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        if (dataFilesToStore.size() != dataFilesInSubSet.size()) {
            Set<StorageDataFile> notSubSetDataFiles = Sets.newHashSet(dataFilesToStore);
            notSubSetDataFiles.removeAll(dataFilesInSubSet);
            for (StorageDataFile prepareFailed : notSubSetDataFiles) {
                prepareFailed.setState(DataFileState.ERROR);
                prepareFailed.addFailureCause(dispatchErrors.get(prepareFailed).orElse(null));
                AIP aip = prepareFailed.getAip();
                aip.setState(AIPState.STORAGE_ERROR);
                dataFileDao.save(prepareFailed);
                save(aip, true);
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
            notifyAdmins("Some file were not associated to a data storage", email.getText(), NotificationLevel.ERROR,
                         MimeTypeUtils.TEXT_HTML);
        }
    }

    /**
     * Use the notification module in admin to create a notification for admins
     */
    private void notifyAdmins(String title, String message, NotificationLevel type, MimeType mimeType) {
        notificationClient.notify(message, title, type, mimeType, DefaultRole.ADMIN);
    }

    /**
     * This method schedules {@link StoreDataFilesJob} or {@link StoreMetadataFilesJob} to storeAndCreate given
     * {@link StorageDataFile}s.<br/>
     * A Job is scheduled for each {@link IWorkingSubset} of each {@link PluginConfiguration}.<br/>
     * @param storageWorkingSetMap List of {@link StorageDataFile} to storeAndCreate per {@link PluginConfiguration}.
     * @param storingData FALSE to store {@link DataType#AIP}, or TRUE for all other type of
     * {@link StorageDataFile}.
     */
    private void scheduleStorage(Multimap<Long, StorageDataFile> storageWorkingSetMap, boolean storingData) {
        Set<StorageDataFile> scheduledFiles = Sets.newHashSet();
        for (Long dataStorageConfId : storageWorkingSetMap.keySet()) {
            try {
                scheduledFiles.addAll(scheduleStorageForPluginConf(storageWorkingSetMap.get(dataStorageConfId),
                                                                   dataStorageConfId, storingData));
            } catch (InvalidDatastoragePluginConfException e) {
                LOGGER.error(e.getMessage(), e);
                notifyAdmins("Storage schedule", e.getMessage(), NotificationLevel.ERROR, MimeTypeUtils.TEXT_PLAIN);
            }
        }
        // now that files are given to the jobs, lets remove the source url so once stored we only have the good urls
        for (StorageDataFile dataFile : scheduledFiles) {
            dataFile.setUrls(new HashSet<>());
            dataFileDao.save(dataFile);
            em.flush();
            em.clear();
        }
    }

    /**
     *
     * @param dataFiles{@link StorageDataFile} to store
     * @param dataStorageConfId {@link PluginConfiguration} to use for storage
     * @param storingData FALSE to store {@link DataType#AIP}, or TRUE for all other type of {@link StorageDataFile}.
     * @throws InvalidDatastoragePluginConfException If {@link PluginConfiguration} is invalid
     *
     * @return Scheduled {@link StorageDataFile}s
     */
    private Set<StorageDataFile> scheduleStorageForPluginConf(Collection<StorageDataFile> dataFiles,
            Long dataStorageConfId, boolean storingData) throws InvalidDatastoragePluginConfException {
        Set<StorageDataFile> scheduledFiles = Sets.newHashSet();
        Set<IWorkingSubset> workingSubSets = getWorkingSubsets(dataFiles, dataStorageConfId,
                                                               DataStorageAccessModeEnum.STORE_MODE);
        LOGGER.trace("Preparing a job for each working subsets");
        // lets instantiate every job for every DataStorage to use
        for (IWorkingSubset workingSubset : workingSubSets) {
            // for each DataStorage we can have multiple WorkingSubSet to treat in parallel, lets storeAndCreate a
            // job for
            // each of them
            scheduledFiles.addAll(workingSubset.getDataFiles());
            Set<JobParameter> parameters = Sets.newHashSet();
            parameters.add(new JobParameter(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, dataStorageConfId));
            parameters.add(new JobParameter(AbstractStoreFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));
            if (storingData) {
                jobInfoService.createAsQueued(new JobInfo(false, StorageJobsPriority.STORE_DATA_JOB, parameters,
                        authResolver.getUser(), StoreDataFilesJob.class.getName())).getId();
            } else {
                jobInfoService.createAsQueued(new JobInfo(false, StorageJobsPriority.STORE_METADATA_JOB, parameters,
                        authResolver.getUser(), StoreMetadataFilesJob.class.getName())).getId();
            }
            // FIXME : If Jobs are interrupted, AIP is in PENDING state, DataFiles are in PENDING state
            // It is a non recoverable state.
        }
        return scheduledFiles;
    }

    /**
     * Call the {@link IDataStorage} plugins associated to the given {@link PluginConfiguration}s to create
     * {@link IWorkingSubset} of {@link StorageDataFile}s.
     * @param dataFilesToSubSet List of {@link StorageDataFile} to prepare.
     * @param dataStorageConfId {@link PluginConfiguration}
     * @return {@link IWorkingSubset}s, empty if the plugin could not be instantiated
     */
    private Set<IWorkingSubset> getWorkingSubsets(Collection<StorageDataFile> dataFilesToSubSet, Long dataStorageConfId,
            DataStorageAccessModeEnum accessMode) throws InvalidDatastoragePluginConfException {

        IDataStorage<IWorkingSubset> storage = null;
        try {
            if (pluginService.canInstantiate(dataStorageConfId)) {
                storage = pluginService.getPlugin(dataStorageConfId);
            } else {
                throw new InvalidDatastoragePluginConfException(dataStorageConfId);
            }
        } catch (ModuleException e) {
            throw new InvalidDatastoragePluginConfException(dataStorageConfId, e);
        }

        LOGGER.debug("Getting working subsets for data storage of id {}", dataStorageConfId);
        WorkingSubsetWrapper<?> workingSubsetWrapper = storage.prepare(dataFilesToSubSet, accessMode);
        @SuppressWarnings("unchecked")
        Set<IWorkingSubset> workingSubSets = (Set<IWorkingSubset>) workingSubsetWrapper.getWorkingSubSets();
        LOGGER.debug("{} data objects were dispatched into {} working subsets", dataFilesToSubSet.size(),
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
                StorageDataFile dataFile = rejected.getKey();
                dataFile.setState(DataFileState.ERROR);
                dataFile.addFailureCause(rejected.getValue());
                AIP aip = dataFile.getAip();
                aip.setState(AIPState.STORAGE_ERROR);
                dataFileDao.save(dataFile);
                save(aip, true);
            }
            // lets prepare the notification message
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("dataFilesMap", workingSubsetWrapper.getRejectedDataFiles());
            try {
                dataMap.put("dataStorage", pluginService.getPluginConfiguration(dataStorageConfId));
            } catch (EntityNotFoundException e1) {
                LOGGER.error(e1.getMessage(), e1);
            }
            // lets use the template service to get our message
            SimpleMailMessage email;
            try {
                email = templateService.writeToEmail(TemplateServiceConfiguration.NOT_SUBSETTED_DATA_FILES_CODE,
                                                     dataMap);
            } catch (EntityNotFoundException e) {
                throw new MaintenanceException(e.getMessage(), e);
            }
            notifyAdmins("Some file were not handled by a data storage", email.getText(), NotificationLevel.ERROR,
                         MimeTypeUtils.TEXT_HTML);
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
            LOGGER.error(e.getMessage(), e);
            notifyAdmins("Allocation Strategy miss configured", e.getMessage(), NotificationLevel.ERROR,
                         MimeTypeUtils.TEXT_PLAIN);
            throw e;
        }
    }

    private PluginConfiguration getAllocationStrategyConfiguration() {
        // Lets retrieve active configurations of IAllocationStrategy
        List<PluginConfiguration> allocationStrategies = pluginService
                .getPluginConfigurationsByType(IAllocationStrategy.class);
        List<PluginConfiguration> activeAllocationStrategies = allocationStrategies.stream()
                .filter(PluginConfiguration::isActive).collect(Collectors.toList());
        // System can only handle one active configuration of IAllocationStrategy
        if (activeAllocationStrategies.size() != 1) {
            IllegalStateException e = new IllegalStateException(
                    "The application needs one and only one active configuration of "
                            + IAllocationStrategy.class.getName());
            notifyAdmins("No active Allocation Strategy", e.getMessage(), NotificationLevel.ERROR,
                         MimeTypeUtils.TEXT_PLAIN);
            LOGGER.error(e.getMessage(), e);
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
            LOGGER.error(e.getMessage(), e);
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
            for (StorageDataFile dataFile : storageWorkingSetMap.values()) {
                AIP aip = dataFile.getAip();
                aip.setState(AIPState.STORING_METADATA);
                // StorageDataFile provided does not exists in db yet at this step see {@link WriteAIPMetadataJob}
                dataFileDao.save(dataFile);
                save(aip, true);
                em.flush();
                em.clear();
            }
        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            notifyAdmins("Could not schedule metadata storage",
                         "Metadata storage could not be realized because an error occured. Please check the logs",
                         NotificationLevel.ERROR, MimeTypeUtils.TEXT_PLAIN);
        }
    }

    /**
     * Retrieve all {@link StorageDataFile} ready to be stored.
     * @param dataFileLimit maximum number of {@link StorageDataFile} to return
     * @return data files to store
     */
    private Set<AIP> getMetadataFilesToStore() {
        Page<AIP> page = aipDao.findAllByState(AIPState.DATAFILES_STORED,
                                               PageRequest.of(0, aipIterationLimit, Direction.ASC, "id"));
        Set<AIP> aips = page.getContent().stream().collect(Collectors.toSet());
        LOGGER.trace("[METADATA STORE] Number of AIP metadata {} to schedule for storage.", aips.size());
        return aips;
    }

    @Override
    public List<RejectedAip> applyRetryChecks(Set<String> aipIpIds) {
        List<RejectedAip> rejectedAips = Lists.newArrayList();
        for (String ipId : aipIpIds) {
            List<String> rejectionReasons = Lists.newArrayList();
            if (!aipDao.findOneByAipId(ipId).isPresent()) {
                rejectionReasons.add(String.format("AIP with ip id %s does not exists", ipId));
                rejectedAips.add(new RejectedAip(ipId, rejectionReasons));
            }
        }
        return rejectedAips;
    }

    @Override
    public Page<AIP> retrieveAipsByTag(String tag, Pageable page) {
        return aipDao.findAllByTag(tag, page);
    }

    @Override
    public List<Event> retrieveAIPHistory(UniformResourceName ipId) throws ModuleException {
        Optional<AIP> aip = aipDao.findOneByAipId(ipId.toString());
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
        return aipDao.findAllByAipIdIn(ipIds);
    }

    @Override
    public AIP retrieveAip(String aipId) throws EntityNotFoundException {
        return aipDao.findOneByAipId(aipId).orElseThrow(() -> new EntityNotFoundException(aipId, AIP.class));
    }

    @Override
    public int handleUpdateRequests() throws ModuleException {
        int nbAipHandled = 0;
        Pageable page = PageRequest.of(0, 100, Direction.ASC, "id");
        Page<AIPUpdateRequest> updatePage;
        do {
            // Retrieve all update requests.
            updatePage = aipUpdateRequestRepo.findAll(page);
            for (AIPUpdateRequest request : updatePage) {
                // Retrieve the associated AIP to update
                Optional<AIPEntity> oAIP = aipEntityRepository.findOneByAipId(request.getAipId());
                if (oAIP.isPresent()) {
                    if (oAIP.get().getState() == AIPState.STORED) {
                        // If associated AIP is in STORED state, run the update request
                        Optional<AIP> oAipUpdated = updateAip(request.getAipId(), request.getAip(),
                                                              request.getUpdateMessage());
                        // If request is well handled, delete the update request.
                        oAipUpdated.ifPresent(aip -> aipUpdateRequestRepo.delete(request));
                    } else {
                        LOGGER.debug("AIP {} update request is delayed cause the AIP is still in a storing process",
                                     oAIP.get().getProviderId());
                    }
                } else {
                    // AIP doesn't exists anymore, delete the update request.
                    aipUpdateRequestRepo.delete(request);
                }
            }
            page = updatePage.nextPageable();
        } while (updatePage.hasNext());

        return nbAipHandled;
    }

    @Override
    public Optional<AIP> updateAip(String ipId, AIP newAip, String updateMessage)
            throws EntityNotFoundException, EntityInconsistentIdentifierException {
        Optional<AIP> oAipToUpdate = aipDao.findOneByAipId(ipId);
        // first lets check for issues
        if (!oAipToUpdate.isPresent()) {
            throw new EntityNotFoundException(ipId, AIP.class);
        }
        AIP aipToUpdate = oAipToUpdate.get();
        if (aipToUpdate.getState() != AIPState.STORED) {
            LOGGER.info("AIP to update {}, is already handled by a storage process. The requested udpdate is delayed.",
                        aipToUpdate.getProviderId());
            addNewAIPUpdateRequest(newAip, updateMessage);
            return Optional.empty();
        }
        if (newAip.getId() == null) {
            throw new EntityNotFoundException("give updated AIP has no id!");
        }
        if (!aipToUpdate.getId().toString().equals(newAip.getId().toString())) {
            throw new EntityInconsistentIdentifierException(ipId, newAip.getId().toString(), AIP.class);
        }
        LOGGER.debug(String.format("[METADATA UPDATE] updating metadata of aip %s", ipId));
        // now that requirement are meant, lets update the old one
        AIPBuilder newAIPBuilder = new AIPBuilder(aipToUpdate);
        // Only PDI and descriptive information can be updated
        PreservationDescriptionInformation newAipPdi = newAip.getProperties().getPdi();
        // Provenance Information
        // first lets merge the events
        newAIPBuilder.getPDIBuilder()
                .addProvenanceInformationEvents(newAip.getHistory().toArray(new Event[newAip.getHistory().size()]));
        // second lets merge other provenance informations
        Map<String, Object> additionalProvenanceInfoMap;
        if ((additionalProvenanceInfoMap = newAipPdi.getProvenanceInformation().getAdditional()) != null) {
            for (Map.Entry<String, Object> additionalProvenanceEntry : additionalProvenanceInfoMap.entrySet()) {
                newAIPBuilder.getPDIBuilder().addAdditionalProvenanceInformation(additionalProvenanceEntry.getKey(),
                                                                                 additionalProvenanceEntry.getValue());
            }
        }

        // third lets handle those "special" provenance information
        newAIPBuilder.getPDIBuilder().setFacility(newAipPdi.getProvenanceInformation().getFacility());
        newAIPBuilder.getPDIBuilder().setDetector(newAipPdi.getProvenanceInformation().getDetector());
        newAIPBuilder.getPDIBuilder().setFilter(newAipPdi.getProvenanceInformation().getFilter());
        newAIPBuilder.getPDIBuilder().setInstrument(newAipPdi.getProvenanceInformation().getInstrument());
        newAIPBuilder.getPDIBuilder().setProposal(newAipPdi.getProvenanceInformation().getProposal());
        // Context Information
        // first tags
        // remove all existing tags
        newAIPBuilder.getPDIBuilder().removeTags(newAIPBuilder.getPDIBuilder().build().getTags()
                .toArray(new String[newAIPBuilder.getPDIBuilder().build().getTags().size()]));
        // add the new tags
        if (!newAip.getTags().isEmpty()) {
            newAIPBuilder.getPDIBuilder().addTags(newAip.getTags().toArray(new String[newAip.getTags().size()]));
        }
        // now the rest of them
        Map<String, Object> contextInformationMap;
        if ((contextInformationMap = newAipPdi.getContextInformation()) != null) {
            for (Map.Entry<String, Object> contextEntry : contextInformationMap.entrySet()) {
                // tags have their specific handling
                if (!contextEntry.getKey().equals(PreservationDescriptionInformation.CONTEXT_INFO_TAGS_KEY)) {
                    newAIPBuilder.getPDIBuilder().addContextInformation(contextEntry.getKey(), contextEntry.getValue());
                }
            }
        }
        // reference information
        Map<String, String> referenceInformationMap;
        if ((referenceInformationMap = newAipPdi.getReferenceInformation()) != null) {
            for (Map.Entry<String, String> refEntry : referenceInformationMap.entrySet()) {
                // tags have their specific handling
                newAIPBuilder.getPDIBuilder().addContextInformation(refEntry.getKey(), refEntry.getValue());
            }
        }
        // fixity information
        Map<String, Object> fixityInformationMap;
        if ((fixityInformationMap = newAipPdi.getFixityInformation()) != null) {
            for (Map.Entry<String, Object> fixityEntry : fixityInformationMap.entrySet()) {
                // tags have their specific handling
                newAIPBuilder.getPDIBuilder().addContextInformation(fixityEntry.getKey(), fixityEntry.getValue());
            }
        }
        // Access Right information
        if (!Strings.isNullOrEmpty(newAipPdi.getAccessRightInformation().getDataRights())) {
            newAIPBuilder.getPDIBuilder()
                    .setAccessRightInformation(newAipPdi.getAccessRightInformation().getLicence(),
                                               newAipPdi.getAccessRightInformation().getDataRights(),
                                               newAipPdi.getAccessRightInformation().getPublicReleaseDate());
        }

        // descriptive information
        newAIPBuilder.build().getProperties().getDescriptiveInformation().clear();
        Map<String, Object> descriptiveInformationMap;
        if ((descriptiveInformationMap = newAip.getProperties().getDescriptiveInformation()) != null) {
            for (Map.Entry<String, Object> descriptiveEntry : descriptiveInformationMap.entrySet()) {
                newAIPBuilder.addDescriptiveInformation(descriptiveEntry.getKey(), descriptiveEntry.getValue());
            }
        }

        // Create new DataStorageFile to store, update DataStorageFile to delete.
        boolean isNewFilesToStore = handleContentInformationUpdate(newAIPBuilder, newAip, aipToUpdate);

        // Add update event
        newAIPBuilder.addEvent(EventType.UPDATE.toString(), updateMessage, OffsetDateTime.now());
        // now that all updates are set into the builder, lets build and save the updatedAip.

        AIP updatedAip = newAIPBuilder.build();
        if (isNewFilesToStore) {
            // AIP is set to VALID state to be handled for store process new data files and metadata
            updatedAip.setState(AIPState.VALID);
        } else {
            // AIP is set to DATAFILES_STORED state to be handled for store process metadata
            updatedAip.setState(AIPState.DATAFILES_STORED);
        }
        LOGGER.debug(String.format("[METADATA UPDATE] Update of aip %s metadata done", ipId));
        LOGGER.trace(String.format("[METADATA UPDATE] Updated aip : %s", gson.toJson(updatedAip)));
        return Optional.ofNullable(save(updatedAip, false));
    }

    /**
     * Add a new update request pending for the given AIP.
     * @param aipToUpdate
     * @param updateMessage
     */
    private void addNewAIPUpdateRequest(AIP aipToUpdate, String updateMessage) {
        Optional<AIPUpdateRequest> oUpdateRequest = aipUpdateRequestRepo.findOneByAipId(aipToUpdate.getId().toString());
        if (oUpdateRequest.isPresent()) {
            AIPUpdateRequest updateRequest = oUpdateRequest.get();
            updateRequest.setAip(aipToUpdate);
            updateRequest.setUpdateMessage(updateMessage);
            aipUpdateRequestRepo.save(updateRequest);
        } else {
            aipUpdateRequestRepo.save(new AIPUpdateRequest(aipToUpdate, updateMessage));
        }
    }

    @Override
    public Set<StorageDataFile> deleteAip(String ipId) throws ModuleException {
        Optional<AIP> toBeDeletedOpt = aipDao.findOneByAipId(ipId);
        if (toBeDeletedOpt.isPresent()) {
            AIP toBeDeleted = toBeDeletedOpt.get();
            return deleteAip(toBeDeleted);
        }
        return Sets.newHashSet();
    }

    @Override
    public Set<StorageDataFile> deleteAip(AIP toBeDeleted) throws ModuleException {
        long methodStart = System.currentTimeMillis();
        Set<StorageDataFile> notSuppressible = Sets.newHashSet();
        long daoFindStart = System.currentTimeMillis();
        Set<StorageDataFile> dataFilesWithMetadata = dataFileDao.findAllByAip(toBeDeleted);
        long daoFindEnd = System.currentTimeMillis();
        String toBeDeletedIpId = toBeDeleted.getId().toString();
        LOGGER.trace("Finding {} datafile for aip {} took {} ms", dataFilesWithMetadata.size(), toBeDeletedIpId,
                     daoFindEnd - daoFindStart);
        Set<StorageDataFile> dataFilesWithoutMetadata = dataFilesWithMetadata.stream()
                .filter(df -> !DataType.AIP.equals(df.getDataType())).collect(Collectors.toSet());
        for (StorageDataFile dataFile : dataFilesWithoutMetadata) {
            // If dataFile is in error state and no storage succeeded. So no urls are associated to the dataFile.
            if (dataFile.getState().equals(DataFileState.ERROR) && dataFile.getUrls().isEmpty()) {
                // we do not do remove immediately because the aip metadata has to be updated first
                // and the logic is already implemented into DataStorageEventHandler
                publisher.publish(new DataStorageEvent(dataFile, StorageAction.DELETION, StorageEventType.SUCCESSFULL,
                        null, null));
            } else {
                if (dataFile.getState().equals(DataFileState.PENDING)) {
                    notSuppressible.add(dataFile);
                } else {
                    // we order deletion of a file if and only if no other aip references the same file
                    long daoFindOtherDataFileStart = System.currentTimeMillis();
                    long nbDataFilesWithSameFile = dataFileDao
                            .countByChecksumAndStorageDirectory(dataFile.getChecksum(), dataFile.getStorageDirectory());
                    long daoFindOtherDataFileEnd = System.currentTimeMillis();
                    LOGGER.trace("Counting {} other datafile with checksum {} took {} ms", nbDataFilesWithSameFile,
                                 dataFile.getChecksum(), daoFindOtherDataFileEnd - daoFindOtherDataFileStart);
                    if (nbDataFilesWithSameFile == 1) {
                        // add to datafiles that should be removed
                        dataFile.setState(DataFileState.TO_BE_DELETED);
                        dataFileDao.save(dataFile);
                    } else {
                        // if other datafiles are referencing a file, we just remove the data file from the
                        // database.
                        // we do not do remove immediately because the aip metadata has to be updated first
                        // and the logic is already implemented into DataStorageEventHandler
                        publisher.publish(new DataStorageEvent(dataFile, StorageAction.DELETION,
                                StorageEventType.SUCCESSFULL, null, null));
                    }
                }
            }
        }
        // schedule removal of data and metadata
        long initiateBuilder = System.currentTimeMillis();
        AIPBuilder toBeDeletedBuilder = new AIPBuilder(toBeDeleted);
        long endInitiateBuilder = System.currentTimeMillis();
        LOGGER.trace("Initiating AIPBuilder for {} took {} ms", toBeDeletedIpId, endInitiateBuilder - initiateBuilder);
        toBeDeletedBuilder
                .addEvent(EventType.DELETION.name(),
                          "AIP deletion was requested, AIP is considered deleted until its removal from archives",
                          OffsetDateTime.now());
        long endAddEvent = System.currentTimeMillis();
        LOGGER.trace("Adding deletion event to AIP {} took {} ms", toBeDeletedIpId, endAddEvent - endInitiateBuilder);
        toBeDeleted = toBeDeletedBuilder.build();
        long endRebuild = System.currentTimeMillis();
        LOGGER.trace("Rebuilding AIP {} took {} ms", toBeDeletedIpId, endRebuild - endAddEvent);
        toBeDeleted.setState(AIPState.DELETED);
        long endChangeState = System.currentTimeMillis();
        LOGGER.trace("Changing AIP {} state to DELETED took {} ms", toBeDeletedIpId, endChangeState - endRebuild);
        save(toBeDeleted, true);
        long endSave = System.currentTimeMillis();
        LOGGER.trace("Saving AIP {} to DB took {} ms", toBeDeletedIpId, endSave - endChangeState);
        long methodEnd = System.currentTimeMillis();
        LOGGER.trace("Deleting AIP {} took {} ms", toBeDeletedIpId, methodEnd - methodStart);
        return notSuppressible;
    }

    @Override
    public Map<StorageDataFile, String> deleteFilesFromDataStorage(Collection<String> ipIds, Long dataStorageId) {
        Set<StorageDataFile> filesToDelete = dataFileDao.findAllByAipIpIdIn(ipIds);
        // for all these files, lets check if there is still at lease one data storage that references it
        Map<StorageDataFile, String> undeletableFileCauseMap = new HashMap<>();
        Optional<PrioritizedDataStorage> oDataStorage = prioritizedDataStorageRepo.findById(dataStorageId);
        if (oDataStorage.isPresent()) {
            PrioritizedDataStorage dataStorage;
            dataStorage = oDataStorage.get();
            for (StorageDataFile fileToDelete : filesToDelete) {
                if (!fileToDelete.getPrioritizedDataStorages().contains(dataStorage)) {
                    undeletableFileCauseMap
                            .put(fileToDelete,
                                 String.format("File %s from AIP %s is not handled by Data storage %s",
                                               fileToDelete.getChecksum(), fileToDelete.getAipEntity().getAipId(),
                                               dataStorage.getDataStorageConfiguration().getLabel()));
                } else if (fileToDelete.getPrioritizedDataStorages().size() == 1) {
                    undeletableFileCauseMap
                            .put(fileToDelete,
                                 String.format("Data storage %s is the last one for file %s from AIP %s. "
                                         + "Removal from last data storage is forbidden.",
                                               dataStorage.getDataStorageConfiguration().getLabel(),
                                               fileToDelete.getChecksum(), fileToDelete.getAipEntity().getAipId()));
                }
            }
            filesToDelete.removeAll(undeletableFileCauseMap.keySet());
            // now, lets handle files that have to be ONLINE
            Set<StorageDataFile> onlineMandatoryFiles = filesToDelete.stream()
                    .filter(StorageDataFile::isOnlineMandatory).collect(Collectors.toSet());
            for (StorageDataFile onlineMandatoryFile : onlineMandatoryFiles) {
                if (onlineMandatoryFile.getPrioritizedDataStorages().stream()
                        .filter(pds -> pds.getDataStorageType() == DataStorageType.ONLINE).count() == 1) {
                    undeletableFileCauseMap
                            .put(onlineMandatoryFile,
                                 String.format("Data storage %s is the last ONLINE one for file %s from AIP %s. "
                                         + "Removal from last ONLINE data storage is forbidden on %s.",
                                               dataStorage.getDataStorageConfiguration().getLabel(),
                                               onlineMandatoryFile.getChecksum(),
                                               onlineMandatoryFile.getAipEntity().getAipId(),
                                               dataStorage.getDataStorageType()));
                    filesToDelete.remove(onlineMandatoryFile);
                }
            }
            // to avoid concurrency issues, lets remove that data storage from the file right now
            final PrioritizedDataStorage finalDataStorage = dataStorage; // thanks to lambda restriction
            filesToDelete.forEach(sdf -> {
                sdf.increaseNotYetDeletedBy();
                sdf.getPrioritizedDataStorages().remove(finalDataStorage);
            });
            dataFileDao.save(filesToDelete);
            // now, lets plan a job to delete those files
            try {
                scheduleFileDeletion(filesToDelete, dataStorageId);
            } catch (InvalidDatastoragePluginConfException e) {
                filesToDelete.forEach(sdf -> undeletableFileCauseMap
                        .put(sdf,
                             String.format("Deletion job could not be created for the following reason: %s. %n"
                                     + "We could not delete file %s from AIP %s", e.getMessage(), sdf.getName(),
                                           sdf.getAipEntity().getAipId())));
            }
        } else {
            filesToDelete.forEach(sdf -> undeletableFileCauseMap.put(sdf, String
                    .format("Data Storage %s does not exist anymore. " + "We could not delete file %s from AIP %s",
                            dataStorageId, sdf.getName(), sdf.getAipEntity().getAipId())));
        }
        // now that everything has been schedule, lets create a notification for all undeletables
        // lets prepare the notification message
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("dataFilesMap", undeletableFileCauseMap);
        dataMap.put("dataStorage", oDataStorage.isPresent() ? oDataStorage.get() : dataStorageId);
        // lets use the template service to get our message
        SimpleMailMessage email;
        try {
            email = templateService.writeToEmail(TemplateServiceConfiguration.UNDELETABLES_DATA_FILES_CODE, dataMap);
        } catch (EntityNotFoundException e) {
            throw new MaintenanceException(e.getMessage(), e);
        }
        notifyAdmins("REGARDS - Some files could not be deleted from data storage", email.getText(),
                     NotificationLevel.WARNING, MimeTypeUtils.TEXT_HTML);
        // now that we are done with pure removal logic, lets create an update request for the AIPs to write changes
        // made to DataFiles.
        return undeletableFileCauseMap;
    }

    @Override
    public void deleteFilesFromDataStorageByQuery(AIPQueryFilters filters, Long dataStorageId) {
        // prevent the job to remove entities created after this call
        if (filters.getTo() == null) {
            filters.setTo(OffsetDateTime.now());
        }
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(DeleteFilesFromDataStorageJob.FILTER_PARAMETER_NAME, filters));
        parameters.add(new JobParameter(DeleteFilesFromDataStorageJob.DATA_STORAGE_ID_PARAMETER_NAME, dataStorageId));
        JobInfo jobInfo = new JobInfo(false, StorageJobsPriority.METADATA_DELETION_JOB, parameters,
                authResolver.getUser(), DeleteFilesFromDataStorageJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
        LOGGER.debug("New DeleteFilesFromDataStorageJob job scheduled uuid={}", jobInfo.getId().toString());
    }

    @Override
    public Long doDelete() {
        Pageable page = PageRequest.of(0, aipIterationLimit, Direction.ASC, "id");
        Page<StorageDataFile> pageToDelete;
        do {
            pageToDelete = dataFileDao.findPageByState(DataFileState.TO_BE_DELETED, page);
            try {
                scheduleAIPDeletion(pageToDelete.getContent());
            } catch (ModuleException e) {
                LOGGER.error("ERROR occured during deletion scheduling of datafiles.", e);
            }
            page = pageToDelete.nextPageable();
        } while (pageToDelete.hasNext());

        return pageToDelete.getTotalElements();
    }

    @Override
    public Set<StorageDataFile> deleteAipFromSip(UniformResourceName sipId) throws ModuleException {
        Set<StorageDataFile> notSuppressible = new HashSet<>();
        for (AIP aip : aipDao.findAllBySipId(sipId.toString())) {
            notSuppressible.addAll(deleteAip(aip.getId().toString()));
        }
        return notSuppressible;
    }

    @Override
    public void addTags(String ipId, Set<String> tagsToAdd)
            throws EntityNotFoundException, EntityInconsistentIdentifierException, EntityOperationForbiddenException {
        AIP toUpdate = retrieveAip(ipId);
        addTags(toUpdate, tagsToAdd);
    }

    @Override
    public void addTags(AIP toUpdate, Set<String> tagsToAdd)
            throws EntityNotFoundException, EntityInconsistentIdentifierException {
        AIPBuilder updateBuilder = new AIPBuilder(toUpdate);
        updateBuilder.addTags(tagsToAdd.toArray(new String[tagsToAdd.size()]));
        toUpdate = updateBuilder.build();
        String updateMessage = String.format("Add tags [%s].", String.join(" , ", tagsToAdd));
        updateAip(toUpdate.getId().toString(), toUpdate, updateMessage);
    }

    @Override
    public void removeTags(String ipId, Set<String> tagsToRemove)
            throws EntityNotFoundException, EntityInconsistentIdentifierException, EntityOperationForbiddenException {
        AIP toUpdate = retrieveAip(ipId);
        removeTags(toUpdate, tagsToRemove);
    }

    @Override
    public void removeTags(AIP toUpdate, Set<String> tagsToRemove)
            throws EntityNotFoundException, EntityInconsistentIdentifierException {
        AIPBuilder updateBuilder = new AIPBuilder(toUpdate);
        updateBuilder.removeTags(tagsToRemove.toArray(new String[tagsToRemove.size()]));
        toUpdate = updateBuilder.build();
        String updateMessage = String.format("Remove tags [%s].", String.join(" , ", tagsToRemove));
        updateAip(toUpdate.getId().toString(), toUpdate, updateMessage);
    }

    private void scheduleAIPDeletion(Collection<StorageDataFile> dataFilesToDelete) throws ModuleException {
        // when we delete DataFiles, we have to get the DataStorages to use thanks to DB informations
        Multimap<Long, StorageDataFile> dataStorageDataFileMultimap = HashMultimap.create();
        LOGGER.debug("Start schedule AIP deletion for {} StorageDataFiles", dataFilesToDelete.size());
        for (StorageDataFile toDelete : dataFilesToDelete) {
            toDelete.getPrioritizedDataStorages()
                    .forEach(dataStorage -> dataStorageDataFileMultimap.put(dataStorage.getId(), toDelete));
            toDelete.setState(DataFileState.DELETION_PENDING);
            dataFileDao.save(toDelete);
            em.flush();
            em.clear();
        }
        Set<UUID> jobIds = Sets.newHashSet();
        for (Long dataStorageConfId : dataStorageDataFileMultimap.keySet()) {
            try {
                jobIds.addAll(scheduleDeletionJob(dataStorageDataFileMultimap, dataStorageConfId));
            } catch (InvalidDatastoragePluginConfException e) {
                LOGGER.error(e.getMessage(), e);
                notificationClient.notify(e.getMessage(), "Storage - Schedule deletion error", NotificationLevel.ERROR,
                                          DefaultRole.ADMIN);
            }
        }
    }

    private void scheduleFileDeletion(Collection<StorageDataFile> dataFilesToDelete, Long dataStorageConfId)
            throws InvalidDatastoragePluginConfException {
        Multimap<Long, StorageDataFile> dataStorageDataFileMultimap = HashMultimap.create();
        LOGGER.debug("Start schedule file deletion for {} StorageDataFiles", dataFilesToDelete.size());
        dataStorageDataFileMultimap.putAll(dataStorageConfId, dataFilesToDelete);
        for (StorageDataFile toDelete : dataFilesToDelete) {
            toDelete.setState(DataFileState.PARTIAL_DELETION_PENDING);
            dataFileDao.save(toDelete);
            em.flush();
            em.clear();
        }
        scheduleDeletionJob(dataStorageDataFileMultimap, dataStorageConfId);
    }

    private Set<UUID> scheduleDeletionJob(Multimap<Long, StorageDataFile> dataStorageDataFileMultimap,
            Long dataStorageConfId) throws InvalidDatastoragePluginConfException {
        Set<UUID> jobIds = new HashSet<>();
        Set<IWorkingSubset> workingSubSets = getWorkingSubsets(dataStorageDataFileMultimap.get(dataStorageConfId),
                                                               dataStorageConfId,
                                                               DataStorageAccessModeEnum.DELETION_MODE);
        LOGGER.debug("Schedule deletion for {} working subsets", workingSubSets.size());
        // lets instantiate every job for every DataStorage to use
        for (IWorkingSubset workingSubset : workingSubSets) {
            LOGGER.debug("Schedule deletion for working subset with {} StorageDataFiles",
                         workingSubset.getDataFiles().size());
            // for each DataStorage we can have multiple WorkingSubSet to treat in parallel, lets create a
            // job for each of them
            Set<JobParameter> parameters = Sets.newHashSet();
            parameters.add(new JobParameter(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, dataStorageConfId));
            parameters.add(new JobParameter(AbstractStoreFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));
            jobIds.add(jobInfoService.createAsQueued(new JobInfo(false, StorageJobsPriority.DELETION_JOB, parameters,
                    authResolver.getUser(), DeleteDataFilesJob.class.getName())).getId());
        }
        return jobIds;
    }

    @Override
    public Pair<StorageDataFile, InputStream> getAIPDataFile(String pAipId, String pChecksum)
            throws ModuleException, IOException {
        // First find the AIP
        Optional<AIP> oaip = aipDao.findOneByAipId(pAipId);
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
                            .filter(pds -> pds.getDataStorageType().equals(DataStorageType.ONLINE)
                                    && pds.getDataStorageConfiguration().isActive())
                            .sorted().findFirst();
                    if (onlinePrioritizedDataStorageOpt.isPresent()) {
                        @SuppressWarnings("rawtypes")
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

    @Override
    public int removeDeletedAIPMetadatas() {
        Page<AIP> aips = aipDao.findAllByStateService(AIPState.DELETED,
                                                      PageRequest.of(0, aipIterationLimit, Direction.ASC, "id"));
        for (AIP aip : aips) {
            // lets count the number of datafiles per aip:
            // if there is none:
            long nbDataFile = dataFileDao.countByAip(aip);
            if (nbDataFile == 0) {
                // Error case recovering. If AIP is in DELETED state and there is no DataFile linked to it, we can
                // delete aip from database.
                LOGGER.warn("Delete AIP {} which is not associated to any datafile.", aip.getId());
                publisher.publish(new AIPEvent(aip));
                aipDao.remove(aip);
            } else {
                // if there is one, it must be the metadata
                if (nbDataFile == 1) {
                    Set<StorageDataFile> metadatas = dataFileDao.findByAipAndType(aip, DataType.AIP);
                    if (!metadatas.isEmpty()) {
                        for (StorageDataFile meta : metadatas) {
                            meta.setState(DataFileState.TO_BE_DELETED);
                            dataFileDao.save(meta);
                        }
                    } else {
                        LOGGER.error("AIP {} is in state {} and its metadata file cannot be found in DB while it has still "
                                + "some file associated. Database coherence seems shady.", aip.getId().toString(),
                                     aip.getState());
                    }
                }
                //if there is more than one then deletion has not been executed yet, do nothing
            }
        }
        return aips.getNumberOfElements();
    }

    @Override
    public void deleteAIPsByQuery(AIPQueryFilters filters) {
        // prevent the job to remove entities created after this call
        if (filters.getTo() == null) {
            filters.setTo(OffsetDateTime.now());
        }
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(DeleteAIPsJob.FILTER_PARAMETER_NAME, filters));
        JobInfo jobInfo = new JobInfo(false, StorageJobsPriority.METADATA_DELETION_JOB, parameters,
                authResolver.getUser(), DeleteAIPsJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
        LOGGER.debug("New DeleteAIPsJob job scheduled uuid={}", jobInfo.getId().toString());
    }

    @Override
    public List<RejectedSip> deleteAipFromSips(Set<String> sipIds) throws ModuleException {
        List<RejectedSip> notHandledSips = new ArrayList<>();
        //to avoid memory issues with hibernate, lets paginate the select and then evict the entities from the cache
        Pageable page = PageRequest.of(0, 500, Direction.ASC, "id");
        long daofindPageStart = System.currentTimeMillis();
        Page<AIP> aipPage = aipDao.findPageBySipIdIn(sipIds, page);
        long daofindPageEnd = System.currentTimeMillis();
        LOGGER.trace("Finding {} aip from {} sip ids took {} ms", aipPage.getNumberOfElements(), sipIds.size(),
                     daofindPageEnd - daofindPageStart);
        while (aipPage.hasContent()) {
            // while there is aip to delete, lets delete them and get the new page at the end
            Map<String, Set<AIP>> aipsPerSip = aipPage.getContent().stream()
                    .collect(Collectors.toMap(aip -> aip.getSipId().get(), Sets::newHashSet, Sets::union));
            for (String sipId : aipsPerSip.keySet()) {
                long timeStart = System.currentTimeMillis();
                Set<AIP> aipsToDelete = aipsPerSip.get(sipId);
                Set<StorageDataFile> notSuppressible = new HashSet<>();
                for (AIP aip : aipsToDelete) {
                    notSuppressible.addAll(deleteAip(aip));
                }
                long timeEnd = System.currentTimeMillis();
                LOGGER.trace("deleting sip {} took {} ms", sipId, timeEnd - timeStart);
                if (!notSuppressible.isEmpty()) {
                    StringJoiner sj = new StringJoiner(", ",
                            "This sip could not be deleted because at least one of its aip file has not be handle by the storage process: ",
                            ".");
                    notSuppressible.stream().map(StorageDataFile::getAipEntity)
                            .forEach(aipEntity -> sj.add(aipEntity.getAipId()));
                    notHandledSips.add(new RejectedSip(sipId, sj.toString()));
                }
            }
            // Before getting the next page, lets evict actual entities from cache
            em.flush();
            em.clear();
            // now that hibernate cache has been cleared, lets get the next page
            page = page.next();
            daofindPageStart = System.currentTimeMillis();
            aipPage = aipDao.findPageBySipIdIn(sipIds, page);
            daofindPageEnd = System.currentTimeMillis();
            LOGGER.trace("Finding {} aip from {} sip ids took {} ms", aipPage.getNumberOfElements(), sipIds.size(),
                         daofindPageEnd - daofindPageStart);
        }
        return notHandledSips;
    }

    @Override
    public boolean removeTagsByQuery(RemoveAIPTagsFilters filters) {
        Long jobsScheduled = jobInfoService.retrieveJobsCount(UpdateAIPsTagJob.class.getName(), JobStatus.QUEUED,
                                                              JobStatus.RUNNING);
        if (jobsScheduled > 0) {
            LOGGER.debug("Cannot remove tags on AIPs : {} similar job(s) is(are) already running on this tenant",
                         jobsScheduled);
            return false;
        }
        // prevent the job to remove tags to entities created after this call
        if (filters.getTo() == null) {
            filters.setTo(OffsetDateTime.now());
        }
        UpdateAIPsTagJobType updateType = UpdateAIPsTagJobType.REMOVE;
        JobParameter filterParameter = new JobParameter(UpdateAIPsTagJob.FILTER_PARAMETER_NAME, filters);
        scheduleJobToUpdateTags(updateType, filterParameter);
        return true;
    }

    @Override
    public boolean addTagsByQuery(AddAIPTagsFilters filters) {
        Long jobsScheduled = jobInfoService.retrieveJobsCount(UpdateAIPsTagJob.class.getName(), JobStatus.QUEUED,
                                                              JobStatus.RUNNING);
        if (jobsScheduled > 0) {
            LOGGER.debug("Cannot add tags on AIPs : {} similar job(s) is(are) already running on this tenant",
                         jobsScheduled);
            return false;
        }
        // prevent the job to add tags to entities created after this call
        if (filters.getTo() == null) {
            filters.setTo(OffsetDateTime.now());
        }
        UpdateAIPsTagJobType updateType = UpdateAIPsTagJobType.ADD;
        JobParameter filterParameter = new JobParameter(UpdateAIPsTagJob.FILTER_PARAMETER_NAME, filters);
        scheduleJobToUpdateTags(updateType, filterParameter);
        return true;
    }

    /**
     * Save a new job to process the AIP update
     * @param updateType type of update (add/remove)
     * @param filterParameter user query filters
     */
    private void scheduleJobToUpdateTags(UpdateAIPsTagJobType updateType, JobParameter filterParameter) {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(filterParameter);
        parameters.add(new JobParameter(UpdateAIPsTagJob.UPDATE_TYPE_PARAMETER_NAME, updateType));
        JobInfo jobInfo = new JobInfo(false, StorageJobsPriority.UPDATE_TAGS_JOB, parameters, authResolver.getUser(),
                UpdateAIPsTagJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
        LOGGER.debug("New job scheduled uuid={}", jobInfo.getId().toString());
    }

    @Override
    public List<String> retrieveAIPTagsByQuery(AIPQueryFilters request) {
        return aipDao.findAllByCustomQuery(AIPQueryGenerator
                .searchAipTagsUsingSQL(request.getState(), request.getFrom(), request.getTo(), request.getTags(),
                                       request.getSession(), request.getProviderId(), request.getAipIds(),
                                       request.getAipIdsExcluded(), request.getStoredOn()));
    }

    @Override
    public AIPSession getSession(String sessionId, Boolean createIfNotExists) throws EntityNotFoundException {
        AIPSession session;
        String id = sessionId;
        if (id == null) {
            id = DEFAULT_SESSION_ID;
        }
        Optional<AIPSession> oSession = aipSessionRepository.findById(id);
        if (oSession.isPresent()) {
            session = oSession.get();
        } else if (createIfNotExists) {
            session = aipSessionRepository.save(AIPSessionBuilder.build(id));
        } else {
            throw new EntityNotFoundException(sessionId, AIPSession.class);
        }
        return session;
    }

    @Override
    public AIPSession getSessionWithStats(String sessionId) throws EntityNotFoundException {
        AIPSession session = getSession(sessionId, false);
        return addSessionSipInformations(session);
    }

    @Override
    public Page<AIPSession> searchSessions(String id, OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        Page<AIPSession> pagedSessions = aipSessionRepository.findAll(AIPSessionSpecifications.search(id, from, to),
                                                                      pageable);
        List<AIPSession> sessions = new ArrayList<>();
        pagedSessions.forEach(s -> sessions.add(this.addSessionSipInformations(s)));
        return new PageImpl<>(sessions, pageable, pagedSessions.getTotalElements());
    }

    /**
     * Create a {@link AIPSession} for the session id.
     * @return {@link AIPSession}
     */
    private AIPSession addSessionSipInformations(AIPSession session) {
        long aipsCount = aipDao.countBySessionId(session.getId());
        long queuedAipsCount = aipDao.countBySessionIdAndStateIn(session.getId(), Sets
                .newHashSet(AIPState.VALID, AIPState.PENDING, AIPState.STORING_METADATA));
        long storedAipsCount = aipDao.countBySessionIdAndStateIn(session.getId(), Sets.newHashSet(AIPState.STORED));
        long deletedAipsCount = aipDao.countBySessionIdAndStateIn(session.getId(), Sets.newHashSet(AIPState.DELETED));
        long errorAipsCount = aipDao.countBySessionIdAndStateIn(session.getId(),
                                                                Sets.newHashSet(AIPState.STORAGE_ERROR));
        long nbFilesStored = dataFileDao.findAllByStateAndAipSession(DataFileState.STORED, session.getId());
        long nbFiles = dataFileDao.findAllByAipSession(session.getId());

        session.setAipsCount(aipsCount);
        session.setDeletedAipsCount(deletedAipsCount);
        session.setErrorAipsCount(errorAipsCount);
        session.setQueuedAipsCount(queuedAipsCount);
        session.setStoredAipsCount(storedAipsCount);
        session.setStoredDataFilesCount(nbFilesStored);
        session.setDataFilesCount(nbFiles);
        return session;
    }

    /**
     * Creates new {@link StorageDataFile} in PENDING state. Those new files will be handled by schedulers for storage.
     * Update {@link StorageDataFile} to remove to TO_BE_DELETED state. Those files will be handled by schedulers for deletion.
     * @param newAIPBuilder
     * @param newAip new {@link AIP} values
     * @param aipToUpdate current {@link AIP} to update
     * @throws EntityNotFoundException
     * @return TRUE if there is new {@link StorageDataFile} to store
     */
    private boolean handleContentInformationUpdate(AIPBuilder newAIPBuilder, AIP newAip, AIP aipToUpdate)
            throws EntityNotFoundException {
        boolean newfilesToStore = false;
        Set<StorageDataFile> existingFiles = dataFileDao.findAllByAip(aipToUpdate);
        Optional<AIPEntity> aipEntity = aipEntityRepository.findOneByAipId(aipToUpdate.getId().toString());
        if (!aipEntity.isPresent()) {
            throw new EntityNotFoundException(aipToUpdate.getId().toString(), AIPEntity.class);
        }
        Set<StorageDataFile> newDataFiles = StorageDataFile.extractDataFilesForExistingAIP(newAip, aipEntity.get());
        Set<StorageDataFile> toDelete = Sets.newHashSet();
        toDelete.addAll(existingFiles);
        for (StorageDataFile newFile : newDataFiles) {
            if (existingFiles.contains(newFile)) {
                toDelete.remove(newFile);
            } else {
                newfilesToStore = true;
                LOGGER.debug("[UPDATE AIP] Add new datastore file {} for AIP {}.", newFile.getName(),
                             newAip.getProviderId());
                newAIPBuilder.getContentInformationBuilder()
                        .setDataObject(newFile.getDataType(), newFile.getName(), newFile.getAlgorithm(),
                                       newFile.getChecksum(), newFile.getFileSize(),
                                       newFile.getUrls().toArray(new URL[newFile.getUrls().size()]));
                newAIPBuilder.getContentInformationBuilder().setSyntax(newFile.getMimeType());
                newFile.setState(DataFileState.PENDING);
                dataFileDao.save(newFile);
                em.flush();
                em.clear();
                newAIPBuilder.addContentInformation();
            }
        }
        // Schedule deletion for all files except for AIP file. The metadata AIP file will be deleted only when
        // the new metadata file will be stored.
        toDelete.stream().filter(df -> !df.getDataType().equals(DataType.AIP)).forEach(fileToDelete -> {
            LOGGER.debug("[UPDATE AIP] Update datastore file {} for AIP {} to TO_BE_DELETED state.",
                         fileToDelete.getName(), newAip.getProviderId());
            fileToDelete.setState(DataFileState.TO_BE_DELETED);
            dataFileDao.save(fileToDelete);
            em.flush();
            em.clear();
        });
        return newfilesToStore;
    }
}
