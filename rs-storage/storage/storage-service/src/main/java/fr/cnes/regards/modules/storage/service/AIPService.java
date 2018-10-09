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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import feign.FeignException;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
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
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
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
import fr.cnes.regards.modules.storage.dao.AIPQueryGenerator;
import fr.cnes.regards.modules.storage.dao.AIPSessionSpecifications;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IAIPEntityRepository;
import fr.cnes.regards.modules.storage.dao.IAIPSessionRepository;
import fr.cnes.regards.modules.storage.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.AIPSessionBuilder;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.AipDataFiles;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.CoupleAvailableError;
import fr.cnes.regards.modules.storage.domain.FileCorruptedException;
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
import fr.cnes.regards.modules.storage.service.job.StoreDataFilesJob;
import fr.cnes.regards.modules.storage.service.job.StoreMetadataFilesJob;
import fr.cnes.regards.modules.storage.service.job.UpdateAIPsTagJob;
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

    /**
     * JSON files extension.
     */
    public static final String JSON_FILE_EXT = ".json";

    public static final String DEFAULT_SESSION_ID = "default";

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

    /**
     * {@link IWorkspaceService} instance
     */
    @Autowired
    private IWorkspaceService workspaceService;

    @Autowired
    private EntityManager em;

    @Autowired
    private IAIPSessionRepository aipSessionRepository;

    @Autowired
    private IAIPEntityRepository aipEntityRepository;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepo;

    /**
     * The spring application name ~= microservice type
     */
    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    public AIP save(AIP aip, boolean publish) {
        // Create the session if it's not already existing
        AIPSession aipSession = getSession(aip.getSession(), true);
        AIP daoAip = aipDao.save(aip, aipSession);
        if (publish) {
            publisher.publish(new AIPEvent(daoAip));
        }
        em.flush();
        em.clear();
        return daoAip;
    }

    @Override
    public List<RejectedAip> validateAndStore(AIPCollection aips) throws ModuleException {

        // Validate AIPs
        List<RejectedAip> rejectedAips = new ArrayList<>();
        Set<AIP> validAips = validate(aips, rejectedAips);

        // Store valid AIPs
        for (AIP aip : validAips) {
            aip.setState(AIPState.VALID);
            aip.addEvent(EventType.SUBMISSION.name(), "Submission to REGARDS");
            save(aip, false);

            // Extract data files
            AIPSession aipSession = getSession(aip.getSession(), false);
            Set<StorageDataFile> dataFiles = StorageDataFile.extractDataFiles(aip, aipSession);
            dataFiles.forEach(df -> {
                df.setState(DataFileState.PENDING);
                dataFileDao.save(df);
            });
            // To avoid performance problems due to hibernate cache size. We flush entity manager after each entity to save.
            em.flush();
            em.clear();
        }

        return rejectedAips;
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
            boolean rejected = false;
            List<String> rejectionReasons = Lists.newArrayList();
            String ipId = aip.getId().toString();
            // first of all lets see if there already is an aip with this ip id into the database
            if (aipDao.findOneByAipId(ipId).isPresent()) {
                rejectionReasons.add(String.format("AIP with ip id %s already exists", ipId));
                rejected = true;
            }
            Errors errors = new BeanPropertyBindingResult(aip, "aip");
            validator.validate(aip, errors);
            if (errors.hasErrors()) {
                errors.getFieldErrors().forEach(oe -> rejectionReasons
                        .add(String.format("Property %s is invalid: %s", oe.getField(), oe.getDefaultMessage())));
                // now lets handle validation issues
                rejected = true;
            }
            if (rejected) {
                rejectedAips.add(new RejectedAip(ipId, rejectionReasons));
            } else {
                validAips.add(aip);
            }
        }
        return validAips;
    }

    @Override
    public Page<AIP> storePage(Pageable page) throws ModuleException {
        Page<AIP> createdAips = aipDao.findAllWithLockByState(AIPState.VALID, page);
        if (createdAips.getNumberOfElements() > 0) {
            List<AIP> aips = createdAips.getContent();
            Set<StorageDataFile> dataFilesToStore = Sets.newHashSet();

            for (AIP aip : aips) {
                // Retrieve data files to store
                Collection<StorageDataFile> dataFiles;
                if (aip.isRetry()) {
                    dataFiles = dataFileDao.findAllByStateAndAip(DataFileState.ERROR, aip);
                } else {
                    dataFiles = dataFileDao.findAllByStateAndAip(DataFileState.PENDING, aip);
                }
                aip.setState(AIPState.PENDING);
                aip.setRetry(false);
                aipDao.updateAIPStateAndRetry(aip);
                dataFilesToStore.addAll(dataFiles);
                publisher.publish(new AIPEvent(aip));
            }
            // Dispatch and check data files
            Multimap<Long, StorageDataFile> storageWorkingSetMap = dispatchAndCheck(dataFilesToStore);
            // Schedule storage jobs
            scheduleStorage(storageWorkingSetMap, true);
        }
        return createdAips;
    }

    @Override
    public long storeMetadata() {
        long nbScheduled = 0;
        Set<StorageDataFile> metadataToStore;
        do {
            // first lets get AIP that are not fully stored(at least metadata are not stored)
            metadataToStore = getMetadataFilesToStore();
            nbScheduled = nbScheduled + metadataToStore.size();
            // now that we know all the metadata that should be stored, lets schedule their storage!
            if (!metadataToStore.isEmpty()) {
                LOGGER.debug("Scheduling {} new metadata files for storage.", metadataToStore.size());
                scheduleStorageMetadata(metadataToStore);
            }
        } while (!metadataToStore.isEmpty());
        return nbScheduled;
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
        LOGGER.debug("{} data objects has been dispatched between {} data storage by allocation strategy",
                     dataFilesToStore.size(), storageWorkingSetMap.keySet().size());
        // as we are trusty people, we check that the dispatch gave us back all DataFiles into the WorkingSubSets
        LOGGER.debug("Cheking dispatch results ....");
        checkDispatch(dataFilesToStore, storageWorkingSetMap, dispatchErrors);
        LOGGER.debug("Dispatch results checked !");
        // now that those who should be in error are handled,  lets set notYetStoredBy and save data files
        for (StorageDataFile df : storageWorkingSetMap.values()) {
            df.increaseNotYetStoredBy();
        }
        // Save dataFiles
        for (StorageDataFile file : storageWorkingSetMap.values()) {
            dataFileDao.save(file);
            em.flush();
            em.clear();
        }
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
        Pageable page = new PageRequest(0, 500, Sort.Direction.ASC, "id");
        Page<StorageDataFile> dataFilePage = dataFileDao.findPageByChecksumIn(requestedChecksums, page);
        while (dataFilePage.hasContent()) {

            Set<StorageDataFile> dataFiles = Sets.newHashSet(dataFilePage.getContent());
            // 1. Check for invalid files.
            // Because we only have a page of data file here, we must intersect the ones missing with the ones we have not found before too.
            if (dataFilePage.getTotalElements() != requestedChecksums.size()) {
                Set<String> dataFilesChecksumsForThisPage = dataFiles.stream().map(df -> df.getChecksum())
                        .collect(Collectors.toSet());
                Set<String> checksumNotFoundForThisPage = Sets.difference(requestedChecksums,
                                                                          dataFilesChecksumsForThisPage);
                checksumNotFound = Sets.intersection(checksumNotFound, checksumNotFoundForThisPage);
            }

            Set<StorageDataFile> dataFilesWithAccess = checkLoadFilesAccessRights(dataFiles);

            // Once we know to which file we have access, lets set the others in error.
            // As a file can be associated to multiple AIP, we have to compare their checksums.
            Set<String> checksumsWithoutAccessForThisPage = Sets
                    .difference(dataFiles.stream().map(df -> df.getChecksum()).collect(Collectors.toSet()),
                                dataFilesWithAccess.stream().map(df -> df.getChecksum()).collect(Collectors.toSet()));
            checksumsWithoutAccess = Sets.intersection(checksumsWithoutAccess, checksumsWithoutAccessForThisPage);

            Set<StorageDataFile> nearlineFiles = Sets.newHashSet();

            // 2. Check for online files. Online files don't need to be stored in the cache
            // they can be accessed directly where they are stored.
            for (StorageDataFile df : dataFilesWithAccess) {
                if (df.getPrioritizedDataStorages() != null) {
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
        // the if is needed here too because otherwise checksumNotFound being all checksums requested,
        // everything is considered not found
        if (dataFilePage.getTotalElements() != requestedChecksums.size()) {
            // lets logs not found now that we know that remaining checksums are not handled by REGARDS
            errors.addAll(checksumNotFound);
            checksumNotFound.stream()
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
            for (Iterator<UniformResourceName> i = aipIdsMap.keySet().iterator(); i.hasNext();) {
                if (!urnsWithAccess.contains(i.next())) {
                    i.remove();
                }
            }
        }
        return ImmutableSet.copyOf(aipIdsMap.values());
    }

    @Override
    public Page<AIP> retrieveAIPs(AIPState state, OffsetDateTime from, OffsetDateTime to, List<String> tags,
            String session, String providerId, Pageable pageable) throws ModuleException {
        if (!getSecurityDelegationPlugin().hasAccessToListFeature()) {
            throw new EntityOperationForbiddenException("Only Admins can access this feature.");
        }
        AIPSession aipSession = getSession(session, false);
        return aipDao.findAll(AIPQueryGenerator.search(state, from, to, tags, aipSession, providerId, null, null),
                              pageable);
    }

    @Override
    public Page<AipDataFiles> retrieveAipDataFiles(AIPState state, Set<String> tags, OffsetDateTime fromLastUpdateDate,
            Pageable pageable) {
        // first lets get the page of aips
        // we have two cases: there is a date or not
        Page<AIP> aips;
        if (fromLastUpdateDate == null) {
            if ((tags == null) || tags.isEmpty()) {
                aips = aipDao.findAllByState(state, pageable);
            } else {
                aips = aipDao.findAll(AIPQueryGenerator.search(state, null, null, new ArrayList<>(tags), null, null,
                                                               null, null),
                                      pageable);
            }
        } else {
            if ((tags == null) || tags.isEmpty()) {
                aips = aipDao.findAllByStateAndLastEventDateAfter(state, fromLastUpdateDate, pageable);
            } else {
                aips = aipDao.findAllByStateAndTagsInAndLastEventDateAfter(state, tags, fromLastUpdateDate, pageable);
            }
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
        Pageable page = new PageRequest(0, aipIterationLimit);
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
     * @param {@link DispatchErrors} errors during files dispatch
     */
    private void checkDispatch(Set<StorageDataFile> dataFilesToStore,
            Multimap<Long, StorageDataFile> storageWorkingSetMap, DispatchErrors dispatchErrors) {
        Set<StorageDataFile> dataFilesInSubSet = storageWorkingSetMap.entries().stream().map(entry -> entry.getValue())
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
            notifyAdmins("Some file were not associated to a data storage", email.getText(), NotificationType.ERROR);
        }
    }

    /**
     * Use the notification module in admin to create a notification for admins
     */
    private void notifyAdmins(String title, String message, NotificationType type) {
        NotificationDTO notif = new NotificationDTO(message, Sets.newHashSet(),
                Sets.newHashSet(DefaultRole.ADMIN.name()), applicationName, title, type);
        try {
            FeignSecurityManager.asSystem();
            notificationClient.createNotification(notif);
        } catch (FeignException | HttpClientErrorException | HttpServerErrorException e) {
            LOGGER.error("Error sending notification to admins through admin microservice.", e);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    /**
     * This method schedules {@link StoreDataFilesJob} or {@link StoreMetadataFilesJob} to storeAndCreate given
     * {@link StorageDataFile}s.<br/>
     * A Job is scheduled for each {@link IWorkingSubset} of each {@link PluginConfiguration}.<br/>
     * @param storageWorkingSetMap List of {@link StorageDataFile} to storeAndCreate per {@link PluginConfiguration}.
     * @param storingData FALSE to store {@link DataType#AIP}, or TRUE for all other type of
     * {@link StorageDataFile}.
     * @return List of {@link UUID} of jobs scheduled.
     */
    public Set<UUID> scheduleStorage(Multimap<Long, StorageDataFile> storageWorkingSetMap, boolean storingData)
            throws ModuleException {
        Set<UUID> jobIds = Sets.newHashSet();
        for (Long dataStorageConfId : storageWorkingSetMap.keySet()) {
            Set<IWorkingSubset> workingSubSets = getWorkingSubsets(storageWorkingSetMap.get(dataStorageConfId),
                                                                   dataStorageConfId,
                                                                   DataStorageAccessModeEnum.STORE_MODE);
            LOGGER.trace("Preparing a job for each working subsets");
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
                    jobIds.add(jobInfoService.createAsQueued(new JobInfo(false, 10, parameters, authResolver.getUser(),
                            StoreMetadataFilesJob.class.getName())).getId());
                }

            }
        }
        // now that files are given to the jobs, lets remove the source url so once stored we only have the good urls
        Collection<StorageDataFile> storageDataFiles = storageWorkingSetMap.values();
        storageDataFiles.forEach(file -> file.setUrls(new HashSet<>()));
        for (StorageDataFile dataFile : storageDataFiles) {
            dataFileDao.save(dataFile);
            em.flush();
            em.clear();
        }
        return jobIds;
    }

    /**
     * Call the {@link IDataStorage} plugins associated to the given {@link PluginConfiguration}s to create
     * {@link IWorkingSubset} of {@link StorageDataFile}s.
     * @param dataFilesToSubSet List of {@link StorageDataFile} to prepare.
     * @param dataStorageConfId {@link PluginConfiguration}
     * @return {@link IWorkingSubset}s, empty if the plugin could not be instantiated
     */
    protected Set<IWorkingSubset> getWorkingSubsets(Collection<StorageDataFile> dataFilesToSubSet,
            Long dataStorageConfId, DataStorageAccessModeEnum accessMode) throws ModuleException {
        if (pluginService.canInstantiate(dataStorageConfId)) {
            IDataStorage<IWorkingSubset> storage = pluginService.getPlugin(dataStorageConfId);
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
                dataMap.put("dataStorage", pluginService.getPluginConfiguration(dataStorageConfId));
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
        } else {
            notifyAdmins("Some files could not be handled by their storage plugin.",
                         String.format("Plugin Configuration %s could not be instanciated."
                                 + " Please check the configuration."
                                 + " Skipping work(mode: %s) on this Plugin configuration for now.", dataStorageConfId,
                                       accessMode),
                         NotificationType.ERROR);
            return new HashSet<>();
        }
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
            for (StorageDataFile dataFile : metadataToStore) {
                AIP aip = dataFile.getAip();
                aip.setState(AIPState.STORING_METADATA);
                save(aip, true);
            }
        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            notifyAdmins("Could not schedule metadata storage",
                         "Metadata storage could not be realized because an error occured. Please check the logs",
                         NotificationType.ERROR);
        }
    }

    /**
     * Retrieve all {@link StorageDataFile} ready to be stored.
     * @return data files to store
     */
    private Set<StorageDataFile> getMetadataFilesToStore() {
        Set<StorageDataFile> metadataToStore = Sets.newHashSet();
        Page<AIP> pendingAips = aipDao.findAllWithLockByState(AIPState.PENDING, new PageRequest(0, aipIterationLimit));
        List<AIP> notFullyStored = pendingAips.getContent();
        // first lets handle the case where every dataFiles of an AIP are successfully stored.
        for (AIP aip : notFullyStored) {
            AIPSession aipSession = getSession(aip.getSession(), false);
            Set<StorageDataFile> storedDataFile = dataFileDao.findAllByStateAndAip(DataFileState.STORED, aip);
            Set<StorageDataFile> aipDataFiles = StorageDataFile.extractDataFiles(aip, aipSession);
            if (storedDataFile.containsAll(aipDataFiles)) {
                // that means all StorageDataFile of this AIP has been stored, lets prepare the metadata storage,
                // first we need to write the metadata into a file
                StorageDataFile meta;
                try {
                    LOGGER.debug("Writting meta-data for aip fully stored {}", aip.getId().toString());
                    meta = writeMetaToWorkspace(aip);
                    // now if we have a meta to store, lets add it
                    meta.setState(DataFileState.PENDING);
                    dataFileDao.save(meta);
                    em.flush();
                    em.clear();
                    metadataToStore.add(meta);
                    // We do not schedule StoreJob for this AIP here, to avoid multiple access to IDataStorage.
                    // The StoreJob is scheduled after with all the AIP DataFiles to store.
                } catch (IOException | FileCorruptedException e) {
                    // if we don't have a meta to storeAndCreate that means a problem happened and we set the aip to
                    // STORAGE_ERROR
                    LOGGER.error(e.getMessage(), e);
                    aip.setState(AIPState.STORAGE_ERROR);
                    save(aip, true);
                }
            } else {
                LOGGER.debug("There is still {} datafiles not stored for AIP {}. Metadata file cannot be generated yet.",
                             aipDataFiles.size() - storedDataFile.size(), aip.getProviderId());
            }
        }
        return metadataToStore;
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
        return aipDao.findAllByTags(tag, page);
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
        Pageable page = new PageRequest(0, 100);
        Page<AIPUpdateRequest> updatePage;
        do {
            // Retrieve all update requests.
            updatePage = aipUpdateRequestRepo.findAll(page);
            for (AIPUpdateRequest request : updatePage) {
                // Retrieve the associated AIP to update
                Optional<AIPEntity> oAIP = aipEntityRepository.findOneWithLockByAipId(request.getAipId());
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
            throws EntityNotFoundException, EntityInconsistentIdentifierException, EntityOperationForbiddenException {
        Optional<AIP> oAipToUpdate = aipDao.findOneWithLockByAipId(ipId);
        // first lets check for issues
        if (!oAipToUpdate.isPresent()) {
            throw new EntityNotFoundException(ipId, AIP.class);
        }
        AIP aipToUpdate = oAipToUpdate.get();
        if ((aipToUpdate.getState() != AIPState.STORED)) {
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
        if (newAip.getTags().size() > 0) {
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

        // Create new DataStoragFile to store, update DataStorageFile to delete.
        handleContentInformationUpdate(newAIPBuilder, newAip, aipToUpdate);

        // Add update event
        newAIPBuilder.addEvent(EventType.UPDATE.toString(), updateMessage, OffsetDateTime.now());
        // now that all updates are set into the builder, lets build and save the updatedAip.
        // AIP is set to VALID state to be handled for store process (datafiles and metadatas)
        AIP updatedAip = newAIPBuilder.build();
        updatedAip.setState(AIPState.VALID);
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
        Optional<AIPUpdateRequest> oUpdateRequest = aipUpdateRequestRepo
                .findOneWithLockByAipId(aipToUpdate.getId().toString());
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
        LOGGER.trace("Finding {} datafile for aip {} took {} ms", dataFilesWithMetadata.size(),
                     toBeDeleted.getId().toString(), daoFindEnd - daoFindStart);
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
        LOGGER.trace("Initiating AIPBuilder for {} took {} ms", toBeDeleted.getId().toString(),
                     endInitiateBuilder - initiateBuilder);
        toBeDeletedBuilder
                .addEvent(EventType.DELETION.name(),
                          "AIP deletion was requested, AIP is considered deleted until its removal from archives",
                          OffsetDateTime.now());
        long endAddEvent = System.currentTimeMillis();
        LOGGER.trace("Adding deletion event to AIP {} took {} ms", toBeDeleted.getId().toString(),
                     endAddEvent - endInitiateBuilder);
        toBeDeleted = toBeDeletedBuilder.build();
        long endRebuild = System.currentTimeMillis();
        LOGGER.trace("Rebuilding AIP {} took {} ms", toBeDeleted.getId().toString(), endRebuild - endAddEvent);
        toBeDeleted.setState(AIPState.DELETED);
        long endChangeState = System.currentTimeMillis();
        LOGGER.trace("Changing AIP {} state to DELETED took {} ms", toBeDeleted.getId().toString(),
                     endChangeState - endRebuild);
        save(toBeDeleted, false);
        long endSave = System.currentTimeMillis();
        LOGGER.trace("Saving AIP {} to DB took {} ms", toBeDeleted.getId().toString(), endSave - endChangeState);
        long methodEnd = System.currentTimeMillis();
        LOGGER.trace("Deleting AIP {} took {} ms", toBeDeleted.getId().toString(), methodEnd - methodStart);
        return notSuppressible;
    }

    @Override
    public Long doDelete() {
        Pageable page = new PageRequest(0, aipIterationLimit);
        Page<StorageDataFile> pageToDelete;
        do {
            pageToDelete = dataFileDao.findPageByState(DataFileState.TO_BE_DELETED, page);
            try {
                scheduleDeletion(pageToDelete.getContent());
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
            throws EntityNotFoundException, EntityInconsistentIdentifierException, EntityOperationForbiddenException {
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
            throws EntityNotFoundException, EntityInconsistentIdentifierException, EntityOperationForbiddenException {
        AIPBuilder updateBuilder = new AIPBuilder(toUpdate);
        updateBuilder.removeTags(tagsToRemove.toArray(new String[tagsToRemove.size()]));
        toUpdate = updateBuilder.build();
        String updateMessage = String.format("Remove tags [%s].", String.join(" , ", tagsToRemove));
        updateAip(toUpdate.getId().toString(), toUpdate, updateMessage);
    }

    private Set<UUID> scheduleDeletion(Collection<StorageDataFile> dataFilesToDelete) throws ModuleException {
        // when we delete DataFiles, we have to get the DataStorages to use thanks to DB informations
        Multimap<Long, StorageDataFile> deletionWorkingSetMultimap = HashMultimap.create();
        for (StorageDataFile toDelete : dataFilesToDelete) {
            toDelete.getPrioritizedDataStorages().forEach(dataStorage -> {
                deletionWorkingSetMultimap.put(dataStorage.getId(), toDelete);
            });
            toDelete.setState(DataFileState.DELETION_PENDING);
            dataFileDao.save(toDelete);
            em.flush();
            em.clear();
        }
        Set<UUID> jobIds = Sets.newHashSet();
        for (Long dataStorageConfId : deletionWorkingSetMultimap.keySet()) {
            Set<IWorkingSubset> workingSubSets = getWorkingSubsets(deletionWorkingSetMultimap.get(dataStorageConfId),
                                                                   dataStorageConfId,
                                                                   DataStorageAccessModeEnum.DELETION_MODE);
            LOGGER.trace("Preparing a deletion job for each working subsets");
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
     * Write on disk the associated metadata file of the given {@link AIP}.
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
                AIPSession aipSession = getSession(aip.getSession(), false);
                metadataAipFile = new StorageDataFile(Sets.newHashSet(urlToMetadata), checksum, checksumAlgorithm,
                        DataType.AIP, urlToMetadata.openConnection().getContentLengthLong(),
                        new MimeType("application", "json"), new AIPEntity(aip, aipSession), aipSession,
                        aip.getId().toString() + JSON_FILE_EXT, null);
            } else {
                workspaceService.removeFromWorkspace(metadataName);
                LOGGER.error(String
                        .format("Storage of AIP metadata(%s) into workspace(%s) failed. Computed checksum once stored does not "
                                + "match expected one", aip.getId().toString(),
                                workspaceService.getMicroserviceWorkspace()));
                throw new FileCorruptedException(String
                        .format("File has been corrupted during storage into workspace. Checksums before(%s) and after (%s) are"
                                + " different", checksum, fileChecksum));
            }
        } catch (NoSuchAlgorithmException e) {
            // Delete written file
            LOGGER.error(e.getMessage(), e);
            workspaceService.removeFromWorkspace(metadataName);
            // this exception should never be thrown as it comes from the same algorithm then at the beginning
            throw new IOException(e);
        }
        return metadataAipFile;
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
        Page<AIP> aips = aipDao.findAllByStateService(AIPState.DELETED, new PageRequest(0, aipIterationLimit));
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
        JobInfo jobInfo = new JobInfo(false, 0, parameters, authResolver.getUser(), DeleteAIPsJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
        LOGGER.debug("New job scheduled uuid={}", jobInfo.getId().toString());
    }

    @Override
    public List<RejectedSip> deleteAipFromSips(Set<String> sipIds) throws ModuleException {
        List<RejectedSip> notHandledSips = new ArrayList<>();
        //to avoid memory issues with hibernate, lets paginate the select and then evict the entities from the cache
        Pageable page = new PageRequest(0, 500);
        long daofindPageStart = System.currentTimeMillis();
        Page<AIP> aipPage = aipDao.findPageBySipIdIn(sipIds, page);
        long daofindPageEnd = System.currentTimeMillis();
        LOGGER.trace("Finding {} aip from {} sip ids took {} ms", aipPage.getNumberOfElements(), sipIds.size(),
                     daofindPageEnd - daofindPageStart);
        while (aipPage.hasContent()) {
            // while there is aip to delete, lets delete them and get the new page at the end
            Map<String, Set<AIP>> aipsPerSip = aipPage.getContent().stream()
                    .collect(Collectors.toMap(aip -> aip.getSipId().get(), aip -> Sets.newHashSet(aip),
                                              (set1, set2) -> Sets.union(set1, set2)));
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
                    notSuppressible.stream().map(sdf -> sdf.getAipEntity())
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
        JobInfo jobInfo = new JobInfo(false, 0, parameters, authResolver.getUser(), UpdateAIPsTagJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
        LOGGER.debug("New job scheduled uuid={}", jobInfo.getId().toString());
    }

    @Override
    public List<String> retrieveAIPTagsByQuery(AIPQueryFilters request) {
        AIPSession aipSession = getSession(request.getSession(), false);
        return aipDao.findAllByCustomQuery(AIPQueryGenerator
                .searchAipTagsUsingSQL(request.getState(), request.getFrom(), request.getTo(), request.getTags(),
                                       aipSession, request.getProviderId(), request.getAipIds(),
                                       request.getAipIdsExcluded()));
    }

    @Override
    public AIPSession getSession(String sessionId, Boolean createIfNotExists) {
        AIPSession session = null;
        String id = sessionId;
        if (sessionId == null) {
            id = DEFAULT_SESSION_ID;
        }
        Optional<AIPSession> oSession = aipSessionRepository.findById(id);
        if (oSession.isPresent()) {
            session = oSession.get();
        } else if (createIfNotExists) {
            session = aipSessionRepository.save(AIPSessionBuilder.build(id));
        }
        return session;
    }

    @Override
    public AIPSession getSessionWithStats(String sessionId) {
        AIPSession session = getSession(sessionId, false);
        return addSessionSipInformations(session);
    }

    @Override
    public Page<AIPSession> searchSessions(String id, OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        Page<AIPSession> pagedSessions = aipSessionRepository.findAll(AIPSessionSpecifications.search(id, from, to),
                                                                      pageable);
        List<AIPSession> sessions = org.apache.commons.compress.utils.Lists.newArrayList();
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

        session.setAipsCount(aipsCount);
        session.setDeletedAipsCount(deletedAipsCount);
        session.setErrorAipsCount(errorAipsCount);
        session.setQueuedAipsCount(queuedAipsCount);
        session.setStoredAipsCount(storedAipsCount);
        return session;
    }

    /**
     * Creates new {@link StorageDataFile} in PENDING state. Those new files will be handled by schedulers for storage.
     * Update {@link StorageDataFile} to remove to TO_BE_DELETED state. Those files will be handled by schedulers for deletion.
     * @param newAIPBuilder
     * @param newAip new {@link AIP} values
     * @param aipToUpdate current {@link AIP} to update
     * @throws EntityNotFoundException
     */
    private void handleContentInformationUpdate(AIPBuilder newAIPBuilder, AIP newAip, AIP aipToUpdate)
            throws EntityNotFoundException {
        Set<StorageDataFile> existingFiles = dataFileDao.findAllByAip(aipToUpdate);
        Optional<AIPEntity> aipEntity = aipEntityRepository.findOneWithLockByAipId(aipToUpdate.getId().toString());
        if (!aipEntity.isPresent()) {
            throw new EntityNotFoundException(aipToUpdate.getId().toString(), AIPEntity.class);
        }
        Set<StorageDataFile> newDataFiles = StorageDataFile
                .extractDataFilesForExistingAIP(newAip, aipEntity.get(), getSession(newAip.getSession(), false));
        Set<StorageDataFile> toDelete = Sets.newHashSet();
        toDelete.addAll(existingFiles);
        for (StorageDataFile newFile : newDataFiles) {
            if (existingFiles.contains(newFile)) {
                toDelete.remove(newFile);
            } else {
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
    }
}
