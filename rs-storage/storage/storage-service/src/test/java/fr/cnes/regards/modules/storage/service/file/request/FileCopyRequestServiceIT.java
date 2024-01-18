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
package fr.cnes.regards.modules.storage.service.file.request;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.fileaccess.dto.request.FileCopyDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileGroupRequestStatus;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesCopyEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileRequestsGroupEvent;
import fr.cnes.regards.modules.storage.domain.database.CacheFile;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import fr.cnes.regards.modules.storage.service.session.SessionNotifierPropertyEnum;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Test class
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler", "nojobs" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_copy_tests" },
                    locations = { "classpath:application-test.properties" })
public class FileCopyRequestServiceIT extends AbstractStorageIT {

    private static final String SESSION_OWNER = "SOURCE 1";

    private static final String SESSION = "SESSION 1";

    @Autowired
    private RequestsGroupService reqGrpService;

    @Autowired
    private IDynamicTenantSettingService dynamicTenantSettingService;

    @Before
    @Override
    public void init() throws ModuleException {
        Mockito.clearInvocations(publisher);
        super.init();
        simulateApplicationStartedEvent();
        simulateApplicationReadyEvent();
    }

    @Test
    public void copyPath() throws InterruptedException, ExecutionException {
        String owner = "first-owner";
        Long nbFiles = 20L;
        for (int i = 0; i < nbFiles; i++) {
            generateStoredFileReference(UUID.randomUUID().toString(),
                                        owner,
                                        String.format("file-%d.test", i),
                                        ONLINE_CONF_LABEL,
                                        Optional.of("/rep/one"),
                                        Optional.of("plop"),
                                        SESSION_OWNER,
                                        SESSION);
        }
        for (int i = 0; i < 5; i++) {
            generateStoredFileReference(UUID.randomUUID().toString(),
                                        owner,
                                        String.format("file-%d.test", i),
                                        ONLINE_CONF_LABEL,
                                        Optional.of("/rep/two"),
                                        Optional.of("plop"),
                                        SESSION_OWNER,
                                        SESSION);
        }
        JobInfo ji = fileCopyRequestService.scheduleJob(ONLINE_CONF_LABEL,
                                                        "rep/one",
                                                        NEARLINE_CONF_LABEL,
                                                        Optional.empty(),
                                                        Sets.newHashSet("plop"),
                                                        SESSION_OWNER,
                                                        SESSION);
        Assert.assertNotNull("A job should be created", ji);
        Mockito.reset(publisher);
        jobService.runJob(ji, getDefaultTenant()).get();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check event is well publish for copying the files
        ArgumentCaptor<FilesCopyEvent> argumentCaptor = ArgumentCaptor.forClass(FilesCopyEvent.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FilesCopyEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        FilesCopyEvent copyItem = null;
        for (Object item : argumentCaptor.getAllValues()) {
            if (item instanceof FilesCopyEvent) {
                copyItem = (FilesCopyEvent) item;
                break;
            }
        }
        Assert.assertNotNull(copyItem);
        // 3 of the 6 files must be copied (only files in target/files)
        Assert.assertEquals(20, copyItem.getFiles().size());
    }

    @Test
    public void copyFile() throws InterruptedException, ExecutionException {
        String requestGroup = UUID.randomUUID().toString();
        FileReference fileRef = this.generateRandomStoredNearlineFileReference("file1.test", Optional.empty());
        Set<FileCopyDto> requests = Sets.newHashSet(FileCopyDto.build(fileRef.getMetaInfo().getChecksum(),
                                                                      ONLINE_CONF_LABEL,
                                                                      SESSION_OWNER,
                                                                      SESSION));
        fileCopyRequestService.copy(Sets.newHashSet(new FilesCopyEvent(requests, requestGroup)));
        // A new copy request should be created
        Optional<FileCopyRequest> oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(),
                                                                       ONLINE_CONF_LABEL);
        Assert.assertTrue("There should be a copy request created", oReq.isPresent());

        // Now run copy schedule
        fileCopyRequestService.scheduleCopyRequests(FileRequestStatus.TO_DO);

        // There should be one availability request created
        Optional<FileCacheRequest> oCacheReq = fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum());
        oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(), ONLINE_CONF_LABEL);
        Assert.assertTrue("There should be a cache request created", oCacheReq.isPresent());
        Assert.assertTrue("No storage request should be created yet", fileStorageRequestRepo.count() == 0);
        Assert.assertTrue("There should be a copy request", oReq.isPresent());
        Assert.assertTrue("There should be a copy request in pending state",
                          oReq.get().getStatus() == FileRequestStatus.PENDING);

        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);

        // Cache file should be restored
        oCacheReq = fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum());
        Optional<CacheFile> oCachedFile = cacheService.findByChecksum(fileRef.getMetaInfo().getChecksum());
        oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(), ONLINE_CONF_LABEL);
        Assert.assertFalse("There should not be a cache request anymore", oCacheReq.isPresent());
        Assert.assertTrue("The file should be restored in  cache", oCachedFile.isPresent());
        Assert.assertTrue("There should be a copy request", oReq.isPresent());
        Assert.assertTrue("There should be a copy request in pending state",
                          oReq.get().getStatus() == FileRequestStatus.PENDING);

        // Simulate send of the event
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        FileReferenceEvent event = getFileReferenceEvent(argumentCaptor.getAllValues());

        argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        fileRefEventHandler.handleBatch(Lists.newArrayList(event));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // A new storage request should be created
        Collection<FileStorageRequestAggregation> storageReqs = stoReqService.search(ONLINE_CONF_LABEL,
                                                                                     fileRef.getMetaInfo()
                                                                                            .getChecksum());
        oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(), ONLINE_CONF_LABEL);
        Assert.assertEquals("There should be a storage request created", 1, storageReqs.size());
        Assert.assertTrue("There should be a copy request", oReq.isPresent());
        Assert.assertTrue("There should be a copy request in pending state",
                          oReq.get().getStatus() == FileRequestStatus.PENDING);

        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());

        // Run storage job
        Mockito.reset(publisher);
        jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO, Lists.newArrayList(), Lists.newArrayList());
        runAndWaitJob(jobs);
        storageReqs = stoReqService.search(ONLINE_CONF_LABEL, fileRef.getMetaInfo().getChecksum());
        Optional<FileReference> oFileRef = fileRefService.search(ONLINE_CONF_LABEL,
                                                                 fileRef.getMetaInfo().getChecksum());
        Assert.assertTrue("File reference should have been created.", oFileRef.isPresent());
        Assert.assertTrue("File reference request should not exists anymore", storageReqs.isEmpty());
        Assert.assertTrue("There should be a copy request", oReq.isPresent());
        Assert.assertTrue("There should be a copy request in pending state",
                          oReq.get().getStatus() == FileRequestStatus.PENDING);

        // get events published
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());

        // check copy request is correctly notified
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 10, stepEventList.size());
        checkStepEvent(stepEventList.get(4),
                       SessionNotifierPropertyEnum.COPY_REQUESTS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(5),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(6),
                       SessionNotifierPropertyEnum.STORE_REQUESTS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(7),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(8),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(9),
                       SessionNotifierPropertyEnum.STORED_FILES,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        Mockito.reset(publisher);

        // Simulate file stored event
        event = getFileReferenceEvent(argumentCaptor.getAllValues());
        argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        fileRefEventHandler.handleBatch(Lists.newArrayList(event));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(), ONLINE_CONF_LABEL);
        Assert.assertFalse("There should not be a copy request anymore", oReq.isPresent());

        // File should not be in cache anymore
        oCachedFile = cacheService.findByChecksum(fileRef.getMetaInfo().getChecksum());
        Assert.assertFalse("The cache file should be deleted after copy", oCachedFile.isPresent());

        // check step events are correctly notified
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 1, stepEventList.size());
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        checkStepEvent(stepEventList.get(0),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER,
                       SESSION,
                       "1");

        // Check request group is done
        Mockito.reset(publisher);
        reqGrpService.checkRequestsGroupsDone();
        argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        FileRequestsGroupEvent frge = null;
        for (ISubscribable e : argumentCaptor.getAllValues()) {
            if (e instanceof FileRequestsGroupEvent) {
                frge = (FileRequestsGroupEvent) e;
                if ((frge.getType() == FileRequestType.COPY) && frge.getGroupId().equals(requestGroup)) {
                    break;
                } else {
                    frge = null;
                }
            }
        }
        Assert.assertNotNull(frge);
        Assert.assertEquals(FileGroupRequestStatus.SUCCESS, frge.getState());
        Assert.assertEquals(1, frge.getSuccess().size());
        Assert.assertEquals(0, frge.getErrors().size());
    }

    @Test
    public void copyFileInSubDir() throws InterruptedException, ExecutionException {
        String copyDestinationPath = "dir/test/copy";
        FileReference fileRef = this.generateRandomStoredNearlineFileReference("file1.test", Optional.empty());
        Set<FileCopyDto> requests = Sets.newHashSet(FileCopyDto.build(fileRef.getMetaInfo().getChecksum(),
                                                                      ONLINE_CONF_LABEL,
                                                                      copyDestinationPath,
                                                                      SESSION_OWNER,
                                                                      SESSION));
        fileCopyRequestService.handle(requests, UUID.randomUUID().toString());
        // A new copy request should be created
        Optional<FileCopyRequest> oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(),
                                                                       ONLINE_CONF_LABEL);
        Assert.assertTrue("There should be a copy request created", oReq.isPresent());

        // Now run copy schedule
        fileCopyRequestService.scheduleCopyRequests(FileRequestStatus.TO_DO);

        // There should be one availability request created
        Optional<FileCacheRequest> oCacheReq = fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum());
        oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(), ONLINE_CONF_LABEL);
        Assert.assertTrue("There should be a cache request created", oCacheReq.isPresent());
        Assert.assertTrue("No storage request should be created yet", fileStorageRequestRepo.count() == 0);
        Assert.assertTrue("There should be a copy request", oReq.isPresent());
        Assert.assertTrue("There should be a copy request in pending state",
                          oReq.get().getStatus() == FileRequestStatus.PENDING);

        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);

        // Cache file should be restored
        oCacheReq = fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum());
        Optional<CacheFile> oCachedFile = cacheService.findByChecksum(fileRef.getMetaInfo().getChecksum());
        oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(), ONLINE_CONF_LABEL);
        Assert.assertFalse("There should not be a cache request anymore", oCacheReq.isPresent());
        Assert.assertTrue("The file should be restored in  cache", oCachedFile.isPresent());
        Assert.assertTrue("There should be a copy request", oReq.isPresent());
        Assert.assertTrue("There should be a copy request in pending state",
                          oReq.get().getStatus() == FileRequestStatus.PENDING);

        // Simulate send of the event
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        FileReferenceEvent event = getFileReferenceEvent(argumentCaptor.getAllValues());

        fileRefEventHandler.handleBatch(Lists.newArrayList(event));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // A new storage request should be created
        Collection<FileStorageRequestAggregation> storageReqs = stoReqService.search(ONLINE_CONF_LABEL,
                                                                                     fileRef.getMetaInfo()
                                                                                            .getChecksum());
        oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(), ONLINE_CONF_LABEL);
        Assert.assertEquals("There should be a storage request created", 1, storageReqs.size());
        Assert.assertEquals("The storage request should contains subdirectory to store to",
                            copyDestinationPath,
                            storageReqs.stream().findFirst().get().getStorageSubDirectory());
        Assert.assertTrue("There should be a copy request", oReq.isPresent());
        Assert.assertTrue("There should be a copy request in pending state",
                          oReq.get().getStatus() == FileRequestStatus.PENDING);

        // Run storage job
        Mockito.reset(publisher);
        jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO, Lists.newArrayList(), Lists.newArrayList());
        runAndWaitJob(jobs);
        storageReqs = stoReqService.search(ONLINE_CONF_LABEL, fileRef.getMetaInfo().getChecksum());
        Optional<FileReference> oFileRef = fileRefService.search(ONLINE_CONF_LABEL,
                                                                 fileRef.getMetaInfo().getChecksum());
        Assert.assertTrue("File reference should have been created.", oFileRef.isPresent());
        Assert.assertTrue("File reference request should not exists anymore", storageReqs.isEmpty());
        Assert.assertTrue("There should be a copy request", oReq.isPresent());
        Assert.assertTrue("There should be a copy request in pending state",
                          oReq.get().getStatus() == FileRequestStatus.PENDING);

        // Simulate file  stored event
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        event = getFileReferenceEvent(argumentCaptor.getAllValues());
        fileRefEventHandler.handleBatch(Lists.newArrayList(event));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(), ONLINE_CONF_LABEL);
        Assert.assertFalse("There should not be a copy request anymore", oReq.isPresent());

        // File should not be in cache anymore
        oCachedFile = cacheService.findByChecksum(fileRef.getMetaInfo().getChecksum());
        Assert.assertFalse("The cache file should be deleted after copy", oCachedFile.isPresent());
    }

    @Test
    public void copyFile_error_offlineFile() {
        String storage = "somewhere";
        String storageCopyDest = "somewhereElse";
        FileReference fileRef = referenceRandomFile("owner",
                                                    "type",
                                                    "file1.test",
                                                    storage,
                                                    SESSION_OWNER,
                                                    SESSION,
                                                    false).get();
        Set<FileCopyDto> requests = Sets.newHashSet(FileCopyDto.build(fileRef.getMetaInfo().getChecksum(),
                                                                      storageCopyDest,
                                                                      SESSION_OWNER,
                                                                      SESSION));
        fileCopyRequestService.handle(requests, UUID.randomUUID().toString());
        Optional<FileCopyRequest> oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(),
                                                                       storageCopyDest);
        Assert.assertTrue("There should be a copy request created", oReq.isPresent());

        // Now run copy schedule
        fileCopyRequestService.scheduleCopyRequests(FileRequestStatus.TO_DO);

        // There should be one availability request created
        Optional<FileCacheRequest> oCacheReq = fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum());
        oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(), storageCopyDest);
        Assert.assertTrue("There should be a cache request created", oCacheReq.isPresent());
        Assert.assertTrue("No storage request should be created yet", fileStorageRequestRepo.count() == 0);
        Assert.assertTrue("There should be a copy request", oReq.isPresent());
        Assert.assertTrue("There should be a copy request in pending state",
                          oReq.get().getStatus() == FileRequestStatus.PENDING);

        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);

        oCacheReq = fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum());
        Assert.assertTrue("There should be a cache request in error state",
                          oCacheReq.get().getStatus() == FileRequestStatus.ERROR);

        // Simulate send of the event
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        FileReferenceEvent event = getFileReferenceEvent(argumentCaptor.getAllValues());
        fileRefEventHandler.handleBatch(Lists.newArrayList(event));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Copy request should be updated in ERROR
        oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(), storageCopyDest);
        Assert.assertTrue("There should be a copy request in error state",
                          oReq.get().getStatus() == FileRequestStatus.ERROR);
    }

    @Test
    public void copyFile_error_unknownFile() {
        String storage = "somewhere";
        String unknownChecksum = UUID.randomUUID().toString();
        Set<FileCopyDto> requests = Sets.newHashSet(FileCopyDto.build(unknownChecksum,
                                                                      storage,
                                                                      SESSION_OWNER,
                                                                      SESSION));
        fileCopyRequestService.handle(requests, UUID.randomUUID().toString());
        Optional<FileCopyRequest> oReq = fileCopyRequestService.search(unknownChecksum, storage);
        Assert.assertFalse("There should not be a copy request created", oReq.isPresent());
    }

}
