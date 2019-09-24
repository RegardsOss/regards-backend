/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storagelight.dao.ICacheFileRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileCacheRequestRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileCopyRequestRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileDeletetionRequestRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storagelight.dao.IGroupRequestInfoRepository;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageType;
import fr.cnes.regards.modules.storagelight.service.cache.CacheService;
import fr.cnes.regards.modules.storagelight.service.file.FileDownloadService;
import fr.cnes.regards.modules.storagelight.service.file.FileReferenceEventPublisher;
import fr.cnes.regards.modules.storagelight.service.file.FileReferenceService;
import fr.cnes.regards.modules.storagelight.service.file.handler.FileReferenceEventHandler;
import fr.cnes.regards.modules.storagelight.service.file.request.FileCacheRequestService;
import fr.cnes.regards.modules.storagelight.service.file.request.FileCopyRequestService;
import fr.cnes.regards.modules.storagelight.service.file.request.FileDeletionRequestService;
import fr.cnes.regards.modules.storagelight.service.file.request.FileReferenceRequestService;
import fr.cnes.regards.modules.storagelight.service.file.request.FileStorageRequestService;
import fr.cnes.regards.modules.storagelight.service.location.StorageLocationConfigurationService;
import fr.cnes.regards.modules.storagelight.service.location.StoragePluginConfigurationHandler;
import fr.cnes.regards.modules.storagelight.service.plugin.SimpleNearlineDataStorage;
import fr.cnes.regards.modules.storagelight.service.plugin.SimpleOnlineDataStorage;

/**
 * @author sbinda
 *
 */
