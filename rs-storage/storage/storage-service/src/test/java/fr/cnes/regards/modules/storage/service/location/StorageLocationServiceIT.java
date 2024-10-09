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
package fr.cnes.regards.modules.storage.service.location;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.StorageType;
import fr.cnes.regards.modules.filecatalog.dto.StorageLocationDto;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.dao.IGroupRequestInfoRepository;
import fr.cnes.regards.modules.storage.dao.IStorageLocationRepository;
import fr.cnes.regards.modules.storage.dao.IStorageMonitoringRepository;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.StorageLocation;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import fr.cnes.regards.modules.storage.service.file.request.FileReferenceRequestService;
import fr.cnes.regards.modules.storage.service.session.SessionNotifierPropertyEnum;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;

import java.nio.file.Paths;
import java.util.*;

/**
 * Test class
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler", "nojobs" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests" },
                    locations = { "classpath:application-test.properties" })
public class StorageLocationServiceIT extends AbstractStorageIT {

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private IFileReferenceRepository fileRefRepo;

    @Autowired
    private IStorageLocationRepository storageLocationRepo;

    @Autowired
    private IGroupRequestInfoRepository requInfoRepo;

    @Autowired
    private IStorageMonitoringRepository storageMonitorRepo;

    @Autowired
    private FileReferenceRequestService fileRefService;

    private Long initStorageLocations(String storage) {
        Long totalSize = 0L;
        createFileReference(storage, 1024L);
        totalSize++;
        createFileReference(storage, 1024L, true);
        totalSize++;
        createFileReference(storage, 1024L);
        totalSize++;
        createFileReference(storage, 1024L);
        totalSize++;
        storageLocationService.monitorStorageLocations(false);
        return totalSize;
    }

    @Before
    public void initialize() throws ModuleException {
        requInfoRepo.deleteAll();
        fileRefRepo.deleteAll();
        storageLocationRepo.deleteAll();
        storageMonitorRepo.deleteAll();
        Mockito.clearInvocations(publisher);
        super.init();
    }

    private FileReference createFileReference(String storage, Long fileSize) {
        return this.createFileReference(storage, fileSize, false);
    }

    private FileReference createFileReference(String storage, Long fileSize, boolean pendingActionRemaining) {
        String checksum = RandomChecksumUtils.generateRandomChecksum();
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum,
                                                                       "MD5",
                                                                       "file.test",
                                                                       fileSize,
                                                                       MediaType.APPLICATION_OCTET_STREAM);
        FileLocation location = new FileLocation(storage,
                                                 "anywhere://in/this/directory/" + checksum,
                                                 pendingActionRemaining);
        try {
            return fileRefService.reference("someone",
                                            fileMetaInfo,
                                            location,
                                            Sets.newHashSet(UUID.randomUUID().toString()),
                                            "defaultSessionOwner",
                                            "defaultSession").getFileReference();
        } catch (ModuleException e) {
            Assert.fail(e.getMessage());
            return null;
        }
    }

    @Test
    public void runPeriodicActions() throws ModuleException {
        initStorageLocations(NEARLINE_CONF_LABEL);
        Optional<StorageLocationDto> location = storageLocationService.getAllLocations()
                                                                      .stream()
                                                                      .filter(l -> l.getName()
                                                                                    .equals(NEARLINE_CONF_LABEL))
                                                                      .findFirst();
        Assert.assertTrue(location.isPresent());
        Assert.assertEquals(4L, location.get().getNbFilesStored());
        Assert.assertEquals(1L, location.get().getNbFilesStoredWithPendingActionRemaining());

        // Now run periodic action, the SimpleNearLinePlugin automaticly inform system that all remaining actions are done.
        Set<JobInfo> jobs = storageLocationService.runPeriodicTasks();
        Assert.assertEquals(1, jobs.size());
        runAndWaitJob(jobs);

        // Run monitoring to update snapshot of storages
        storageLocationService.monitorStorageLocations(false);
        location = storageLocationService.getAllLocations()
                                         .stream()
                                         .filter(l -> l.getName().equals(NEARLINE_CONF_LABEL))
                                         .findFirst();
        Assert.assertTrue(location.isPresent());
        // Now 0 files should be in pending state
        Assert.assertEquals(4L, location.get().getNbFilesStored());
        Assert.assertEquals(0L, location.get().getNbFilesStoredWithPendingActionRemaining());
    }

    @Test
    public void monitorStorageLocation() {
        String storage = "STAF";
        Assert.assertFalse("0. No location should be referenced yet",
                           storageLocationService.search(storage).isPresent());
        Long totalSize = initStorageLocations(storage);
        storageLocationService.monitorStorageLocations(false);
        Assert.assertTrue("1. Storage ocation should be referenced",
                          storageLocationService.search(storage).isPresent());

        Optional<StorageLocation> storageLocation = storageLocationService.search(storage);
        Assert.assertTrue("There should be file referenced for STAF storage", storageLocation.isPresent());
        Assert.assertEquals("Total size on STAF storage is invalid",
                            totalSize,
                            storageLocation.get().getTotalSizeOfReferencedFilesInKo());
        Assert.assertEquals("Total number of files on STAF storage is invalid",
                            4L,
                            storageLocation.get().getNumberOfReferencedFiles().longValue());
        Assert.assertEquals("Total number of pending files invalid",
                            1L,
                            storageLocation.get().getNumberOfPendingFiles().longValue());
        createFileReference(storage, 3 * 1024L);
        totalSize += 3;
        storageLocationService.monitorStorageLocations(false);
        Assert.assertTrue("There should be file referenced for STAF storage",
                          storageLocationService.search(storage).isPresent());
        Assert.assertEquals("Total size on STAF storage is invalid",
                            totalSize,
                            storageLocationService.search(storage).get().getTotalSizeOfReferencedFilesInKo());
        Assert.assertEquals("Total number of files on STAF storage invalid",
                            5L,
                            storageLocationService.search(storage).get().getNumberOfReferencedFiles().longValue());
    }

    @Test
    public void retrieveOne() throws ModuleException {
        String storage = "STAF";
        createFileReference(storage, 2048L);
        createFileReference(storage, 2048L);
        storageLocationService.monitorStorageLocations(false);
        StorageLocationDto loc = storageLocationService.getByName(storage);
        Assert.assertNotNull("A location should be retrieved", loc);
        Assert.assertNull("No configuration should be set for the location", loc.getConfiguration());
        Assert.assertEquals("There should be 2 files referenced", 2L, loc.getNbFilesStored());
        Assert.assertEquals("The total size should be 4ko", 4L, loc.getTotalStoredFilesSizeKo());
        Assert.assertEquals("There should be no storage error", 0L, loc.getNbStorageError());
        Assert.assertEquals("There should be no deletion error", 0L, loc.getNbDeletionError());
    }

    @Test
    public void retrieveAll() throws ModuleException {
        String storage = "STAF";
        createFileReference(storage, 2 * 1024L);
        createFileReference(storage, 2 * 1024L);
        String storage2 = "HPSS";
        createFileReference(storage2, 4 * 1024L);
        createFileReference(storage2, 4 * 1024L);
        createFileReference(storage2, 4 * 1024L);
        createFileReference(storage2, 4 * 1024L);
        createFileReference(storage2, 4 * 1024L);
        storageLocationService.monitorStorageLocations(false);
        Set<StorageLocationDto> locs = storageLocationService.getAllLocations();
        Assert.assertNotNull("Locations should be retrieved", locs);
        Assert.assertEquals("There should be 7 locations", 7, locs.size());
        Assert.assertEquals("Location one is missing",
                            1L,
                            locs.stream().filter(l -> l.getName().equals(storage)).count());
        locs.stream().filter(l -> l.getName().equals(storage)).forEach(loc -> {
            Assert.assertNotNull("Configuration should be set for the location", loc.getConfiguration());
            Assert.assertEquals("Location should be offline",
                                loc.getConfiguration().getStorageType(),
                                StorageType.OFFLINE);
            Assert.assertEquals("There should be 2 files referenced", 2L, loc.getNbFilesStored());
            Assert.assertEquals("The total size should be 4ko", 4L, loc.getTotalStoredFilesSizeKo());
            Assert.assertEquals("There should be no storage error", 0L, loc.getNbStorageError());
            Assert.assertEquals("There should be no deletion error", 0L, loc.getNbDeletionError());
            Assert.assertFalse("There should not be a pending action running", loc.isPendingActionRunning());
        });
        Assert.assertEquals("Location two is missing",
                            1L,
                            locs.stream().filter(l -> l.getName().equals(storage2)).count());
        locs.stream().filter(l -> l.getName().equals(storage2)).forEach(loc -> {
            Assert.assertNotNull("Configuration should be set for the location", loc.getConfiguration());
            Assert.assertEquals("Location should be offline",
                                loc.getConfiguration().getStorageType(),
                                StorageType.OFFLINE);
            Assert.assertEquals("There should be 5 files referenced", 5L, loc.getNbFilesStored());
            Assert.assertEquals("The total size should be 20ko", 20L, loc.getTotalStoredFilesSizeKo());
            Assert.assertEquals("There should be no storage error", 0L, loc.getNbStorageError());
            Assert.assertEquals("There should be no deletion error", 0L, loc.getNbDeletionError());
            Assert.assertFalse("There should be no pending action running", loc.isPendingActionRunning());
        });
        Assert.assertEquals("Location three is missing",
                            1L,
                            locs.stream().filter(l -> l.getName().equals(ONLINE_CONF_LABEL)).count());
        locs.stream().filter(l -> l.getName().equals(ONLINE_CONF_LABEL)).forEach(loc -> {
            Assert.assertNotNull("A configuration should be set for the location", loc.getConfiguration());
            Assert.assertEquals("Location should be offline",
                                loc.getConfiguration().getStorageType(),
                                StorageType.ONLINE);
            Assert.assertEquals("There should be 0 files referenced", 0L, loc.getNbFilesStored());
            Assert.assertEquals("The total size should be 20ko", 0L, loc.getTotalStoredFilesSizeKo());
            Assert.assertEquals("There should be no storage error", 0L, loc.getNbStorageError());
            Assert.assertEquals("There should be no deletion error", 0L, loc.getNbDeletionError());
        });
        Assert.assertEquals("Location four is missing",
                            1L,
                            locs.stream().filter(l -> l.getName().equals(NEARLINE_CONF_LABEL)).count());
        locs.stream().filter(l -> l.getName().equals(NEARLINE_CONF_LABEL)).forEach(loc -> {
            Assert.assertNotNull("A configuration should be set for the location", loc.getConfiguration());
            Assert.assertEquals("Location should be offline",
                                loc.getConfiguration().getStorageType(),
                                StorageType.NEARLINE);
            Assert.assertEquals("There should be 0 files referenced", 0L, loc.getNbFilesStored());
            Assert.assertEquals("The total size should be 20ko", 0L, loc.getTotalStoredFilesSizeKo());
            Assert.assertEquals("There should be no storage error", 0L, loc.getNbStorageError());
            Assert.assertEquals("There should be no deletion error", 0L, loc.getNbDeletionError());
        });
    }

    @Test
    public void delete_with_files() throws ModuleException {
        String storage = "STAF";
        createFileReference(storage, 2L);
        createFileReference(storage, 2L);
        storageLocationService.monitorStorageLocations(false);
        Assert.assertNotNull("Location should exists", storageLocationService.getByName(storage));
        storageLocationService.delete(storage);
        try {
            Assert.assertNull("Location should exists", storageLocationService.getByName(storage));
            Assert.fail("Location should not exists anymore");
        } catch (EntityNotFoundException e) {
            // Nothing to do
        }
        // As files as referenced after monitoring location should be recreated
        storageLocationService.monitorStorageLocations(false);
        Assert.assertNotNull("Location should exists", storageLocationService.getByName(storage));
    }

    @Test
    public void delete_without_files() throws ModuleException {
        storageLocationService.monitorStorageLocations(false);
        Assert.assertNotNull("Location should exists", storageLocationService.getByName(ONLINE_CONF_LABEL));
        storageLocationService.delete(ONLINE_CONF_LABEL);
        try {
            Assert.assertNull("Location should exists", storageLocationService.getByName(ONLINE_CONF_LABEL));
            Assert.fail("Location should not exists anymore");
        } catch (EntityNotFoundException e) {
            // Nothing to do
        }
        // As no files as referenced after monitoring location should not be recreated
        storageLocationService.monitorStorageLocations(false);
        try {
            Assert.assertNull("Location should exists", storageLocationService.getByName(ONLINE_CONF_LABEL));
            Assert.fail("Location should not exists anymore");
        } catch (EntityNotFoundException e) {
            // Nothing to do
        }
    }

    @Test
    @Transactional
    @Purpose("Test if ERROR requests are to be processed after a retry. Check associated events sent.")
    public void retryBySessionTest() {
        String sessionOwner1 = "SOURCE 1";
        String session1 = "SESSION 1";
        int nbDeletionReq = 12;
        int nbStorageReq = 10;
        // --- INIT ---
        // create deletion requests
        createFileDeletionRequests(nbDeletionReq, FileRequestStatus.ERROR, sessionOwner1, session1);
        // create storage requests
        createFileStorageRequests(nbStorageReq, FileRequestStatus.ERROR, sessionOwner1, session1);

        // --- LAUNCH RETRY ---
        storageLocationService.retryErrorsBySourceAndSession(sessionOwner1, session1);

        // --- CHECK RESULTS ---
        // assert all requests states have changed
        // deletion requests
        List<FileDeletionRequest> updatedDeletionRequests = this.fileDeletionRequestRepo.findAll();
        updatedDeletionRequests.forEach(req -> Assert.assertEquals("Request state should have been updated for retry",
                                                                   FileRequestStatus.TO_DO,
                                                                   req.getStatus()));
        // storage requests
        List<FileStorageRequestAggregation> updatedStorageRequests = this.fileStorageRequestRepo.findAll();
        updatedStorageRequests.forEach(req -> Assert.assertEquals("Request state should have been updated for retry",
                                                                  FileRequestStatus.TO_DO,
                                                                  req.getStatus()));

        // Simulate events and check if they are correctly sent
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 4, stepEventList.size());
        checkStepEvent(stepEventList.get(0),
                       SessionNotifierPropertyEnum.REQUESTS_ERRORS,
                       StepPropertyEventTypeEnum.DEC,
                       sessionOwner1,
                       session1,
                       String.valueOf(nbDeletionReq));
        checkStepEvent(stepEventList.get(1),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       sessionOwner1,
                       session1,
                       String.valueOf(nbDeletionReq));
        checkStepEvent(stepEventList.get(2),
                       SessionNotifierPropertyEnum.REQUESTS_ERRORS,
                       StepPropertyEventTypeEnum.DEC,
                       sessionOwner1,
                       session1,
                       String.valueOf(nbStorageReq));
        checkStepEvent(stepEventList.get(3),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       sessionOwner1,
                       session1,
                       String.valueOf(nbStorageReq));
    }

    @Test
    @Transactional
    @Purpose("Test if requests not in ERROR state are unchanged after a retry.")
    public void retryBySessionTestNoChange() {
        String sessionOwner1 = "SOURCE 1";
        String session1 = "SESSION 1";
        int nbDeletionReq = 12;
        int nbStorageReq = 10;
        // --- INIT ---
        // create deletion requests
        createFileDeletionRequests(nbDeletionReq, FileRequestStatus.DELAYED, sessionOwner1, session1);
        // create storage requests
        createFileStorageRequests(nbStorageReq, FileRequestStatus.PENDING, sessionOwner1, session1);

        // --- LAUNCH RETRY ---
        storageLocationService.retryErrorsBySourceAndSession(sessionOwner1, session1);

        // --- CHECK RESULTS ---
        // assert all requests states are not changed
        // deletion requests
        List<FileDeletionRequest> updatedDeletionRequests = this.fileDeletionRequestRepo.findAll();
        updatedDeletionRequests.forEach(req -> Assert.assertEquals("Request state should be in the same state",
                                                                   FileRequestStatus.DELAYED,
                                                                   req.getStatus()));
        // storage requests
        List<FileStorageRequestAggregation> updatedStorageRequests = this.fileStorageRequestRepo.findAll();
        updatedStorageRequests.forEach(req -> Assert.assertEquals("Request state should be in the same state",
                                                                  FileRequestStatus.PENDING,
                                                                  req.getStatus()));

        // Simulate events and check if they no StepPropertyUpdateRequestEvents were sent as requests are unchanged
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertTrue("Unexpected number of StepPropertyUpdateRequestEvents", stepEventList.isEmpty());
    }

    private List<FileDeletionRequest> createFileDeletionRequests(int nbRequests,
                                                                 FileRequestStatus requestStatus,
                                                                 String sessionOwner,
                                                                 String session) {
        List<FileDeletionRequest> createdDeletionRequests = new ArrayList<>();
        // init parameters
        // create requests
        for (int i = 0; i < nbRequests; i++) {
            // create file reference
            FileDeletionRequest request = new FileDeletionRequest(createFileReference("LOCAL", 1024L),
                                                                  true,
                                                                  UUID.randomUUID().toString(),
                                                                  requestStatus,
                                                                  sessionOwner,
                                                                  session);
            createdDeletionRequests.add(request);
        }
        return this.fileDeletionRequestRepo.saveAll(createdDeletionRequests);
    }

    /**
     * Method to create FileStorageRequests for test
     */
    private List<FileStorageRequestAggregation> createFileStorageRequests(int nbRequests,
                                                                          FileRequestStatus requestStatus,
                                                                          String sessionOwner,
                                                                          String session) {
        List<FileStorageRequestAggregation> createdStorageRequests = new ArrayList<>();
        // init parameters
        String owner = "test";
        String checksum = "2468";
        String algorithm = "dynamic";
        String fileName = "randomFile.test";
        Long fileSize = 20L;
        MimeType mimeType = MediaType.IMAGE_PNG;
        FileReferenceMetaInfo metaInfos = new FileReferenceMetaInfo(checksum, algorithm, fileName, fileSize, mimeType);
        String originUrl = "file://" + Paths.get("src/test/resources/input/cnes.png").toAbsolutePath();
        // create requests
        for (int i = 0; i < nbRequests; i++) {
            FileStorageRequestAggregation request = new FileStorageRequestAggregation(owner,
                                                                                      metaInfos,
                                                                                      originUrl,
                                                                                      "storage-" + i,
                                                                                      Optional.empty(),
                                                                                      UUID.randomUUID().toString(),
                                                                                      sessionOwner,
                                                                                      session);
            request.setStatus(requestStatus);
            createdStorageRequests.add(request);
        }
        return this.fileStorageRequestRepo.saveAll(createdStorageRequests);
    }
}