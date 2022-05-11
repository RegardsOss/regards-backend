/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.file.flow.performance;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.storage.dao.FileReferenceSpecification;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestDTO;
import fr.cnes.regards.modules.storage.domain.flow.AvailabilityFlowItem;
import fr.cnes.regards.modules.storage.domain.flow.DeletionFlowItem;
import fr.cnes.regards.modules.storage.domain.flow.ReferenceFlowItem;
import fr.cnes.regards.modules.storage.domain.flow.StorageFlowItem;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import fr.cnes.regards.modules.storage.service.file.flow.AvailabilityFlowItemHandler;
import fr.cnes.regards.modules.storage.service.file.flow.DeletionFlowHandler;
import fr.cnes.regards.modules.storage.service.file.flow.ReferenceFlowItemHandler;
import fr.cnes.regards.modules.storage.service.file.flow.StorageFlowItemHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Performances tests for creating and store new file references.
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(
        properties = { "spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.default_schema=storage_perf_tests"},
        locations = { "classpath:application-test.properties" })
@Ignore("Performances tests")
public class FlowPerformanceIT extends AbstractStorageIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowPerformanceIT.class);

    private static final String FILE_REF_OWNER = "owner";

    private static final  String SESSION_OWNER = "SOURCE 1";

    private static final String SESSION = "SESSION 1";

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
        if (!storageLocationConfService.search(ONLINE_CONF_LABEL).isPresent()) {
            initDataStoragePluginConfiguration(ONLINE_CONF_LABEL, true);
        }

        if (!storageLocationConfService.search(NEARLINE_CONF_LABEL).isPresent()) {
            initDataStorageNLPluginConfiguration(NEARLINE_CONF_LABEL);
        }
        storagePlgConfHandler.refresh();
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        if (fileRefRepo.count() == 0) {
            // Insert many refs
            Set<FileReference> toSave = Sets.newHashSet();
            for (Long i = 0L; i < 1_000_000; i++) {
                FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(UUID.randomUUID().toString(), "UUID",
                        "file_" + i + ".test", i, MediaType.APPLICATION_OCTET_STREAM);
                FileLocation location = new FileLocation("storage_" + i, "storage://plop/file");
                FileReference fileRef = new FileReference(Lists.newArrayList(FILE_REF_OWNER), metaInfo, location);
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
            FileReference fileRef = new FileReference(Lists.newArrayList(FILE_REF_OWNER), metaInfo, location);
            toSave.add(fileRef);
        }
        long start = System.currentTimeMillis();
        fileRefRepo.saveAll(toSave);
        LOGGER.info("Saves {} NearLines done in {}ms", toSave.size(), System.currentTimeMillis() - start);

        LOGGER.info("----- Tests initialization OK-----");
    }

    @Test
    public void referenceFileWithManyOwners() {
        String checksum = UUID.randomUUID().toString();
        Set<FileReferenceRequestDTO> requests = Sets.newHashSet();
        List<ReferenceFlowItem> items = new ArrayList<>();
        for (int i = 0; i < 5_000; i++) {
            items.clear();
            requests.clear();
            String newOwner = "owner-" + UUID.randomUUID().toString();
            String sessionOwner = "source-" + i;
            String session = "session-" + i;
            requests.add(FileReferenceRequestDTO.build(checksum, checksum, "MD5", "application/octet-stream", 10L,
                                                       newOwner, "storage", "file://storage/location/file1",
                                                       sessionOwner, session));
            items.add(ReferenceFlowItem.build(requests, UUID.randomUUID().toString()));
            referenceFlowHandler.handleBatch(items);
        }

    }

    @Test
    public void referenceFiles() throws InterruptedException {
        LOGGER.info(" --------     REFERENCE TEST     --------- ");
        String refStorage = "storage-1";
        String storage = "storage" + UUID.randomUUID().toString();
        List<ReferenceFlowItem> items = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            String newOwner = "owner-" + UUID.randomUUID().toString();
            String sessionOwner = "source-" + i;
            String session = "session-" + i;
            String checksum = UUID.randomUUID().toString();
            Set<FileReferenceRequestDTO> requests = Sets.newHashSet();
            requests.add(FileReferenceRequestDTO.build("quicklook.1-" + checksum, UUID.randomUUID().toString(), "MD5",
                                                       "application/octet-stream", 10L, newOwner, refStorage,
                                                       "file://storage/location/quicklook1", sessionOwner, session));
            requests.add(FileReferenceRequestDTO.build("quicklook.2-" + checksum, UUID.randomUUID().toString(), "MD5",
                                                       "application/octet-stream", 10L, newOwner, refStorage,
                                                       "file://storage/location/quicklook1", sessionOwner, session));
            requests.add(FileReferenceRequestDTO.build("quicklook.3-" + checksum, UUID.randomUUID().toString(), "MD5",
                                                       "application/octet-stream", 10L, newOwner, refStorage,
                                                       "file://storage/location/quicklook1", sessionOwner, session));
            requests.add(FileReferenceRequestDTO.build("quicklook.4-" + checksum, UUID.randomUUID().toString(), "MD5",
                                                       "application/octet-stream", 10L, newOwner, refStorage,
                                                       "file://storage/location/quicklook1", sessionOwner, session));
            // Create a new bus message File reference request
            requests.add(FileReferenceRequestDTO.build("file.name", checksum, "MD5", "application/octet-stream", 10L,
                                                       newOwner, storage, "file://storage/location/file.name",
                                                       sessionOwner, session));
            items.add(ReferenceFlowItem.build(requests, UUID.randomUUID().toString()));
            if (items.size() >= referenceFlowHandler.getBatchSize()) {
                referenceFlowHandler.handleBatch(items);
                items.clear();
            }
        }
        if (items.size() > 0) {
            referenceFlowHandler.handleBatch(items);
            items.clear();
        }

        int loops = 0;
        Page<FileReference> page;
        do {
            Thread.sleep(2_000);
            page = fileRefRepo.findByLocationStorage(storage, PageRequest.of(0, 1, Direction.ASC, "id"));
            loops++;
        } while ((loops < 50) && ((page.getTotalElements()) != 5000));

        Assert.assertEquals("There should be 5000 file ref created", 5000, fileRefRepo
                .findByLocationStorage(storage, PageRequest.of(0, 1, Direction.ASC, "id")).getTotalElements());
    }

    @Test
    public void storeFiles() throws InterruptedException {
        LOGGER.info(" --------     STORE TEST     --------- ");
        OffsetDateTime now = OffsetDateTime.now();
        List<StorageFlowItem> items = Lists.newArrayList();
        for (int i = 0; i < 5000; i++) {
            String checksum = UUID.randomUUID().toString();
            // Create a new bus message File reference request
            items.add(StorageFlowItem
                    .build(FileStorageRequestDTO.build("file.name", checksum, "MD5", "application/octet-stream",
                                                       "owner-test", SESSION_OWNER, SESSION, originUrl,
                                                       ONLINE_CONF_LABEL, Optional.empty()),
                           UUID.randomUUID().toString()));

            // Publish request
            if (items.size() > storeFlowHandler.getBatchSize()) {
                storeFlowHandler.handleBatch(items);
                items.clear();
            }
        }
        storeFlowHandler.handleBatch(items);

        Assert.assertEquals("There should be 5000 file storage request created", 5000, stoReqService
                .search(ONLINE_CONF_LABEL, PageRequest.of(0, 1, Direction.ASC, "id")).getTotalElements());

        PageRequest pageable = PageRequest.of(0, 1, Direction.ASC, "id");
        Assert.assertEquals("No file ref should be created", 0, fileRefService.search(FileReferenceSpecification
                .search(null, null, null, Lists.newArrayList(ONLINE_CONF_LABEL), null, now, null, pageable), pageable)
                .getTotalElements());
        long start = System.currentTimeMillis();
        Collection<JobInfo> jobs = stoReqService
                .scheduleJobs(FileRequestStatus.TO_DO, Lists.newArrayList(ONLINE_CONF_LABEL), Lists.newArrayList());
        Thread.sleep(10_000);
        start = System.currentTimeMillis();
        runAndWaitJob(jobs);
        LOGGER.info("...{} jobs handled in {} ms", jobs.size(), System.currentTimeMillis() - start);
        Assert.assertEquals("There should be no file storage request created", 0, stoReqService
                .search(ONLINE_CONF_LABEL, PageRequest.of(0, 1, Direction.ASC, "id")).getTotalElements());
        Assert.assertEquals("5000 file ref should be created", 5000, fileRefService.search(FileReferenceSpecification
                .search(null, null, null, Lists.newArrayList(ONLINE_CONF_LABEL), null, now, null, pageable), pageable)
                .getTotalElements());
    }

    @Test
    public void deleteReferencedFiles() throws InterruptedException {
        LOGGER.info(" --------     DELETE TEST     --------- ");
        int nbToDelete = 500;
        Page<FileReference> page = fileRefService.search(PageRequest.of(0, nbToDelete, Direction.ASC, "id"));
        Long total = page.getTotalElements();
        List<DeletionFlowItem> items = Lists.newArrayList();
        for (FileReference fileRef : page.getContent()) {
            items.add(DeletionFlowItem
                    .build(FileDeletionRequestDTO.build(fileRef.getMetaInfo().getChecksum(),
                                                        fileRef.getLocation().getStorage(), FILE_REF_OWNER,
                                                        SESSION_OWNER, SESSION, false),
                           UUID.randomUUID().toString()));
            if (items.size() > deleteHandler.getBatchSize()) {
                deleteHandler.handleBatch(items);
                items.clear();
            }
        }
        deleteHandler.handleBatch(items);

        page = fileRefService.search(PageRequest.of(0, 1, Direction.ASC, "id"));

        Assert.assertEquals("500 ref should be deleted", nbToDelete, total - page.getTotalElements());
    }

    @Test
    public void deleteStoredFiles() throws InterruptedException {
        LOGGER.info(" --------     DELETE TEST     --------- ");
        int nbToDelete = 500;
        List<DeletionFlowItem> items = Lists.newArrayList();
        Page<FileReference> page = fileRefService.search(NEARLINE_CONF_LABEL,
                                                         PageRequest.of(0, nbToDelete, Direction.ASC, "id"));
        for (FileReference fileRef : page.getContent()) {
            items.add(DeletionFlowItem
                    .build(FileDeletionRequestDTO.build(fileRef.getMetaInfo().getChecksum(),
                                                        fileRef.getLocation().getStorage(), FILE_REF_OWNER,
                                                        SESSION_OWNER, SESSION, false),
                           UUID.randomUUID().toString()));
            if (items.size() > deleteHandler.getBatchSize()) {
                deleteHandler.handleBatch(items);
                items.clear();
            }
        }
        deleteHandler.handleBatch(items);
        LOGGER.info("Waiting ....");
        int loops = 0;
        Page<FileDeletionRequest> pageDel = null;
        do {
            Thread.sleep(500);
            pageDel = fileDeletionRequestService.search(NEARLINE_CONF_LABEL, PageRequest.of(0, 1, Direction.ASC, "id"));
            loops++;
        } while ((loops < 100) && (pageDel.getTotalElements() < nbToDelete));

        Assert.assertEquals("500 deletion requests should be created", nbToDelete, pageDel.getTotalElements());
    }

    @Test
    public void makeAvailableFlowItem() throws InterruptedException {
        LOGGER.info(" --------     AVAILABILITY TEST     --------- ");
        Assert.assertEquals("Invalid count of cached files", 0, cacheFileRepo.count());
        Assert.assertTrue("There should be checksums to restore from nearline storages", nlChecksums.size() > 0);
        // Create a new bus message File reference request
        AvailabilityFlowItem item = AvailabilityFlowItem.build(nlChecksums, OffsetDateTime.now().plusDays(1),
                                                               UUID.randomUUID().toString());
        List<AvailabilityFlowItem> items = new ArrayList<>();
        items.add(item);
        availabilityHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Assert.assertEquals("Invalid count of cache file request", nlChecksums.size(), fileCacheReqRepo.count());
    }
}
