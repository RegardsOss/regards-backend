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
package fr.cnes.regards.modules.storagelight.service.file.flow.performance;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.storagelight.dao.FileReferenceSpecification;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.dto.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileStorageRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.flow.AvailabilityFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.DeletionFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.ReferenceFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.StorageFlowItem;
import fr.cnes.regards.modules.storagelight.service.file.AbstractStorageTest;
import fr.cnes.regards.modules.storagelight.service.file.flow.AvailabilityFlowItemHandler;
import fr.cnes.regards.modules.storagelight.service.file.flow.DeletionFlowHandler;
import fr.cnes.regards.modules.storagelight.service.file.flow.ReferenceFlowItemHandler;
import fr.cnes.regards.modules.storagelight.service.file.flow.StorageFlowItemHandler;

/**
 * Performances tests for creating and store new file references.
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_perf_tests",
        "regards.storage.cache.path=target/cache" })
// @Ignore("Performances tests")
public class FlowPerformanceTest extends AbstractStorageTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowPerformanceTest.class);

    @Autowired
    private ReferenceFlowItemHandler referenceFlowHandler;

    @Autowired
    private StorageFlowItemHandler storeFlowHandler;

    @Autowired
    private AvailabilityFlowItemHandler availabilityHandler;

    @Autowired
    private DeletionFlowHandler deleteHandler;

    private final Set<String> nlChecksums = Sets.newHashSet();

    @Before
    public void initialize() throws ModuleException {

        LOGGER.info("----- Tests initialization -----");
        Mockito.clearInvocations(publisher);

        fileStorageRequestRepo.deleteAll();
        fileCacheReqRepo.deleteAll();
        jobInfoRepo.deleteAll();
        if (!prioritizedDataStorageService.search(ONLINE_CONF_LABEL).isPresent()) {
            initDataStoragePluginConfiguration(ONLINE_CONF_LABEL);
        }

        if (!prioritizedDataStorageService.search(NEARLINE_CONF_LABEL).isPresent()) {
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
                if (toSave.size() >= 10_000) {
                    long start = System.currentTimeMillis();
                    fileRefRepo.saveAll(toSave);
                    LOGGER.info("Saves {} done in {}ms", toSave.size(), System.currentTimeMillis() - start);
                    toSave.clear();
                }
            }

            // Init nearline stored refs
            long start = System.currentTimeMillis();
            fileRefRepo.saveAll(toSave);
            LOGGER.info("Saves {} NearLines done in {}ms", toSave.size(), System.currentTimeMillis() - start);
        }

        Set<FileReference> toSave = Sets.newHashSet();
        for (Long i = 0L; i < 1_000; i++) {
            String checksum = UUID.randomUUID().toString();
            nlChecksums.add(checksum);
            FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(checksum, "UUID", "file_" + i + ".test", i,
                    MediaType.APPLICATION_OCTET_STREAM);
            FileLocation location = new FileLocation(NEARLINE_CONF_LABEL, "storage://plop/file");
            FileReference fileRef = new FileReference(Lists.newArrayList("owner"), metaInfo, location);
            toSave.add(fileRef);
        }
        long start = System.currentTimeMillis();
        fileRefRepo.saveAll(toSave);
        LOGGER.info("Saves {} NearLines done in {}ms", toSave.size(), System.currentTimeMillis() - start);

        try {
            originUrl = new URL("file://in/this/directory/file.test");
        } catch (MalformedURLException e) {
            Assert.fail(e.getMessage());
        }

        LOGGER.info("----- Tests initialization OK-----");
    }

    @Test
    public void referenceFiles() throws InterruptedException {
        LOGGER.info(" --------     REFERENCE TEST     --------- ");
        String storage = "storage" + UUID.randomUUID().toString();
        for (int i = 0; i < 5000; i++) {
            String checksum = UUID.randomUUID().toString();
            // Create a new bus message File reference request
            ;
            ReferenceFlowItem item = ReferenceFlowItem.build(FileReferenceRequestDTO
                    .build("error.file.name", checksum, "MD5", "application/octet-stream", 10L, "owner-test", storage,
                           "file://storage/location/file.name"), UUID.randomUUID().toString());
            TenantWrapper<ReferenceFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
            // Publish request
            referenceFlowHandler.handle(wrapper);
        }

        int loops = 0;
        Page<FileReference> page;
        do {
            Thread.sleep(5_000);
            page = fileRefRepo.findByLocationStorage(storage, PageRequest.of(0, 1, Direction.ASC, "id"));
            loops++;
        } while ((loops < 10) && ((page.getTotalElements()) != 5000));

        Assert.assertEquals("There should be 5000 file ref created", fileRefRepo
                .findByLocationStorage(storage, PageRequest.of(0, 1, Direction.ASC, "id")).getTotalElements(), 5000);
    }

    @Test
    public void storeFiles() throws InterruptedException {
        LOGGER.info(" --------     STORE TEST     --------- ");
        OffsetDateTime now = OffsetDateTime.now();
        for (int i = 0; i < 5000; i++) {
            String checksum = UUID.randomUUID().toString();
            // Create a new bus message File reference request
            StorageFlowItem item = StorageFlowItem
                    .build(FileStorageRequestDTO.build("file.name", checksum, "MD5", "application/octet-stream",
                                                       "owner-test", originUrl, ONLINE_CONF_LABEL, Optional.empty()),
                           UUID.randomUUID().toString());
            TenantWrapper<StorageFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
            // Publish request
            storeFlowHandler.handle(wrapper);
        }
        int loops = 0;
        Page<FileStorageRequest> page;
        do {
            Thread.sleep(10_000);
            page = stoReqService.search(ONLINE_CONF_LABEL, PageRequest.of(0, 1, Direction.ASC, "id"));
            loops++;
        } while ((loops < 10) && ((page.getTotalElements()) != 5000));

        Assert.assertEquals("There should be 5000 file storage request created", 5000, stoReqService
                .search(ONLINE_CONF_LABEL, PageRequest.of(0, 1, Direction.ASC, "id")).getTotalElements());

        Assert.assertEquals("No file ref should be created", 0, fileRefService
                .search(FileReferenceSpecification.search(null, null, null, Lists.newArrayList(ONLINE_CONF_LABEL), null,
                                                          now, null),
                        PageRequest.of(0, 1, Direction.ASC, "id"))
                .getTotalElements());
        long start = System.currentTimeMillis();
        Collection<JobInfo> jobs = stoReqService
                .scheduleJobs(FileRequestStatus.TODO, Lists.newArrayList(ONLINE_CONF_LABEL), Lists.newArrayList());
        Thread.sleep(10_000);
        start = System.currentTimeMillis();
        runAndWaitJob(jobs);
        LOGGER.info("...{} jobs handled in {} ms", jobs.size(), System.currentTimeMillis() - start);
        Assert.assertEquals("There should be no file storage request created", 0, stoReqService
                .search(ONLINE_CONF_LABEL, PageRequest.of(0, 1, Direction.ASC, "id")).getTotalElements());
        Assert.assertEquals("5000 file ref should be created", 5000, fileRefService
                .search(FileReferenceSpecification.search(null, null, null, Lists.newArrayList(ONLINE_CONF_LABEL), null,
                                                          now, null),
                        PageRequest.of(0, 1, Direction.ASC, "id"))
                .getTotalElements());
    }

    @Test
    public void deleteReferencedFiles() throws InterruptedException {
        LOGGER.info(" --------     DELETE TEST     --------- ");
        int nbToDelete = 500;
        Page<FileReference> page = fileRefService.search(PageRequest.of(0, nbToDelete, Direction.ASC, "id"));
        Long total = page.getTotalElements();
        for (FileReference fileRef : page.getContent()) {
            FileDeletionRequestDTO.build(fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(),
                                         fileRef.getOwners().get(0), false);
            DeletionFlowItem item = DeletionFlowItem.build(FileDeletionRequestDTO
                    .build(fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(),
                           fileRef.getOwners().get(0), false), UUID.randomUUID().toString());
            TenantWrapper<DeletionFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
            deleteHandler.handle(wrapper);
        }
        LOGGER.info("Waiting ....");
        int loops = 0;
        do {
            Thread.sleep(5_000);
            page = fileRefService.search(PageRequest.of(0, 1, Direction.ASC, "id"));
            loops++;
        } while ((loops < 10) && (nbToDelete != (total - page.getTotalElements())));

        Assert.assertEquals("500 ref should be deleted", nbToDelete, total - page.getTotalElements());
    }

    @Test
    public void makeAvailableFlowItem() throws InterruptedException {
        LOGGER.info(" --------     AVAILABILITY TEST     --------- ");
        Assert.assertEquals("Invalid count of cached files", 0, cacheFileRepo.count());
        // Create a new bus message File reference request
        AvailabilityFlowItem item = AvailabilityFlowItem.build(nlChecksums, OffsetDateTime.now().plusDays(1),
                                                               UUID.randomUUID().toString());
        TenantWrapper<AvailabilityFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        // Publish request
        availabilityHandler.handle(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Assert.assertEquals("Invalid count of cache file request", nlChecksums.size(), fileCacheReqRepo.count());
    }
}
