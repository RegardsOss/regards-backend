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
package fr.cnes.regards.modules.storage.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.StorageType;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileRequestsGroupEvent;
import fr.cnes.regards.modules.storage.dao.*;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.service.cache.CacheService;
import fr.cnes.regards.modules.storage.service.file.FileDownloadService;
import fr.cnes.regards.modules.storage.service.file.FileReferenceEventPublisher;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.handler.FileReferenceEventHandler;
import fr.cnes.regards.modules.storage.service.file.request.*;
import fr.cnes.regards.modules.storage.service.location.StorageLocationConfigurationService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationService;
import fr.cnes.regards.modules.storage.service.location.StoragePluginConfigurationHandler;
import fr.cnes.regards.modules.storage.service.plugin.SimpleNearlineDataStorage;
import fr.cnes.regards.modules.storage.service.plugin.SimpleOfflineDataStorage;
import fr.cnes.regards.modules.storage.service.plugin.SimpleOnlineDataStorage;
import fr.cnes.regards.modules.storage.service.session.SessionNotifierPropertyEnum;
import fr.cnes.regards.modules.templates.dao.ITemplateRepository;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author sbinda
 */
@TestPropertySource(properties = { "regards.storage.schedule.initial.delay=100", "regards.storage.schedule.delay=100" })
public abstract class AbstractStorageIT extends AbstractMultitenantServiceIT {

    public static final String ONLINE_CONF_LABEL = "target";

    protected static final String OFFLINE_CONF_LABEL = "offline";

    protected static final String ONLINE_CONF_LABEL_WITHOUT_DELETE = "target_without_delete";

    protected static final String NEARLINE_CONF_LABEL = "NL_target";

