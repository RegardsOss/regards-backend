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
package fr.cnes.regards.modules.storagelight.service.file.reference.flow.performance;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.storagelight.dao.FileReferenceSpecification;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.flow.AddFileRefFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.DeleteFileRefFlowItem;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageType;
import fr.cnes.regards.modules.storagelight.service.file.reference.AbstractFileReferenceTest;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileStorageRequestService;
import fr.cnes.regards.modules.storagelight.service.file.reference.flow.AddFileReferenceFlowItemHandler;
import fr.cnes.regards.modules.storagelight.service.file.reference.flow.DeleteFileReferenceFlowHandler;

/**
 * Performances tests for creating and store new file references.
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests",
        "regards.storage.cache.path=target/cache" })
@Ignore("Performances tests")
public class FlowPerformanceTest extends AbstractFileReferenceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowPerformanceTest.class);

    @Autowired
    private AddFileReferenceFlowItemHandler storageHandler;

    @Autowired
    private DeleteFileReferenceFlowHandler deleteHandler;

    @Autowired
    FileReferenceService fileRefService;

    @Autowired
    FileStorageRequestService fileStorageRequestService;

    @SpyBean
    public IPublisher publisher;

    @Before
    public void initialize() throws ModuleException {
        Mockito.clearInvocations(publisher);

        fileStorageRequestRepo.deleteAll();
        jobInfoRepo.deleteAll();
        prioritizedDataStorageService.search(StorageType.NEARLINE).forEach(c -> {
            try {
                prioritizedDataStorageService.delete(c.getId());
            } catch (ModuleException e) {
                Assert.fail(e.getMessage());
            }
        });
        if (!prioritizedDataStorageService.search(ONLINE_CONF_LABEL).isPresent()) {
            initDataStoragePluginConfiguration(ONLINE_CONF_LABEL);
        }

        if (prioritizedDataStorageService.search(NEARLINE_CONF_LABEL).isPresent()) {
            initDataStorageNLPluginConfiguration(NEARLINE_CONF_LABEL);
        }
        storagePlgConfHandler.refresh();

        if (fileRefRepo.count() == 0) {
            // Insert many refs
            Set<FileReference> toSave = Sets.newHashSet();
            for (Long i = 0L; i < 100_000; i++) {
                FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(UUID.randomUUID().toString(), "UUID",
                        "file_" + i + ".test", i, MediaType.APPLICATION_OCTET_STREAM);
                FileLocation location = new FileLocation("storage_" + i, "storage://plop/file");
                FileReference fileRef = new FileReference(Lists.newArrayList("owner"), metaInfo, location);
                toSave.add(fileRef);
                if (toSave.size() > 10_000) {
                    long start = System.currentTimeMillis();
                    fileRefRepo.saveAll(toSave);
                    LOGGER.info("Saves {} done in {}ms", toSave.size(), System.currentTimeMillis() - start);
                    toSave.clear();
                }
            }
            long start = System.currentTimeMillis();
            fileRefRepo.saveAll(toSave);
            LOGGER.info("Saves {} done in {}ms", toSave.size(), System.currentTimeMillis() - start);
        }
    }

    @Test
    public void addFileRefFlowItems_withoutStorage() throws InterruptedException {
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" --------     Starting     --------- ");
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" ----------------------------------- ");
        String storage = "storage" + UUID.randomUUID().toString();
        for (int i = 0; i < 5000; i++) {
            String checksum = UUID.randomUUID().toString();
            // Create a new bus message File reference request
            AddFileRefFlowItem item = new AddFileRefFlowItem("file.name", checksum, "MD5", "application/octet-stream",
                    10L, "owner-test", storage, "file://storage/location/file.name", storage,
                    "file://storage/location/file.name");
            TenantWrapper<AddFileRefFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
            // Publish request
            storageHandler.handle(wrapper);
        }
        Thread.sleep(30000);
        Assert.assertEquals("There should be 5000 file ref created",
                            fileRefRepo.findByLocationStorage(storage, PageRequest.of(0, 1)).getTotalElements(), 5000);
    }

    @Test
    public void addFileRefFlowItems_withStorage() throws InterruptedException {
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" --------     Starting     --------- ");
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" ----------------------------------- ");
        OffsetDateTime now = OffsetDateTime.now();
        for (int i = 0; i < 5000; i++) {
            String checksum = UUID.randomUUID().toString();
            // Create a new bus message File reference request
            AddFileRefFlowItem item = new AddFileRefFlowItem("file.name", checksum, "MD5", "application/octet-stream",
                    10L, "owner-test", null, "file://storage/location/file.name", ONLINE_CONF_LABEL,
                    "/storage/location");
            TenantWrapper<AddFileRefFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
            // Publish request
            storageHandler.handle(wrapper);
        }
        Thread.sleep(30000);

        Assert.assertEquals("There should be 5000 file storage request created", 5000, fileStorageRequestService
                .search(ONLINE_CONF_LABEL, PageRequest.of(0, 1)).getTotalElements());

        Assert.assertEquals("No file ref should be created", 0, fileRefService.search(FileReferenceSpecification
                .search(null, null, null, Lists.newArrayList(ONLINE_CONF_LABEL), null, now, null), PageRequest.of(0, 1))
                .getTotalElements());
        long start = System.currentTimeMillis();
        Collection<JobInfo> jobs = fileStorageRequestService
                .scheduleJobs(FileRequestStatus.TODO, Lists.newArrayList(ONLINE_CONF_LABEL), Lists.newArrayList());
        LOGGER.info("...{} jobs scheduled in {} ms", jobs.size(), System.currentTimeMillis() - start);
        Thread.sleep(3000);
        start = System.currentTimeMillis();
        runAndWaitJob(jobs);
        LOGGER.info("...{} jobs handled in {} ms", jobs.size(), System.currentTimeMillis() - start);
        Assert.assertEquals("There should be no file storage request created", 0, fileStorageRequestService
                .search(ONLINE_CONF_LABEL, PageRequest.of(0, 1)).getTotalElements());
        Assert.assertEquals("5000 file ref should be created", 5000, fileRefService.search(FileReferenceSpecification
                .search(null, null, null, Lists.newArrayList(ONLINE_CONF_LABEL), null, now, null), PageRequest.of(0, 1))
                .getTotalElements());
    }

    @Test
    public void deleteFileReFlowItems_withoutStorage() throws InterruptedException {
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" --------     Starting     --------- ");
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" ----------------------------------- ");
        LOGGER.info(" ----------------------------------- ");
        int nbToDelete = 500;
        Page<FileReference> page = fileRefService.search(PageRequest.of(0, nbToDelete));
        Long total = page.getTotalElements();
        for (FileReference fileRef : page.getContent()) {
            DeleteFileRefFlowItem item = new DeleteFileRefFlowItem(fileRef.getMetaInfo().getChecksum(),
                    fileRef.getLocation().getStorage(), fileRef.getOwners().get(0));
            TenantWrapper<DeleteFileRefFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
            deleteHandler.handle(wrapper);
        }
        LOGGER.info("Waiting ....");
        Thread.sleep(30000);
        page = fileRefService.search(PageRequest.of(0, 1));
        Assert.assertEquals("500 ref should be deleted", nbToDelete, total - page.getTotalElements());
    }
}
