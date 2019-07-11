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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.compress.utils.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

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
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRequestRepository;
import fr.cnes.regards.modules.storagelight.domain.FileReferenceRequestStatus;
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
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_file_ref",
        "regards.storage.cache.path=target/cache", "regards.storage.cache.minimum.time.to.live.hours=12" })
public class FileReferenceServiceTest extends AbstractMultitenantServiceTest {

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private FileReferenceRequestService fileRefRequestService;

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
    public void init() throws ModuleException {
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
        initDataStoragePluginConfiguration("target");
    }

    @Test
    public void referenceFileWithoutStorage() {
        List<String> owners = Lists.newArrayList();
        owners.add("someone");
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo("invalid_checksum", "MD5", "file.test", 132L,
                MediaType.APPLICATION_OCTET_STREAM);
        FileLocation origin = new FileLocation("anywhere", "anywhere://in/this/directory/file.test");
        fileRefService.createFileReference(owners, fileMetaInfo, origin, origin);
        Optional<FileReference> oFileRef = fileRefService.search(origin.getStorage(), fileMetaInfo.getChecksum());
        Optional<FileReferenceRequest> oFileRefReq = fileRefRequestService.search(origin.getStorage(),
                                                                                  fileMetaInfo.getChecksum());
        Assert.assertTrue("File reference should have been created", oFileRef.isPresent());
        Assert.assertTrue("File reference request should not exists anymore as file is well referenced",
                          !oFileRefReq.isPresent());
    }

    @Test
    public void unknownStorageLocation() {
        List<String> owners = Lists.newArrayList();
        owners.add("someone");
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo("invalid_checksum", "MD5", "file.test", 132L,
                MediaType.APPLICATION_OCTET_STREAM);
        FileLocation origin = new FileLocation("anywhere", "anywhere://in/this/directory/file.test");
        FileLocation destination = new FileLocation("elsewhere", "elsewhere://in/this/directory/file.test");
        fileRefService.createFileReference(owners, fileMetaInfo, origin, destination);
        Optional<FileReference> oFileRef = fileRefService.search(destination.getStorage(), fileMetaInfo.getChecksum());
        Optional<FileReferenceRequest> oFileRefReq = fileRefRequestService.search(destination.getStorage(),
                                                                                  fileMetaInfo.getChecksum());
        Assert.assertFalse("File reference should not have been created. As storage is not possible into an unkown storage location",
                           oFileRef.isPresent());
        Assert.assertTrue("File reference request should exists", oFileRefReq.isPresent());
        Assert.assertTrue("File reference request should be in STORE_ERROR status",
                          oFileRefReq.get().getStatus().equals(FileReferenceRequestStatus.STORE_ERROR));
    }

    @Test
    public void storeWithPlugin() throws InterruptedException, ExecutionException {
        List<String> owners = Lists.newArrayList();
        owners.add("someone");
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo("invalid_checksum", "MD5", "file.test", 132L,
                MediaType.APPLICATION_OCTET_STREAM);
        FileLocation origin = new FileLocation("anywhere", "anywhere://in/this/directory/file.test");
        FileLocation destination = new FileLocation("target", "/in/this/directory");
        // Run file reference creation.
        fileRefService.createFileReference(owners, fileMetaInfo, origin, destination);
        // The file reference should exist yet cause a storage job is needed. Nevertheless a FileReferenceRequest should be created.
        Optional<FileReference> oFileRef = fileRefService.search(destination.getStorage(), fileMetaInfo.getChecksum());
        Optional<FileReferenceRequest> oFileRefReq = fileRefRequestService.search(destination.getStorage(),
                                                                                  fileMetaInfo.getChecksum());
        Assert.assertFalse("File reference should not have been created yet.", oFileRef.isPresent());
        Assert.assertTrue("File reference request should exists", oFileRefReq.isPresent());
        Assert.assertTrue("File reference request should be in TO_STORE status",
                          oFileRefReq.get().getStatus().equals(FileReferenceRequestStatus.TO_STORE));
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Collection<JobInfo> jobs = fileRefRequestService.scheduleStoreJobs();
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        String tenant = runtimeTenantResolver.getTenant();
        jobService.runJob(jobs.iterator().next(), tenant).get();
        runtimeTenantResolver.forceTenant(tenant);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        oFileRefReq = fileRefRequestService.search(destination.getStorage(), fileMetaInfo.getChecksum());
        oFileRef = fileRefService.search(destination.getStorage(), fileMetaInfo.getChecksum());
        Assert.assertTrue("File reference should have been created.", oFileRef.isPresent());
        Assert.assertFalse("File reference request should exists", oFileRefReq.isPresent());
    }

    @Test
    public void storeWithPluginError() throws InterruptedException, ExecutionException {
        List<String> owners = Lists.newArrayList();
        owners.add("someone");
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo("invalid_checksum", "MD5", "error.file.test",
                132L, MediaType.APPLICATION_OCTET_STREAM);
        FileLocation origin = new FileLocation("anywhere", "anywhere://in/this/directory/error.file.test");
        FileLocation destination = new FileLocation("target", "/in/this/directory");
        // Run file reference creation.
        fileRefService.createFileReference(owners, fileMetaInfo, origin, destination);
        // The file reference should exist yet cause a storage job is needed. Nevertheless a FileReferenceRequest should be created.
        Optional<FileReference> oFileRef = fileRefService.search(destination.getStorage(), fileMetaInfo.getChecksum());
        Optional<FileReferenceRequest> oFileRefReq = fileRefRequestService.search(destination.getStorage(),
                                                                                  fileMetaInfo.getChecksum());
        Assert.assertFalse("File reference should not have been created yet.", oFileRef.isPresent());
        Assert.assertTrue("File reference request should exists", oFileRefReq.isPresent());
        Assert.assertTrue("File reference request should be in TO_STORE status",
                          oFileRefReq.get().getStatus().equals(FileReferenceRequestStatus.TO_STORE));
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Collection<JobInfo> jobs = fileRefRequestService.scheduleStoreJobs();
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
        Assert.assertTrue("File reference request should be STORE_ERROR status",
                          oFileRefReq.get().getStatus().equals(FileReferenceRequestStatus.STORE_ERROR));
    }

    private PrioritizedDataStorage initDataStoragePluginConfiguration(String label) throws ModuleException {
        try {
            PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOnlineDataStorage.class);
            URL baseStorageLocation = new URL("file", "",
                    Paths.get("target/simpleOnlineStorage").toFile().getAbsolutePath());
            Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
            Set<PluginParameter> parameters = PluginParametersFactory.build()
                    .addParameter(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                  baseStorageLocation.toString())
                    .addParameter(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, "error.*").getParameters();
            PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, label, parameters, 0);
            dataStorageConf.setIsActive(true);
            return prioritizedDataStorageService.create(dataStorageConf);
        } catch (IOException | URISyntaxException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }
}
