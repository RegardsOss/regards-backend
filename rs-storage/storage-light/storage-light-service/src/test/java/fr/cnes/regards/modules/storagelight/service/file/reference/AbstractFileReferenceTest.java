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
package fr.cnes.regards.modules.storagelight.service.file.reference;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.assertj.core.util.Sets;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storagelight.dao.IFileDeletetionRequestRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.database.PrioritizedStorage;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageType;
import fr.cnes.regards.modules.storagelight.service.plugin.SimpleOnlineDataStorage;
import fr.cnes.regards.modules.storagelight.service.storage.PrioritizedStorageService;
import fr.cnes.regards.modules.storagelight.service.storage.flow.StoragePluginConfigurationHandler;

/**
 * @author sbinda
 *
 */
public abstract class AbstractFileReferenceTest extends AbstractMultitenantServiceTest {

    protected static final String ONLINE_CONF_LABEL = "target";

    @Autowired
    protected FileReferenceService fileRefService;

    @Autowired
    protected FileStorageRequestService fileRefRequestService;

    @Autowired
    protected FileDeletionRequestService fileDeletionRequestService;

    @Autowired
    protected IJobService jobService;

    @Autowired
    protected StoragePluginConfigurationHandler storageHandler;

    @Autowired
    protected IFileReferenceRepository fileRefRepo;

    @Autowired
    protected IFileStorageRequestRepository fileRefRequestRepo;

    @Autowired
    protected IFileDeletetionRequestRepository fileDeletionRequestRepo;

    @Autowired
    protected IJobInfoRepository jobInfoRepo;

    @Autowired
    protected PrioritizedStorageService prioritizedDataStorageService;

    protected void init() throws ModuleException {
        fileDeletionRequestRepo.deleteAll();
        fileRefRequestRepo.deleteAll();
        fileRefRepo.deleteAll();
        jobInfoRepo.deleteAll();
        prioritizedDataStorageService.search(StorageType.ONLINE).forEach(c -> {
            try {
                prioritizedDataStorageService.delete(c.getId());
            } catch (ModuleException e) {
                Assert.fail(e.getMessage());
            }
        });
        initDataStoragePluginConfiguration(ONLINE_CONF_LABEL);
        storageHandler.refresh();
    }