    protected static final String NEARLINE_EXT_CACHE_CONF_LABEL = "NL_target_with_external_cache";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStorageIT.class);

    private static final Long ALLOCATED_SIZE_IN_KO = 1_000_000L;

    @SpyBean
    protected FileReferenceEventPublisher fileEventPublisher;

    @Autowired
    protected FileReferenceEventHandler fileRefEventHandler;

    @Autowired
    protected FileReferenceRequestService fileReqService;

    @Autowired
    protected FileReferenceService fileRefService;

    @Autowired
    protected FileDownloadService fileDownloadService;

    @Autowired
    protected DownloadTokenService downloadTokenService;

    @Autowired
    protected FileStorageRequestService stoReqService;

    @Autowired
    protected FileCacheRequestService fileCacheRequestService;

    @Autowired
    protected FileCopyRequestService fileCopyRequestService;

    @Autowired
    protected CacheService cacheService;

    @Autowired
    protected FileDeletionRequestService fileDeletionRequestService;

    @Autowired
    protected IJobService jobService;

    @Autowired
    protected StoragePluginConfigurationHandler storagePlgConfHandler;

    @Autowired
    protected IFileReferenceRepository fileRefRepo;

    @Autowired
    protected IFileReferenceWithOwnersRepository fileRefWithOwnersRepo;

    @Autowired
    protected IFileCacheRequestRepository fileCacheRequestRepository;

    @Autowired
    protected ICacheFileRepository cacheFileRepository;

    @Autowired
    protected IFileStorageRequestRepository fileStorageRequestRepo;

    @Autowired
    protected IFileDeletetionRequestRepository fileDeletionRequestRepo;

    @Autowired
    protected IFileCopyRequestRepository copyRequestRepository;

    @Autowired
    protected IJobInfoRepository jobInfoRepo;

    @Autowired
    protected IGroupRequestInfoRepository groupRequestInfoRepository;

    @Autowired
    protected StorageLocationConfigurationService storageLocationConfService;

    @Autowired
    protected IDownloadTokenRepository downloadTokenRepo;

    @Autowired
    protected RequestStatusService reqStatusService;

    @Autowired
    protected IRequestGroupRepository requestGroupRepository;

    @Autowired
    protected ITemplateRepository templateRepo;

    @Autowired
    protected IPluginService pluginService;

    protected String originUrl = "file://in/this/directory/file.test";

    protected PluginConfiguration nearLineConf;

    @Autowired
    protected StorageLocationService storageLocationService;

    protected void init() throws ModuleException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        try {
            if (Files.exists(Paths.get("target/cache"))) {
                FileUtils.deleteDirectory(Paths.get("target/cache").toFile());
            }
            if (Files.exists(Paths.get("target/storage"))) {
                FileUtils.deleteDirectory(Paths.get("target/storage").toFile());
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        templateRepo.deleteAll();
        groupRequestInfoRepository.deleteAll();
        copyRequestRepository.deleteAll();
        fileDeletionRequestRepo.deleteAll();
        fileStorageRequestRepo.deleteAll();
        fileCacheRequestRepository.deleteAll();
        cacheFileRepository.deleteAll();
        fileRefRepo.deleteAll();
        jobInfoRepo.deleteAll();
        downloadTokenRepo.deleteAll();
        requestGroupRepository.deleteAll();

        storageLocationService.getAllLocations().forEach(f -> {
            try {
                storageLocationService.delete(f.getName());
            } catch (ModuleException e) {
                Assert.fail(e.getMessage());
            }
        });

        initDataStoragePluginConfiguration(ONLINE_CONF_LABEL, true);
        initDataStorageOLPluginConfiguration(OFFLINE_CONF_LABEL);
        initDataStoragePluginConfiguration(ONLINE_CONF_LABEL_WITHOUT_DELETE, false);

        nearLineConf = initDataStorageNLPluginConfiguration(NEARLINE_CONF_LABEL, false).getPluginConfiguration();
        initDataStorageNLPluginConfiguration(NEARLINE_EXT_CACHE_CONF_LABEL, true).getPluginConfiguration();

        storagePlgConfHandler.refresh();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        simulateApplicationStartedEvent();
    }

    protected StorageLocationConfiguration initDataStorageOLPluginConfiguration(String label) throws ModuleException {
        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOfflineDataStorage.class);

        Set<IPluginParam> parameters = IPluginParam.set();
        PluginConfiguration dataStorageConf = new PluginConfiguration(label, parameters, 0, dataStoMeta.getPluginId());
        dataStorageConf.setIsActive(true);
        return storageLocationConfService.create(label, dataStorageConf, ALLOCATED_SIZE_IN_KO);
    }

    protected StorageLocationConfiguration initDataStoragePluginConfiguration(String label,
                                                                              boolean allowPhysicalDeletion)
        throws ModuleException {
        try {
            PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOnlineDataStorage.class);

            Files.createDirectories(Paths.get(getBaseStorageLocation().getPath()));

            Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                                                               getBaseStorageLocation().getPath()),
                                                            IPluginParam.build(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN,
                                                                               "error.*"),
                                                            IPluginParam.build(SimpleOnlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN,
                                                                               "delErr.*"),
                                                            IPluginParam.build(SimpleOnlineDataStorage.ALLOW_PHYSICAL_DELETION,
                                                                               allowPhysicalDeletion));
            PluginConfiguration dataStorageConf = new PluginConfiguration(label,
                                                                          parameters,
                                                                          0,
                                                                          dataStoMeta.getPluginId());
            dataStorageConf.setIsActive(true);
            StorageLocationConfiguration storageLocationConfiguration = storageLocationConfService.create(label,
                                                                                                          dataStorageConf,
                                                                                                          ALLOCATED_SIZE_IN_KO);
            pluginService.cleanLocalPluginCache(dataStorageConf.getBusinessId());
            return storageLocationConfiguration;
        } catch (IOException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

    protected StorageLocationConfiguration initDataStorageNLPluginConfiguration(String label, boolean externalCache)
        throws ModuleException {
        try {
            PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleNearlineDataStorage.class);

            Files.createDirectories(Paths.get(getBaseStorageLocation().getPath()));
            Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SimpleNearlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                                                               getBaseStorageLocation().getPath()),
                                                            IPluginParam.build(SimpleNearlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN,
                                                                               "error.*"),
                                                            IPluginParam.build(SimpleNearlineDataStorage.HANDLE_STORAGE_PENDING_FILE_PATTERN,
                                                                               "pending.*"),
                                                            IPluginParam.build(SimpleNearlineDataStorage.HANDLE_RESTORATION_ERROR_FILE_PATTERN,
                                                                               "restoError.*"),
                                                            IPluginParam.build(SimpleNearlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN,
                                                                               "delErr.*"),
                                                            IPluginParam.build(SimpleNearlineDataStorage.EXT_CACHE_PLUGIN_PARAM_NAME,
                                                                               externalCache));
            PluginConfiguration dataStorageConf = new PluginConfiguration(label,
                                                                          parameters,
                                                                          0,
                                                                          dataStoMeta.getPluginId());
            dataStorageConf.setIsActive(true);
            return storageLocationConfService.create(label, dataStorageConf, ALLOCATED_SIZE_IN_KO);
        } catch (IOException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

    protected void updatePluginConfForError(String newErrorPattern) throws ModuleException {
        StorageLocationConfiguration conf = storageLocationConfService.getFirstActive(StorageType.ONLINE);
        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                                                           getBaseStorageLocation().getPath()),
                                                        IPluginParam.build(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN,
                                                                           newErrorPattern),
                                                        IPluginParam.build(SimpleOnlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN,
                                                                           "delErr.*"));
        conf.getPluginConfiguration().setParameters(parameters);
        storageLocationConfService.update(conf.getName(), conf);
        pluginService.cleanLocalPluginCache(conf.getPluginConfiguration().getBusinessId());
    }

    protected FileReference generateRandomStoredOnlineFileReference() throws InterruptedException, ExecutionException {
        return this.generateRandomStoredOnlineFileReference("file.test", Optional.empty());
    }

    protected FileReference generateRandomStoredOnlineFileReference(String fileName, Optional<String> subDir)
        throws InterruptedException, ExecutionException {
        return this.generateStoredFileReference(UUID.randomUUID().toString(),
                                                "someone",
                                                fileName,
                                                ONLINE_CONF_LABEL,
                                                subDir,
                                                Optional.empty(),
                                                "source1",
                                                "session1");
    }

    protected FileReference generateRandomStoredNearlineFileReference(boolean externalCache)
        throws InterruptedException, ExecutionException {
        return generateRandomStoredNearlineFileReference("file.test", Optional.empty(), externalCache);
    }

    protected FileReference generateRandomStoredNearlineFileReference()
        throws InterruptedException, ExecutionException {
        return generateRandomStoredNearlineFileReference(false);
    }

    protected FileReference generateRandomStoredNearlineFileReference(String fileName, Optional<String> subDir)
        throws InterruptedException, ExecutionException {
        return generateRandomStoredNearlineFileReference(fileName, subDir, false);
    }

    protected FileReference generateRandomStoredNearlineFileReference(String fileName,
                                                                      Optional<String> subDir,
                                                                      boolean externalCache)
        throws InterruptedException, ExecutionException {
        return generateStoredFileReference(UUID.randomUUID().toString(),
                                           "someone",
                                           fileName,
                                           externalCache ? NEARLINE_EXT_CACHE_CONF_LABEL : NEARLINE_CONF_LABEL,
                                           subDir,
                                           Optional.empty(),
                                           "source1",
                                           "session1");
    }

    protected Optional<FileReference> generateStoredFileReferenceAlreadyReferenced(String checksum,
                                                                                   String storage,
                                                                                   String newOwner,
                                                                                   String sessionOwner,
                                                                                   String session) {
        Optional<FileReference> oFilef = fileRefService.search(storage, checksum);
        Assert.assertTrue("File reference should already exists", oFilef.isPresent());
        return stoReqService.handleRequest(newOwner,
                                           sessionOwner,
                                           session,
                                           oFilef.get().getMetaInfo(),
                                           originUrl,
                                           oFilef.get().getLocation().getStorage(),
                                           Optional.empty(),
                                           UUID.randomUUID().toString());

    }

    protected FileReference generateStoredFileReference(String checksum,
                                                        String owner,
                                                        String fileName,
                                                        String storage,
                                                        Optional<String> subDir,
                                                        Optional<String> type,
                                                        String sessionOwner,
                                                        String session)
        throws InterruptedException, ExecutionException {
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum,
                                                                       "MD5",
                                                                       fileName,
                                                                       1024L,
                                                                       MediaType.APPLICATION_OCTET_STREAM);
        fileMetaInfo.withType(type.orElse(null));
        // Run file reference creation.
        stoReqService.handleRequest(owner,
                                    sessionOwner,
                                    session,
                                    fileMetaInfo,
                                    originUrl,
                                    storage,
                                    subDir,
                                    UUID.randomUUID().toString());
        // The file reference should exist yet cause a storage job is needed. Nevertheless a FileReferenceRequest should be created.
        Optional<FileReference> oFileRef = fileRefService.search(storage, checksum);
        Collection<FileStorageRequestAggregation> fileRefReqs = stoReqService.search(storage, checksum);
        Assert.assertFalse("File reference should not have been created yet.", oFileRef.isPresent());
        Assert.assertEquals("File reference request should exists", 1, fileRefReqs.size());
        Assert.assertEquals("File reference request should be in TO_STORE status",
                            FileRequestStatus.TO_DO,
                            fileRefReqs.stream().findFirst().get().getStatus());
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO, null, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        runAndWaitJob(jobs);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        fileRefReqs = stoReqService.search(storage, checksum);
        oFileRef = fileRefService.search(storage, checksum);
        Assert.assertTrue("File reference should have been created.", oFileRef.isPresent());
        Assert.assertFalse("File reference should be fully stored witout remaining action.",
                           oFileRef.get().getLocation().isPendingActionRemaining());
        try {
            Assert.assertTrue("File should be created on disk",
                              Files.exists(Paths.get(new URL(oFileRef.get().getLocation().getUrl()).getPath())));
        } catch (MalformedURLException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue("File reference request should not exists anymore", fileRefReqs.isEmpty());
        return fileRefWithOwnersRepo.findOneById(oFileRef.get().getId());
    }

    protected Optional<FileReference> referenceFile(String checksum,
                                                    String owner,
                                                    String type,
                                                    String fileName,
                                                    String storage,
                                                    String sessionOwner,
                                                    String session,
                                                    boolean pendingActionRemaining) {
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum,
                                                                       "MD5",
                                                                       fileName,
                                                                       1024L,
                                                                       MediaType.APPLICATION_OCTET_STREAM);
        fileMetaInfo.setType(type);
        FileLocation location = new FileLocation(storage,
                                                 "anywhere://in/this/directory/file.test",
                                                 pendingActionRemaining);
        try {
            fileReqService.reference(owner,
                                     fileMetaInfo,
                                     location,
                                     Sets.newHashSet(UUID.randomUUID().toString()),
                                     sessionOwner,
                                     session);
        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
        return fileRefService.search(location.getStorage(), fileMetaInfo.getChecksum());
    }

    protected Optional<FileReference> referenceRandomFile(String owner,
                                                          String type,
                                                          String fileName,
                                                          String storage,
                                                          String sessionOwner,
                                                          String session,
                                                          boolean pendingRemainingAction) {
        return this.referenceFile(UUID.randomUUID().toString(),
                                  owner,
                                  type,
                                  fileName,
                                  storage,
                                  sessionOwner,
                                  session,
                                  pendingRemainingAction);
    }

    protected FileStorageRequestAggregation generateRandomStorageRequest(String id,
                                                                         String checksum,
                                                                         FileRequestStatus status) {
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum,
                                                                       "MD5",
                                                                       checksum,
                                                                       132L,
                                                                       MediaType.APPLICATION_OCTET_STREAM);
        FileStorageRequestAggregation fr = new FileStorageRequestAggregation(UUID.randomUUID().toString(),
                                                                             fileMetaInfo,
                                                                             "file:///test/toto/" + id,
                                                                             ONLINE_CONF_LABEL,
                                                                             Optional.empty(),
                                                                             UUID.randomUUID().toString(),
                                                                             "sessionOwner",
                                                                             "session");
        fr.setStatus(status);
        return fr;
    }

    protected FileStorageRequestAggregation generateStoreFileError(String owner,
                                                                   String storageDestination,
                                                                   String sessionOwner,
                                                                   String session) {
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(UUID.randomUUID().toString(),
                                                                       "MD5",
                                                                       "error.file.test",
                                                                       132L,
                                                                       MediaType.APPLICATION_OCTET_STREAM);
        FileLocation destination = new FileLocation(storageDestination, "/in/this/directory", false);
        // Run file reference creation.
        stoReqService.handleRequest(owner,
                                    sessionOwner,
                                    session,
                                    fileMetaInfo,
                                    originUrl,
                                    storageDestination,
                                    Optional.of("/in/this/directory"),
                                    UUID.randomUUID().toString());
        // The file reference should exist yet cause a storage job is needed. Nevertheless a FileReferenceRequest should be created.
        Optional<FileReference> oFileRef = fileRefService.search(destination.getStorage(), fileMetaInfo.getChecksum());
        Collection<FileStorageRequestAggregation> fileRefReqs = stoReqService.search(destination.getStorage(),
                                                                                     fileMetaInfo.getChecksum());
        Assert.assertFalse("File reference should not have been created yet.", oFileRef.isPresent());
        Assert.assertEquals("File reference request should exists", 1, fileRefReqs.size());
        // only the configured storage can be used for storage. Otherwise the request should be set in error.
        if (storageDestination.equals(ONLINE_CONF_LABEL) || storageDestination.equals(NEARLINE_CONF_LABEL)) {
            Assert.assertEquals("File reference request should be in TO_DO status",
                                FileRequestStatus.TO_DO,
                                fileRefReqs.stream().findFirst().get().getStatus());
            // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
            Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO,
                                                                  Sets.newHashSet(),
                                                                  Sets.newHashSet());
            Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
            // Run Job and wait for end
            runAndWaitJob(jobs);
            // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
            fileRefReqs = stoReqService.search(destination.getStorage(), fileMetaInfo.getChecksum());
            oFileRef = fileRefService.search(destination.getStorage(), fileMetaInfo.getChecksum());
            Assert.assertFalse("File reference should not have been created yet.", oFileRef.isPresent());
            Assert.assertEquals("File reference request should exists", 1, fileRefReqs.size());
            Assert.assertEquals("File reference request should be STORE_ERROR status",
                                FileRequestStatus.ERROR,
                                fileRefReqs.stream().findFirst().get().getStatus());
        } else {
            Assert.assertEquals("File reference request should be in STORE_ERROR status",
                                FileRequestStatus.ERROR,
                                fileRefReqs.stream().findFirst().get().getStatus());
        }

        return fileRefReqs.stream().findFirst().get();
    }

    protected URL getBaseStorageLocation() {
        try {
            return new URL("file", "", Paths.get("target/simpleOnlineStorage").toFile().getAbsolutePath());
        } catch (MalformedURLException e) {
            Assert.fail(e.getMessage());
            return null;
        }
    }

    protected void runAndWaitJob(Collection<JobInfo> jobs) {
        // Run Job and wait for end
        String tenant = runtimeTenantResolver.getTenant();
        try {
            Iterator<JobInfo> it = jobs.iterator();
            List<RunnableFuture<Void>> list = Lists.newArrayList();
            while (it.hasNext()) {
                list.add(jobService.runJob(it.next(), tenant));
            }
            for (RunnableFuture<Void> futur : list) {
                LOGGER.info("Waiting synchronous job ...");
                futur.get(120L, TimeUnit.SECONDS);
                LOGGER.info("Synchronous job ends");
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        } finally {
            runtimeTenantResolver.forceTenant(tenant);
        }
    }

    protected FileReferenceEvent getFileReferenceEvent(Collection<ISubscribable> events) {
        FileReferenceEvent event = null;
        for (ISubscribable c : events) {
            if (c instanceof FileReferenceEvent) {
                event = (FileReferenceEvent) c;
            }
        }
        Assert.assertNotNull("No file reference event checked", event);
        return event;
    }

    protected FileRequestsGroupEvent getFileRequestsGroupEvent(Collection<ISubscribable> events) {
        FileRequestsGroupEvent event = null;
        for (ISubscribable c : events) {
            if (c instanceof FileRequestsGroupEvent) {
                event = (FileRequestsGroupEvent) c;
            }
        }
        Assert.assertNotNull("No file reference event checked", event);
        return event;
    }

    protected Collection<FileReferenceEvent> getFileReferenceEvents(Collection<ISubscribable> events) {
        Set<FileReferenceEvent> evts = Sets.newHashSet();
        for (ISubscribable c : events) {
            if (c instanceof FileReferenceEvent) {
                evts.add((FileReferenceEvent) c);
            }
        }
        Assert.assertFalse("No file reference event checked", events.isEmpty());
        return evts;
    }

    protected List<StepPropertyUpdateRequestEvent> getStepPropertyEvents(Collection<ISubscribable> events) {
        // get all events of type StepPropertyUpdateRequestEvent
        List<StepPropertyUpdateRequestEvent> stepList = new ArrayList<>();
        for (ISubscribable e : events) {
            if (e instanceof StepPropertyUpdateRequestEvent) {
                stepList.add((StepPropertyUpdateRequestEvent) e);
            }
        }
        // sort list to make sure it is sorted by creation date
        stepList.sort(Comparator.comparing(StepPropertyUpdateRequestEvent::getDate));
        return stepList;
    }

    protected void checkStepEvent(StepPropertyUpdateRequestEvent event,
                                  SessionNotifierPropertyEnum expectedEventProperty,
                                  StepPropertyEventTypeEnum expectedType,
                                  String expectedSessionOwner,
                                  String expectedSession,
                                  String expectedValue) {
        StepProperty stepProperty = event.getStepProperty();
        Assert.assertEquals("This property was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedEventProperty.getName(),
                            stepProperty.getStepPropertyInfo().getProperty());
        Assert.assertEquals("This value was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedValue,
                            stepProperty.getStepPropertyInfo().getValue());
        Assert.assertEquals("This type was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedType,
                            event.getType());
        Assert.assertEquals("This session owner was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedSessionOwner,
                            stepProperty.getSource());
        Assert.assertEquals("This session was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedSession,
                            stepProperty.getSession());
    }

    public void checkSessionEvents(int total,
                                   int incStore,
                                   int decStore,
                                   int incRun,
                                   int decRun,
                                   int stored,
                                   int incErrors,
                                   int decErrors) {
        List<StepPropertyUpdateRequestEvent> stepEventList = getPublishedEvents(StepPropertyUpdateRequestEvent.class);
        Assert.assertEquals(total, stepEventList.size());
        checkPropertyCount(stepEventList,
                           incStore,
                           SessionNotifierPropertyEnum.STORE_REQUESTS.getName(),
                           StepPropertyEventTypeEnum.INC);
        checkPropertyCount(stepEventList,
                           decStore,
                           SessionNotifierPropertyEnum.STORE_REQUESTS.getName(),
                           StepPropertyEventTypeEnum.DEC);

        checkPropertyCount(stepEventList,
                           incRun,
                           SessionNotifierPropertyEnum.REQUESTS_RUNNING.getName(),
                           StepPropertyEventTypeEnum.INC);
        checkPropertyCount(stepEventList,
                           decRun,
                           SessionNotifierPropertyEnum.REQUESTS_RUNNING.getName(),
                           StepPropertyEventTypeEnum.DEC);

        checkPropertyCount(stepEventList,
                           stored,
                           SessionNotifierPropertyEnum.STORED_FILES.getName(),
                           StepPropertyEventTypeEnum.INC);

        checkPropertyCount(stepEventList,
                           incErrors,
                           SessionNotifierPropertyEnum.REQUESTS_ERRORS.getName(),
                           StepPropertyEventTypeEnum.INC);
        checkPropertyCount(stepEventList,
                           decErrors,
                           SessionNotifierPropertyEnum.REQUESTS_ERRORS.getName(),
                           StepPropertyEventTypeEnum.DEC);
    }

    private void checkPropertyCount(List<StepPropertyUpdateRequestEvent> stepEventList,
                                    int count,
                                    String property,
                                    StepPropertyEventTypeEnum type) {
        Assert.assertEquals(count,
                            stepEventList.stream()
                                         .filter(s -> s.getStepProperty()
                                                       .getStepPropertyInfo()
                                                       .getProperty()
                                                       .equals(property))
                                         .filter(s -> s.getType() == type)
                                         .count());
    }

    protected void simulateFileInInternalCache(String checksum) {
        try {
            String filePath = cacheService.getFilePath(checksum);
            cacheService.addFile(checksum,
                                 123L,
                                 "file",
                                 MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                                 DataType.RAWDATA.name(),
                                 new URL("file", null, filePath),
                                 OffsetDateTime.now().plusDays(1),
                                 Set.of(UUID.randomUUID().toString()),
                                 null);
            // Create file on disk
            if (!Files.exists(Paths.get(filePath).getParent())) {
                Files.createDirectories(Paths.get(filePath).getParent());
            }
            if (!Files.exists(Paths.get(filePath))) {
                Files.createFile(Paths.get(filePath));
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}
