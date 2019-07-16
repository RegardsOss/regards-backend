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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.assertj.core.util.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;

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
import fr.cnes.regards.modules.storagelight.dao.FileReferenceSpecification;
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRequestRepository;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceRequest;
import fr.cnes.regards.modules.storagelight.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storagelight.domain.plugin.DataStorageType;
import fr.cnes.regards.modules.storagelight.service.plugin.SimpleOnlineDataStorage;

/**
 * @author sbinda
 *
 */
@ActiveProfiles("disableStorageTasks")
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests",
        "regards.storage.cache.path=target/cache", "regards.storage.cache.minimum.time.to.live.hours=12" })
public class FileReferenceServiceTest extends AbstractMultitenantServiceTest {

    private static final String ONLINE_CONF_LABEL = "target";

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private FileReferenceRequestService fileRefRequestService;

    @Autowired
    private StoragePluginConfigurationHandler storageHandler;

    @Autowired
    private IFileReferenceRepository fileRefRepo;

    @Autowired
    private IFileReferenceRequestRepository fileRefRequestRepo;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @Autowired
    private IJobService jobService;

    @Autowired
    private PrioritizedDataStorageService prioritizedDataStorageService;

    @Before
    public void init() throws ModuleException, InterruptedException {
        fileRefRepo.deleteAll();
        fileRefRequestRepo.deleteAll();
        jobInfoRepo.deleteAll();
        prioritizedDataStorageService.findAllByType(DataStorageType.ONLINE).forEach(c -> {
            try {
                prioritizedDataStorageService.delete(c.getId());
            } catch (ModuleException e) {
                Assert.fail(e.getMessage());
            }
        });
        initDataStoragePluginConfiguration(ONLINE_CONF_LABEL);
        int limit = 0;
        while (!storageHandler.getConfiguredStorages().contains(ONLINE_CONF_LABEL) && (limit < 20)) {
            Thread.sleep(100);
            limit++;
        }
        if (limit == 20) {
            Assert.fail("Plugin is still not referenced in service !");
        }
    }

    @Test
    public void unknownStorageLocation() {
        List<String> owners = Lists.newArrayList();
        owners.add("someone");
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo("invalid_checksum", "MD5", "file.test", 132L,
                MediaType.APPLICATION_OCTET_STREAM);
        FileLocation origin = new FileLocation("anywhere", "anywhere://in/this/directory/file.test");
        FileLocation destination = new FileLocation("elsewhere", "elsewhere://in/this/directory/file.test");
        fileRefService.addFileReference(owners, fileMetaInfo, origin, destination);
        Optional<FileReference> oFileRef = fileRefService.search(destination.getStorage(), fileMetaInfo.getChecksum());
        Optional<FileReferenceRequest> oFileRefReq = fileRefRequestService.search(destination.getStorage(),
                                                                                  fileMetaInfo.getChecksum());
        Assert.assertFalse("File reference should not have been created. As storage is not possible into an unkown storage location",
                           oFileRef.isPresent());
        Assert.assertTrue("File reference request should exists", oFileRefReq.isPresent());
        Assert.assertTrue("File reference request should be in STORE_ERROR status",
                          oFileRefReq.get().getStatus().equals(FileRequestStatus.ERROR));
    }

    @Test
    public void referenceFileWithoutStorage() {
        List<String> owners = Lists.newArrayList();
        owners.add("someone");
        generateFileReference(owners, Lists.newArrayList(), "file.test", "anywhere");
    }