    protected PrioritizedStorage initDataStoragePluginConfiguration(String label) throws ModuleException {
        try {
            PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOnlineDataStorage.class);
            Files.createDirectories(Paths.get(getBaseStorageLocation().toURI()));
            Set<PluginParameter> parameters = PluginParametersFactory.build()
                    .addParameter(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                  getBaseStorageLocation().toString())
                    .addParameter(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, "error.*")
                    .addParameter(SimpleOnlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN, "delErr.*").getParameters();
            PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, label, parameters, 0);
            dataStorageConf.setIsActive(true);
            return prioritizedDataStorageService.create(dataStorageConf);
        } catch (IOException | URISyntaxException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

    protected void updatePluginConfForError(String newErrorPattern) throws MalformedURLException, ModuleException {
        PrioritizedStorage conf = prioritizedDataStorageService.getFirstActive(StorageType.ONLINE);
        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                              getBaseStorageLocation().toString())
                .addParameter(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, newErrorPattern)
                .addParameter(SimpleOnlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN, "delErr.*").getParameters();
        conf.getStorageConfiguration().setParameters(parameters);
        prioritizedDataStorageService.update(conf.getId(), conf);
    }

    protected FileReference generateRandomStoredFileReference() throws InterruptedException, ExecutionException {
        return this.generateStoredFileReference(UUID.randomUUID().toString(), "someone", "file.test");
    }

    protected Optional<FileReference> generateStoredFileReferenceAlreadyReferenced(String checksum, String storage,
            String newOwner) {
        Optional<FileReference> oFilef = fileRefService.search(storage, checksum);
        Assert.assertTrue("File reference should already exists", oFilef.isPresent());
        FileLocation origin = new FileLocation("anywhere", "anywhere://in/this/directory/file.test");
        return fileRefService.addFileReference(Lists.newArrayList(newOwner), oFilef.get().getMetaInfo(), origin,
                                               oFilef.get().getLocation());

    }

    protected FileReference generateStoredFileReference(String checksum, String owner, String fileName)
            throws InterruptedException, ExecutionException {
        List<String> owners = Lists.newArrayList();
        owners.add(owner);
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum, "MD5", fileName, 132L,
                MediaType.APPLICATION_OCTET_STREAM);
        FileLocation origin = new FileLocation("anywhere", "anywhere://in/this/directory/" + fileName);
        FileLocation destination = new FileLocation(ONLINE_CONF_LABEL, "/in/this/directory");
        // Run file reference creation.
        fileRefService.addFileReference(owners, fileMetaInfo, origin, destination);
        // The file reference should exist yet cause a storage job is needed. Nevertheless a FileReferenceRequest should be created.
        Optional<FileReference> oFileRef = fileRefService.search(destination.getStorage(), checksum);
        Optional<FileStorageRequest> oFileRefReq = fileRefRequestService.search(destination.getStorage(), checksum);
        Assert.assertFalse("File reference should not have been created yet.", oFileRef.isPresent());
        Assert.assertTrue("File reference request should exists", oFileRefReq.isPresent());
        Assert.assertEquals("File reference request should be in TO_STORE status", FileRequestStatus.TODO,
                            oFileRefReq.get().getStatus());
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Collection<JobInfo> jobs = fileRefRequestService.scheduleStoreJobs(FileRequestStatus.TODO, null, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        runAndWaitJob(jobs);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        oFileRefReq = fileRefRequestService.search(destination.getStorage(), checksum);
        oFileRef = fileRefService.search(destination.getStorage(), checksum);
        Assert.assertTrue("File reference should have been created.", oFileRef.isPresent());
        Assert.assertFalse("File reference request should not exists anymore", oFileRefReq.isPresent());
        return oFileRef.get();
    }

    protected Optional<FileReference> referenceFile(String checksum, Collection<String> owners, String type,
            String fileName, String storage) {
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum, "MD5", fileName, 132L,
                MediaType.APPLICATION_OCTET_STREAM);
        fileMetaInfo.setType(type);
        FileLocation origin = new FileLocation(storage, "anywhere://in/this/directory/file.test");
        fileRefService.addFileReference(owners, fileMetaInfo, origin, origin);
        return fileRefService.search(origin.getStorage(), fileMetaInfo.getChecksum());
    }

    protected Optional<FileReference> referenceRandomFile(List<String> owners, String type, String fileName,
            String storage) {
        return this.referenceFile(UUID.randomUUID().toString(), owners, type, fileName, storage);
    }

    protected FileStorageRequest generateStoreFileError(String owner, String storageDestination)
            throws InterruptedException, ExecutionException {
        List<String> owners = Lists.newArrayList();
        owners.add(owner);
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(UUID.randomUUID().toString(), "MD5",
                "error.file.test", 132L, MediaType.APPLICATION_OCTET_STREAM);
        FileLocation origin = new FileLocation("anywhere", "anywhere://in/this/directory/error.file.test");
        FileLocation destination = new FileLocation(storageDestination, "/in/this/directory");
        // Run file reference creation.
        fileRefService.addFileReference(owners, fileMetaInfo, origin, destination);
        // The file reference should exist yet cause a storage job is needed. Nevertheless a FileReferenceRequest should be created.
        Optional<FileReference> oFileRef = fileRefService.search(destination.getStorage(), fileMetaInfo.getChecksum());
        Optional<FileStorageRequest> oFileRefReq = fileRefRequestService.search(destination.getStorage(),
                                                                                fileMetaInfo.getChecksum());
        Assert.assertFalse("File reference should not have been created yet.", oFileRef.isPresent());
        Assert.assertTrue("File reference request should exists", oFileRefReq.isPresent());
        if (storageDestination.equals(ONLINE_CONF_LABEL)) {
            Assert.assertEquals("File reference request should be in TO_STORE status", FileRequestStatus.TODO,
                                oFileRefReq.get().getStatus());
            // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
            Collection<JobInfo> jobs = fileRefRequestService.scheduleStoreJobs(FileRequestStatus.TODO,
                                                                               Sets.newHashSet(), Sets.newHashSet());
            Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
            // Run Job and wait for end
            runAndWaitJob(jobs);
            // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
            oFileRefReq = fileRefRequestService.search(destination.getStorage(), fileMetaInfo.getChecksum());
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

    protected URL getBaseStorageLocation() throws MalformedURLException {
        return new URL("file", "", Paths.get("target/simpleOnlineStorage").toFile().getAbsolutePath());
    }

    protected void runAndWaitJob(Collection<JobInfo> jobs) {
        // Run Job and wait for end
        String tenant = runtimeTenantResolver.getTenant();
        try {
            jobService.runJob(jobs.iterator().next(), tenant).get();
        } catch (InterruptedException | ExecutionException e) {
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

}