public abstract class AbstractStorageTest extends AbstractMultitenantServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStorageTest.class);

    protected static final String ONLINE_CONF_LABEL = "target";

    protected static final String NEARLINE_CONF_LABEL = "NL_target";

    private static final Long ALLOCATED_SIZE_IN_KO = 1_000_000L;

    @SpyBean
    protected FileReferenceEventPublisher fileEventPublisher;

    @SpyBean
    protected IPublisher publisher;

    @Autowired
    protected FileReferenceEventHandler fileRefEventHandler;

    @Autowired
    protected FileReferenceRequestService fileReqService;

    @Autowired
    protected FileReferenceService fileRefService;

    @Autowired
    protected FileDownloadService downloadService;

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
    protected IFileCacheRequestRepository fileCacheReqRepo;

    @Autowired
    protected ICacheFileRepository cacheFileRepo;

    @Autowired
    protected IFileStorageRequestRepository fileStorageRequestRepo;

    @Autowired
    protected IFileDeletetionRequestRepository fileDeletionRequestRepo;

    @Autowired
    protected IFileCopyRequestRepository copyRequestRepository;

    @Autowired
    protected IJobInfoRepository jobInfoRepo;

    @Autowired
    protected IGroupRequestInfoRepository grpReqInfoRepo;

    @Autowired
    protected StorageLocationConfigurationService prioritizedDataStorageService;

    protected String originUrl = "file://in/this/directory/file.test";

    protected void init() throws ModuleException {
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
        grpReqInfoRepo.deleteAll();
        copyRequestRepository.deleteAll();
        fileDeletionRequestRepo.deleteAll();
        fileStorageRequestRepo.deleteAll();
        fileCacheReqRepo.deleteAll();
        cacheFileRepo.deleteAll();
        fileRefRepo.deleteAll();
        jobInfoRepo.deleteAll();
        prioritizedDataStorageService.search(StorageType.ONLINE).forEach(c -> {
            try {
                prioritizedDataStorageService.delete(c.getId());
            } catch (ModuleException e) {
                Assert.fail(e.getMessage());
            }
        });
        prioritizedDataStorageService.search(StorageType.NEARLINE).forEach(c -> {
            try {
                prioritizedDataStorageService.delete(c.getId());
            } catch (ModuleException e) {
                Assert.fail(e.getMessage());
            }
        });

        initDataStoragePluginConfiguration(ONLINE_CONF_LABEL);
        initDataStorageNLPluginConfiguration(NEARLINE_CONF_LABEL);
        storagePlgConfHandler.refresh();

    }

    protected StorageLocationConfiguration initDataStoragePluginConfiguration(String label) throws ModuleException {
        try {
            PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOnlineDataStorage.class);
            Files.createDirectories(Paths.get(getBaseStorageLocation().toURI()));

            Set<IPluginParam> parameters = IPluginParam
                    .set(IPluginParam.build(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                            getBaseStorageLocation().toString()),
                         IPluginParam.build(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, "error.*"),
                         IPluginParam.build(SimpleOnlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN, "delErr.*"));
            PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, label, parameters, 0);
            dataStorageConf.setIsActive(true);
            dataStorageConf.setBusinessId(label);
            return prioritizedDataStorageService.create(dataStorageConf, ALLOCATED_SIZE_IN_KO);
        } catch (IOException | URISyntaxException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

    protected StorageLocationConfiguration initDataStorageNLPluginConfiguration(String label) throws ModuleException {
        try {
            PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleNearlineDataStorage.class);
            Files.createDirectories(Paths.get(getBaseStorageLocation().toURI()));
            Set<IPluginParam> parameters = IPluginParam
                    .set(IPluginParam.build(SimpleNearlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                            getBaseStorageLocation().toString()),
                         IPluginParam.build(SimpleNearlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, "error.*"),
                         IPluginParam.build(SimpleNearlineDataStorage.HANDLE_RESTORATION_ERROR_FILE_PATTERN,
                                            "restoError.*"),
                         IPluginParam.build(SimpleNearlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN, "delErr.*"));
            PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, label, parameters, 0);
            dataStorageConf.setIsActive(true);
            dataStorageConf.setBusinessId(label);
            return prioritizedDataStorageService.create(dataStorageConf, ALLOCATED_SIZE_IN_KO);
        } catch (IOException | URISyntaxException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

    protected void updatePluginConfForError(String newErrorPattern) throws ModuleException {
        StorageLocationConfiguration conf = prioritizedDataStorageService.getFirstActive(StorageType.ONLINE);
        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                        getBaseStorageLocation().toString()),
                     IPluginParam.build(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, newErrorPattern),
                     IPluginParam.build(SimpleOnlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN, "delErr.*"));
        conf.getStorageConfiguration().setParameters(parameters);
        prioritizedDataStorageService.update(conf.getId(), conf);
    }

    protected FileReference generateRandomStoredOnlineFileReference() throws InterruptedException, ExecutionException {
        return this.generateRandomStoredOnlineFileReference("file.test", Optional.empty());
    }

    protected FileReference generateRandomStoredOnlineFileReference(String fileName, Optional<String> subDir)
            throws InterruptedException, ExecutionException {
        return this.generateStoredFileReference(UUID.randomUUID().toString(), "someone", fileName, ONLINE_CONF_LABEL,
                                                subDir);
    }

    protected FileReference generateRandomStoredNearlineFileReference()
            throws InterruptedException, ExecutionException {
        return this.generateRandomStoredNearlineFileReference("file.test", Optional.empty());
    }

    protected FileReference generateRandomStoredNearlineFileReference(String fileName, Optional<String> subDir)
            throws InterruptedException, ExecutionException {
        return this.generateStoredFileReference(UUID.randomUUID().toString(), "someone", fileName, NEARLINE_CONF_LABEL,
                                                subDir);
    }

    protected Optional<FileReference> generateStoredFileReferenceAlreadyReferenced(String checksum, String storage,
            String newOwner) {
        Optional<FileReference> oFilef = fileRefService.search(storage, checksum);
        Assert.assertTrue("File reference should already exists", oFilef.isPresent());
        return stoReqService.handleRequest(newOwner, oFilef.get().getMetaInfo(), originUrl,
                                           oFilef.get().getLocation().getStorage(), Optional.empty(),
                                           UUID.randomUUID().toString());

    }

    protected FileReference generateStoredFileReference(String checksum, String owner, String fileName, String storage,
            Optional<String> subDir) throws InterruptedException, ExecutionException {
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum, "MD5", fileName, 1024L,
                MediaType.APPLICATION_OCTET_STREAM);
        FileLocation destination = new FileLocation(storage, "/in/this/directory");
        // Run file reference creation.
        stoReqService.handleRequest(owner, fileMetaInfo, originUrl, storage, subDir, UUID.randomUUID().toString());
        // The file reference should exist yet cause a storage job is needed. Nevertheless a FileReferenceRequest should be created.
        Optional<FileReference> oFileRef = fileRefService.search(destination.getStorage(), checksum);
        Optional<FileStorageRequest> oFileRefReq = stoReqService.search(destination.getStorage(), checksum);
        Assert.assertFalse("File reference should not have been created yet.", oFileRef.isPresent());
        Assert.assertTrue("File reference request should exists", oFileRefReq.isPresent());
        Assert.assertEquals("File reference request should be in TO_STORE status", FileRequestStatus.TO_DO,
                            oFileRefReq.get().getStatus());
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO, null, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        runAndWaitJob(jobs);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        oFileRefReq = stoReqService.search(destination.getStorage(), checksum);
        oFileRef = fileRefService.search(destination.getStorage(), checksum);
        Assert.assertTrue("File reference should have been created.", oFileRef.isPresent());
        Assert.assertFalse("File reference request should not exists anymore", oFileRefReq.isPresent());
        return oFileRef.get();
    }

    protected Optional<FileReference> referenceFile(String checksum, String owner, String type, String fileName,
            String storage) {
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum, "MD5", fileName, 1024L,
                MediaType.APPLICATION_OCTET_STREAM);
        fileMetaInfo.setType(type);
        FileLocation location = new FileLocation(storage, "anywhere://in/this/directory/file.test");
        try {
            fileReqService.reference(owner, fileMetaInfo, location, Sets.newHashSet(UUID.randomUUID().toString()));
        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
        return fileRefService.search(location.getStorage(), fileMetaInfo.getChecksum());
    }

    protected Optional<FileReference> referenceRandomFile(String owner, String type, String fileName, String storage) {
        return this.referenceFile(UUID.randomUUID().toString(), owner, type, fileName, storage);
    }

    protected FileStorageRequest generateStoreFileError(String owner, String storageDestination)
            throws InterruptedException, ExecutionException {
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(UUID.randomUUID().toString(), "MD5",
                "error.file.test", 132L, MediaType.APPLICATION_OCTET_STREAM);
        FileLocation destination = new FileLocation(storageDestination, "/in/this/directory");
        // Run file reference creation.
        stoReqService.handleRequest(owner, fileMetaInfo, originUrl, storageDestination,
                                    Optional.of("/in/this/directory"), UUID.randomUUID().toString());
        // The file reference should exist yet cause a storage job is needed. Nevertheless a FileReferenceRequest should be created.
        Optional<FileReference> oFileRef = fileRefService.search(destination.getStorage(), fileMetaInfo.getChecksum());
        Optional<FileStorageRequest> oFileRefReq = stoReqService.search(destination.getStorage(),
                                                                        fileMetaInfo.getChecksum());
        Assert.assertFalse("File reference should not have been created yet.", oFileRef.isPresent());
        Assert.assertTrue("File reference request should exists", oFileRefReq.isPresent());
        // only the configured storage can be used for storage. Otherwise the request should be set in eroor.
        if (storageDestination.equals(ONLINE_CONF_LABEL) || storageDestination.equals(NEARLINE_CONF_LABEL)) {
            Assert.assertEquals("File reference request should be in TO_STORE status", FileRequestStatus.TO_DO,
                                oFileRefReq.get().getStatus());
            // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
            Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO, Sets.newHashSet(),
                                                                  Sets.newHashSet());
            Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
            // Run Job and wait for end
            runAndWaitJob(jobs);
            // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
            oFileRefReq = stoReqService.search(destination.getStorage(), fileMetaInfo.getChecksum());
            oFileRef = fileRefService.search(destination.getStorage(), fileMetaInfo.getChecksum());
            Assert.assertFalse("File reference should have been created.", oFileRef.isPresent());
            Assert.assertTrue("File reference request should exists", oFileRefReq.isPresent());
            Assert.assertEquals("File reference request should be STORE_ERROR status", FileRequestStatus.ERROR,
                                oFileRefReq.get().getStatus());
        } else {
            Assert.assertEquals("File reference request should be in STORE_ERROR status", FileRequestStatus.ERROR,
                                oFileRefReq.get().getStatus());
        }

        return oFileRefReq.get();
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
                futur.get();
            }
        } catch (InterruptedException | ExecutionException e) {
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

    protected void simulateFileInCache(String checksum) {
        try {
            String filePath = cacheService.getFilePath(checksum);
            cacheService.addFile(checksum, 123L, new URL("file", null, filePath), OffsetDateTime.now().plusDays(1),
                                 UUID.randomUUID().toString());
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