    @Test
    public void search() throws InterruptedException {
        // 1. Add reference for search tests
        List<String> owners = Lists.newArrayList("someone");
        List<String> types = Lists.newArrayList();
        OffsetDateTime beforeDate = OffsetDateTime.now().minusSeconds(1);
        FileReference fileRef = generateFileReference(owners, types, "file1.test", "anywhere");
        OffsetDateTime afterFirstDate = OffsetDateTime.now();
        Thread.sleep(1);
        owners.add("someone-else");
        types.add("Type1");
        generateFileReference(owners, types, "file2.test", "somewhere-else");
        types.add("Type2");
        owners.add("someone-else-again");
        generateFileReference(owners, types, "file3.test", "somewhere-else");
        types.add("Test");
        generateFileReference(owners, types, "data_4.nc", "somewhere-else");
        generateFileReference(owners, types, "data_5.nc", "void");
        OffsetDateTime afterEndDate = OffsetDateTime.now().plusSeconds(1);

        // Search all
        Assert.assertEquals("There should be 5 file references.", 5,
                            fileRefService.search(PageRequest.of(0, 100)).getTotalElements());
        // Search by fileName
        Assert.assertEquals("There should be one file references named file1.test.", 1,
                            fileRefService
                                    .search(FileReferenceSpecification.search("file1.test", null, null, null, null,
                                                                              null, null),
                                            PageRequest.of(0, 100))
                                    .getTotalElements());
        Assert.assertEquals("There should be 3 file references with name containing file", 3,
                            fileRefService
                                    .search(FileReferenceSpecification.search("file", null, null, null, null, null,
                                                                              null),
                                            PageRequest.of(0, 100))
                                    .getTotalElements());
        // Search by checksum
        Assert.assertEquals("There should be one file references with checksum given", 1,
                            fileRefService.search(FileReferenceSpecification
                                    .search(null, fileRef.getMetaInfo().getChecksum(), null, null, null, null, null),
                                                  PageRequest.of(0, 100))
                                    .getTotalElements());
        // Search by storage
        Assert.assertEquals("There should be 5 file references in given storages", 5,
                            fileRefService.search(
                                                  FileReferenceSpecification
                                                          .search(null, null, null,
                                                                  Sets.newLinkedHashSet("anywhere", "somewhere-else",
                                                                                        "void"),
                                                                  null, null, null),
                                                  PageRequest.of(0, 100))
                                    .getTotalElements());
        Assert.assertEquals("There should be 3 file references in given storages", 3, fileRefService
                .search(FileReferenceSpecification.search(null, null, null, Sets.newLinkedHashSet("somewhere-else"),
                                                          null, null, null),
                        PageRequest.of(0, 100))
                .getTotalElements());
        // Search by type
        Assert.assertEquals("There should be 0 file references for given type", 0,
                            fileRefService.search(FileReferenceSpecification
                                    .search(null, null, Lists.newArrayList("Type0"), null, null, null, null),
                                                  PageRequest.of(0, 100))
                                    .getTotalElements());
        Assert.assertEquals("There should be 3 file references for given type", 3,
                            fileRefService.search(FileReferenceSpecification
                                    .search(null, null, Sets.newLinkedHashSet("Type2"), null, null, null, null),
                                                  PageRequest.of(0, 100))
                                    .getTotalElements());
        // Search by date
        Assert.assertEquals("There should be 5 file references for given from date", 5,
                            fileRefService
                                    .search(FileReferenceSpecification.search(null, null, null, null, null, beforeDate,
                                                                              null),
                                            PageRequest.of(0, 100))
                                    .getTotalElements());
        Assert.assertEquals("There should be 4 file references for given from and to date", 4, fileRefService
                .search(FileReferenceSpecification.search(null, null, null, null, null, afterFirstDate, afterEndDate),
                        PageRequest.of(0, 100))
                .getTotalElements());

    }

    @Test
    public void storeWithPlugin() throws InterruptedException, ExecutionException {
        List<String> owners = Lists.newArrayList();
        owners.add("someone");
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo("invalid_checksum", "MD5", "file.test", 132L,
                MediaType.APPLICATION_OCTET_STREAM);
        FileLocation origin = new FileLocation("anywhere", "anywhere://in/this/directory/file.test");
        FileLocation destination = new FileLocation(ONLINE_CONF_LABEL, "/in/this/directory");
        // Run file reference creation.
        fileRefService.addFileReference(owners, fileMetaInfo, origin, destination);
        // The file reference should exist yet cause a storage job is needed. Nevertheless a FileReferenceRequest should be created.
        Optional<FileReference> oFileRef = fileRefService.search(destination.getStorage(), fileMetaInfo.getChecksum());
        Optional<FileReferenceRequest> oFileRefReq = fileRefRequestService.search(destination.getStorage(),
                                                                                  fileMetaInfo.getChecksum());
        Assert.assertFalse("File reference should not have been created yet.", oFileRef.isPresent());
        Assert.assertTrue("File reference request should exists", oFileRefReq.isPresent());
        Assert.assertEquals("File reference request should be in TO_STORE status", FileRequestStatus.TODO,
                            oFileRefReq.get().getStatus());
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Collection<JobInfo> jobs = fileRefRequestService.scheduleStoreJobs(FileRequestStatus.TODO, null, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        String tenant = runtimeTenantResolver.getTenant();
        jobService.runJob(jobs.iterator().next(), tenant).get();
        runtimeTenantResolver.forceTenant(tenant);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        oFileRefReq = fileRefRequestService.search(destination.getStorage(), fileMetaInfo.getChecksum());
        oFileRef = fileRefService.search(destination.getStorage(), fileMetaInfo.getChecksum());
        Assert.assertTrue("File reference should have been created.", oFileRef.isPresent());
        Assert.assertFalse("File reference request should not exists anymore", oFileRefReq.isPresent());
    }

    @Test
    public void storeWithPluginError() throws InterruptedException, ExecutionException {
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
    }

    @Test
    public void retryStoreErrors()
            throws InterruptedException, ExecutionException, ModuleException, MalformedURLException {
        FileReferenceRequest fileRefReq = this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        // Update plugin conf to now accept error files
        this.updatePluginConfForError("unknown.*");
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Collection<JobInfo> jobs = fileRefRequestService.scheduleStoreJobs(FileRequestStatus.ERROR, null, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        String tenant = runtimeTenantResolver.getTenant();
        jobService.runJob(jobs.iterator().next(), tenant).get();
        runtimeTenantResolver.forceTenant(tenant);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        Optional<FileReferenceRequest> oFileRefReq = fileRefRequestService
                .search(fileRefReq.getDestination().getStorage(), fileRefReq.getMetaInfo().getChecksum());
        Optional<FileReference> oFileRef = fileRefService.search(fileRefReq.getDestination().getStorage(),
                                                                 fileRefReq.getMetaInfo().getChecksum());
        Assert.assertTrue("File reference should have been created.", oFileRef.isPresent());
        Assert.assertFalse("File reference request should not exists anymore", oFileRefReq.isPresent());
    }

    @Test
    public void retryMultipleStoreErrors()
            throws InterruptedException, ExecutionException, ModuleException, MalformedURLException {
        FileReferenceRequest fileRefReq = this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        // Update plugin conf to now accept error files
        this.updatePluginConfForError("unknown.*");
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Collection<JobInfo> jobs = fileRefRequestService.scheduleStoreJobs(FileRequestStatus.ERROR, null, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        String tenant = runtimeTenantResolver.getTenant();
        jobService.runJob(jobs.iterator().next(), tenant).get();
        runtimeTenantResolver.forceTenant(tenant);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        Page<FileReferenceRequest> fileRefReqs = fileRefRequestService.search(fileRefReq.getDestination().getStorage(),
                                                                              PageRequest.of(0, 1000));
        Page<FileReference> fileRefs = fileRefService.search(fileRefReq.getDestination().getStorage(),
                                                             PageRequest.of(0, 1000));
        Assert.assertEquals("File references should have been created.", 3, fileRefs.getContent().size());
        Assert.assertTrue("File reference requests should not exists anymore", fileRefReqs.getContent().isEmpty());
    }

    @Test
    public void retryMultipleStoreErrorsByOwner()
            throws InterruptedException, ExecutionException, ModuleException, MalformedURLException {
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        FileReferenceRequest fileRefReq1 = this.generateStoreFileError("someone-else", ONLINE_CONF_LABEL);
        FileReferenceRequest fileRefReq2 = this.generateStoreFileError("someone-else", ONLINE_CONF_LABEL);
        // Update plugin conf to now accept error files
        this.updatePluginConfForError("unknown.*");
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Set<String> owners = Sets.newLinkedHashSet("someone-else");
        Collection<JobInfo> jobs = fileRefRequestService.scheduleStoreJobs(FileRequestStatus.ERROR, null, owners);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        String tenant = runtimeTenantResolver.getTenant();
        jobService.runJob(jobs.iterator().next(), tenant).get();
        runtimeTenantResolver.forceTenant(tenant);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        Page<FileReferenceRequest> fileRefReqs = fileRefRequestService.search(ONLINE_CONF_LABEL,
                                                                              PageRequest.of(0, 1000));
        Page<FileReference> fileRefs = fileRefService.search(ONLINE_CONF_LABEL, PageRequest.of(0, 1000));
        Assert.assertEquals("File references should have been created for the given owner.", 2,
                            fileRefs.getContent().size());
        Assert.assertTrue("File references should have been created for the given owner.", fileRefs.getContent()
                .stream()
                .anyMatch(fr -> fr.getMetaInfo().getChecksum().equals(fileRefReq1.getMetaInfo().getChecksum())));
        Assert.assertTrue("File references should have been created for the given owner.", fileRefs.getContent()
                .stream()
                .anyMatch(fr -> fr.getMetaInfo().getChecksum().equals(fileRefReq2.getMetaInfo().getChecksum())));
        Assert.assertEquals("File reference requests should not exists anymore for the given owner", 3,
                            fileRefReqs.getContent().size());
    }

    @Test
    public void retryMultipleStoreErrorsByStorage()
            throws InterruptedException, ExecutionException, ModuleException, MalformedURLException {
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        FileReferenceRequest fileRefReqOther = this.generateStoreFileError("someone", "other-target");
        // Update plugin conf to now accept error files
        this.updatePluginConfForError("unknown.*");
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Set<String> storages = Sets.newLinkedHashSet(ONLINE_CONF_LABEL);
        Collection<JobInfo> jobs = fileRefRequestService.scheduleStoreJobs(FileRequestStatus.ERROR, storages, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        String tenant = runtimeTenantResolver.getTenant();
        jobService.runJob(jobs.iterator().next(), tenant).get();
        runtimeTenantResolver.forceTenant(tenant);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        Page<FileReferenceRequest> fileRefReqs = fileRefRequestService.search(PageRequest.of(0, 1000));
        Page<FileReference> fileRefs = fileRefService.search(PageRequest.of(0, 1000));
        Assert.assertEquals("File references should have been created.", 2, fileRefs.getContent().size());
        Assert.assertEquals("File reference requests should not exists anymore for given storage", 1,
                            fileRefReqs.getContent().size());
        Assert.assertTrue("File references request should still exists for other storage.", fileRefReqs.getContent()
                .stream()
                .anyMatch(frr -> frr.getMetaInfo().getChecksum().equals(fileRefReqOther.getMetaInfo().getChecksum())));
    }

    private PrioritizedDataStorage initDataStoragePluginConfiguration(String label) throws ModuleException {
        try {
            PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOnlineDataStorage.class);
            Files.createDirectories(Paths.get(getBaseStorageLocation().toURI()));
            Set<PluginParameter> parameters = PluginParametersFactory.build()
                    .addParameter(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                  getBaseStorageLocation().toString())
                    .addParameter(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, "error.*").getParameters();
            PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, label, parameters, 0);
            dataStorageConf.setIsActive(true);
            return prioritizedDataStorageService.create(dataStorageConf);
        } catch (IOException | URISyntaxException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

    private void updatePluginConfForError(String newErrorPattern) throws MalformedURLException, ModuleException {
        PrioritizedDataStorage conf = prioritizedDataStorageService.getFirstActiveByType(DataStorageType.ONLINE);
        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                              getBaseStorageLocation().toString())
                .addParameter(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, newErrorPattern)
                .getParameters();
        conf.getDataStorageConfiguration().setParameters(parameters);
        prioritizedDataStorageService.update(conf.getId(), conf);
    }

    private URL getBaseStorageLocation() throws MalformedURLException {
        return new URL("file", "", Paths.get("target/simpleOnlineStorage").toFile().getAbsolutePath());
    }

    private FileReference generateFileReference(List<String> owners, List<String> types, String fileName,
            String storage) {
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(UUID.randomUUID().toString(), "MD5", fileName,
                132L, MediaType.APPLICATION_OCTET_STREAM);
        fileMetaInfo.setTypes(types);
        FileLocation origin = new FileLocation(storage, "anywhere://in/this/directory/file.test");
        fileRefService.addFileReference(owners, fileMetaInfo, origin, origin);
        Optional<FileReference> oFileRef = fileRefService.search(origin.getStorage(), fileMetaInfo.getChecksum());
        Optional<FileReferenceRequest> oFileRefReq = fileRefRequestService.search(origin.getStorage(),
                                                                                  fileMetaInfo.getChecksum());
        Assert.assertTrue("File reference should have been created", oFileRef.isPresent());
        Assert.assertTrue("File reference request should not exists anymore as file is well referenced",
                          !oFileRefReq.isPresent());
        return oFileRef.get();
    }

    private FileReferenceRequest generateStoreFileError(String owner, String storageDestination)
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
        Optional<FileReferenceRequest> oFileRefReq = fileRefRequestService.search(destination.getStorage(),
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
            String tenant = runtimeTenantResolver.getTenant();
            jobService.runJob(jobs.iterator().next(), tenant).get();
            runtimeTenantResolver.forceTenant(tenant);
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
}
