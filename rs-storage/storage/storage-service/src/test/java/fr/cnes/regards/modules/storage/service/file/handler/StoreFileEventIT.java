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
package fr.cnes.regards.modules.storage.service.file.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesRetryRequestEvent;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesStorageRequestEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEventType;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import fr.cnes.regards.modules.storage.service.file.request.RequestStatusService;
import fr.cnes.regards.modules.storage.service.session.SessionNotifierPropertyEnum;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.*;

/**
 * Test class
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests" },
                    locations = { "classpath:application-test.properties" })
public class StoreFileEventIT extends AbstractStorageIT {

    private static final String SESSION_OWNER = "SOURCE 1";

    private static final String SESSION = "SESSION 1";

    @Autowired
    private FilesStorageRequestEventHandler storeHandler;

    @Autowired
    private FilesRetryRequestEventHandler retryHandler;

    @Autowired
    private RequestStatusService requestStatusService;

    @Before
    public void initialize() throws ModuleException {
        Mockito.clearInvocations(publisher);
        super.init();
    }

    @Test(expected = IllegalArgumentException.class)
    @Requirement("REGARDS_DSL_STO_AIP_080")
    @Purpose("Check that a storage request without checksum is denied")
    public void store_file_no_checksum() {
        // Create a new bus message File reference request
        new FilesStorageRequestEvent(FileStorageRequestDto.build("file.name",
                                                                 null,
                                                                 "MD5",
                                                                 "application/octet-stream",
                                                                 "owner",
                                                                 SESSION_OWNER,
                                                                 SESSION,
                                                                 originUrl,
                                                                 ONLINE_CONF_LABEL,
                                                                 Optional.empty()), UUID.randomUUID().toString());
    }

    /**
     * Test request to reference a file already stored.
     * The file is not stored by the service as the origin storage and the destination storage are identical
     */
    @Test
    public void store_file_already_stored() {
        String owner = "new-owner";
        String checksum = UUID.randomUUID().toString();
        String storage = "storage";
        // Create a new bus message File reference request
        FilesStorageRequestEvent item = new FilesStorageRequestEvent(FileStorageRequestDto.build("file.name",
                                                                                                 checksum,
                                                                                                 "MD5",
                                                                                                 "application/octet-stream",
                                                                                                 owner,
                                                                                                 SESSION_OWNER,
                                                                                                 SESSION,
                                                                                                 originUrl,
                                                                                                 ONLINE_CONF_LABEL,
                                                                                                 Optional.empty()),
                                                                     UUID.randomUUID().toString());
        List<FilesStorageRequestEvent> items = new ArrayList<>();
        items.add(item);
        storeHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is not referenced yet
        Assert.assertFalse("File should not be referenced yet", fileRefService.search(storage, checksum).isPresent());
        // Check a file reference request is created
        Assert.assertEquals("File request should be created",
                            1,
                            stoReqService.search(ONLINE_CONF_LABEL, checksum).size());
        // Now check for event published
        Mockito.verify(this.publisher, Mockito.times(0)).publish(Mockito.any(FileReferenceEvent.class));

        // SImulate job schedule
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO,
                                                              Lists.newArrayList(ONLINE_CONF_LABEL),
                                                              Lists.newArrayList(owner));
        runAndWaitJob(jobs);
        Optional<FileReference> fileRef = fileRefService.search(ONLINE_CONF_LABEL, checksum);
        Assert.assertTrue("File should be referenced", fileRef.isPresent());
        Assert.assertFalse("File should in stored state", fileRef.get().isReferenced());
        Assert.assertFalse("File should in stored state", fileRef.get().getLocation().isPendingActionRemaining());
        Assert.assertTrue("File request should be deleted",
                          stoReqService.search(ONLINE_CONF_LABEL, checksum).isEmpty());
        // Now check for event published
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published",
                            FileReferenceEventType.STORED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());

        // Check step events were correctly send
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 4, stepEventList.size());
        checkStepEvent(stepEventList.get(0),
                       SessionNotifierPropertyEnum.STORE_REQUESTS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(1),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(2),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(3),
                       SessionNotifierPropertyEnum.STORED_FILES,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
    }

    @Test
    public void store_file_nearline_with_pending_actions() {
        String owner = "new-owner";
        String checksum = UUID.randomUUID().toString();
        String storage = "storage";
        // Create a new bus message File reference request
        FilesStorageRequestEvent item = new FilesStorageRequestEvent(FileStorageRequestDto.build("pending.file.name",
                                                                                                 checksum,
                                                                                                 "MD5",
                                                                                                 "application/octet-stream",
                                                                                                 owner,
                                                                                                 SESSION_OWNER,
                                                                                                 SESSION,
                                                                                                 originUrl,
                                                                                                 NEARLINE_CONF_LABEL,
                                                                                                 Optional.empty()),
                                                                     UUID.randomUUID().toString());
        List<FilesStorageRequestEvent> items = new ArrayList<>();
        items.add(item);
        storeHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO,
                                                              Lists.newArrayList(NEARLINE_CONF_LABEL),
                                                              Lists.newArrayList(owner));
        runAndWaitJob(jobs);
        Optional<FileReference> fileRef = fileRefService.search(NEARLINE_CONF_LABEL, checksum);
        Assert.assertTrue("File should be referenced", fileRef.isPresent());
        Assert.assertFalse("File should in stored state", fileRef.get().isReferenced());
        Assert.assertTrue("File should be referenced with pending action remaining",
                          fileRef.get().getLocation().isPendingActionRemaining());
    }

    @Test
    public void store_file_while_previous_request_exists() {
        String owner = "new-owner";
        String checksum = UUID.randomUUID().toString();
        String storage = "storage";
        // Create a new bus message File reference request
        String algorithm = "MD5";
        String fileName = "file.name";
        String mimeType = "application/octet-stream";
        String groupId = UUID.randomUUID().toString();

        FileRequestStatus oldRequestStatus = FileRequestStatus.TO_DO;
        FileStorageRequestAggregation request = stoReqService.createNewFileStorageRequest(Collections.singleton(owner),
                                                                                          new FileReferenceMetaInfo(
                                                                                              checksum,
                                                                                              algorithm,
                                                                                              fileName,
                                                                                              null,
                                                                                              MediaType.valueOf(mimeType)).withType(
                                                                                              DataType.RAWDATA.toString()),
                                                                                          originUrl,
                                                                                          ONLINE_CONF_LABEL,
                                                                                          Optional.empty(),
                                                                                          groupId,
                                                                                          Optional.of("File "
                                                                                                      + fileName
                                                                                                      + " (checksum: "
                                                                                                      + checksum
                                                                                                      + ") not handled by storage job. Storage job failed cause : For input string: \"Killed\""),
                                                                                          Optional.of(oldRequestStatus),
                                                                                          SESSION_OWNER,
                                                                                          SESSION);

        FilesStorageRequestEvent storageItem1 = new FilesStorageRequestEvent(FileStorageRequestDto.build(fileName,
                                                                                                         checksum,
                                                                                                         algorithm,
                                                                                                         mimeType,
                                                                                                         owner,
                                                                                                         SESSION_OWNER,
                                                                                                         SESSION,
                                                                                                         originUrl,
                                                                                                         ONLINE_CONF_LABEL,
                                                                                                         Optional.empty()),
                                                                             "group1");

        FilesStorageRequestEvent storageItem2 = new FilesStorageRequestEvent(FileStorageRequestDto.build(fileName,
                                                                                                         checksum,
                                                                                                         algorithm,
                                                                                                         mimeType,
                                                                                                         owner,
                                                                                                         SESSION_OWNER,
                                                                                                         SESSION,
                                                                                                         originUrl,
                                                                                                         ONLINE_CONF_LABEL,
                                                                                                         Optional.empty()),
                                                                             "group2");

        List<FilesStorageRequestEvent> items = new ArrayList<>();
        items.add(storageItem1);
        items.add(storageItem2);
        storeHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is not referenced yet
        Assert.assertFalse("File should not be referenced yet", fileRefService.search(storage, checksum).isPresent());
        // Check a file reference request is created
        Collection<FileStorageRequestAggregation> fileStorageRequests = stoReqService.search(ONLINE_CONF_LABEL,
                                                                                             checksum);
        Assert.assertEquals("New storage request in DELAYED status should have been created",
                            3,
                            fileStorageRequests.size());
        List<FileStorageRequestAggregation> newRequests = fileStorageRequests.stream()
                                                                             .filter(r -> !r.getId()
                                                                                            .equals(request.getId()))
                                                                             .toList();
        Assert.assertTrue("New request should be in state " + FileRequestStatus.DELAYED,
                          newRequests.stream().allMatch(r -> r.getStatus() == FileRequestStatus.DELAYED));
        // Now check for event published
        Mockito.verify(this.publisher, Mockito.times(0)).publish(Mockito.any(FileReferenceEvent.class));

        // Simulate job schedule -> Run first request
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO,
                                                              Lists.newArrayList(ONLINE_CONF_LABEL),
                                                              Lists.newArrayList(owner));
        runAndWaitJob(jobs);
        Optional<FileReference> fileRef = fileRefService.search(ONLINE_CONF_LABEL, checksum);
        Assert.assertTrue("File should be referenced", fileRef.isPresent());
        Assert.assertFalse("File should in stored state", fileRef.get().isReferenced());
        // Request should still be delayed
        fileStorageRequests = stoReqService.search(ONLINE_CONF_LABEL, checksum);
        Assert.assertEquals("There should be two delayed request remaining", 2L, fileStorageRequests.size());
        Assert.assertTrue("New request should be in state " + FileRequestStatus.DELAYED,
                          fileStorageRequests.stream().allMatch(r -> r.getStatus() == FileRequestStatus.DELAYED));

        // As no request is still running, the two requests should be merge in only one request and set in TO_DO status.
        // If many requests are  DELAYED, the checkDelayedStorageRequests merge them in only one request
        requestStatusService.checkDelayedStorageRequests();
        fileStorageRequests = stoReqService.search(ONLINE_CONF_LABEL, checksum);
        Assert.assertEquals("There should be only one request", 1L, fileStorageRequests.size());
        Assert.assertSame("Request should be in TODO status",
                          fileStorageRequests.stream().findFirst().get().getStatus(),
                          FileRequestStatus.TO_DO);
    }

    /**
     * Test request to reference a file already stored.
     * The file is not stored by the service as the origin storage and the destination storage are identical
     */
    @Test
    public void store_same_file() {
        String owner = "new-owner";
        String owner2 = owner + "23";
        String checksum = UUID.randomUUID().toString();
        String storage = "storage";
        // Create a new bus message File reference request
        FilesStorageRequestEvent item1 = new FilesStorageRequestEvent(FileStorageRequestDto.build("file.name",
                                                                                                  checksum,
                                                                                                  "MD5",
                                                                                                  "application/octet-stream",
                                                                                                  owner,
                                                                                                  SESSION_OWNER,
                                                                                                  SESSION,
                                                                                                  originUrl,
                                                                                                  ONLINE_CONF_LABEL,
                                                                                                  Optional.empty()),
                                                                      UUID.randomUUID().toString());
        FilesStorageRequestEvent item2 = new FilesStorageRequestEvent(FileStorageRequestDto.build("file.name",
                                                                                                  checksum,
                                                                                                  "MD5",
                                                                                                  "application/octet-stream",
                                                                                                  owner2,
                                                                                                  SESSION_OWNER,
                                                                                                  SESSION,
                                                                                                  originUrl,
                                                                                                  ONLINE_CONF_LABEL,
                                                                                                  Optional.empty()),
                                                                      UUID.randomUUID().toString());
        List<FilesStorageRequestEvent> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);
        storeHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is not referenced yet
        Assert.assertFalse("File should not be referenced yet", fileRefService.search(storage, checksum).isPresent());
        // Check a file reference request is created
        Collection<FileStorageRequestAggregation> requests = stoReqService.search(ONLINE_CONF_LABEL, checksum);
        Assert.assertEquals("there should be two store requests", 2, requests.size());
        Assert.assertTrue("there should be on request in TODO status",
                          requests.stream().anyMatch(r -> r.getStatus() == FileRequestStatus.TO_DO));
        Assert.assertTrue("there should be on request in DELAYED status",
                          requests.stream().anyMatch(r -> r.getStatus() == FileRequestStatus.DELAYED));
        // Now check for event published
        Mockito.verify(this.publisher, Mockito.times(0)).publish(Mockito.any(FileReferenceEvent.class));

        // Simulate job schedule for the first storage request
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO,
                                                              Lists.newArrayList(ONLINE_CONF_LABEL),
                                                              Lists.newArrayList(owner));
        runAndWaitJob(jobs);

        requests = stoReqService.search(ONLINE_CONF_LABEL, checksum);
        // The first request should be done
        Assert.assertEquals("there should be two store requests", 1, requests.size());
        Assert.assertTrue("there should be on request in DELAYED status",
                          requests.stream().anyMatch(r -> r.getStatus() == FileRequestStatus.DELAYED));

        /// simulate job for the the second storage request (that has been delayed)
        reqStatusService.checkDelayedStorageRequests();
        requests = stoReqService.search(ONLINE_CONF_LABEL, checksum);
        Assert.assertEquals("there should be two store requests", 1, requests.size());
        Assert.assertTrue("there should be on request in DELAYED status",
                          requests.stream().anyMatch(r -> r.getStatus() == FileRequestStatus.TO_DO));

        jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO,
                                          Lists.newArrayList(ONLINE_CONF_LABEL),
                                          Lists.newArrayList(owner2));
        runAndWaitJob(jobs);

        // Check results
        Assert.assertTrue("File should be referenced", fileRefService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        Assert.assertTrue("File request should be deleted",
                          stoReqService.search(ONLINE_CONF_LABEL, checksum).isEmpty());
        // Now check for event published
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(2)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published",
                            FileReferenceEventType.STORED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());

        // Check step events were correctly send
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 8, stepEventList.size());
        checkStepEvent(stepEventList.get(0),
                       SessionNotifierPropertyEnum.STORE_REQUESTS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(1),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(2),
                       SessionNotifierPropertyEnum.STORE_REQUESTS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(3),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(4),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(5),
                       SessionNotifierPropertyEnum.STORED_FILES,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(6),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(7),
                       SessionNotifierPropertyEnum.STORED_FILES,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
    }

    @Test
    public void store_files() {
        // Create a new bus message File reference request
        Set<FileStorageRequestDto> requests = Sets.newHashSet();
        String cs1 = UUID.randomUUID().toString();
        String cs2 = UUID.randomUUID().toString();
        requests.add(FileStorageRequestDto.build("file.name",
                                                 cs1,
                                                 "MD5",
                                                 "application/octet-stream",
                                                 "owner",
                                                 SESSION_OWNER,
                                                 SESSION,
                                                 originUrl,
                                                 ONLINE_CONF_LABEL,
                                                 Optional.empty()));
        requests.add(FileStorageRequestDto.build("file.name",
                                                 cs2,
                                                 "MD5",
                                                 "application/octet-stream",
                                                 "owner",
                                                 SESSION_OWNER,
                                                 SESSION,
                                                 originUrl,
                                                 ONLINE_CONF_LABEL,
                                                 Optional.empty()));
        FilesStorageRequestEvent item = new FilesStorageRequestEvent(requests, UUID.randomUUID().toString());

        List<FilesStorageRequestEvent> items = new ArrayList<>();
        items.add(item);
        storeHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Check file is not referenced yet
        Assert.assertFalse("File should not be referenced yet",
                           fileRefService.search(ONLINE_CONF_LABEL, cs1).isPresent());
        Assert.assertFalse("File should not be referenced yet",
                           fileRefService.search(ONLINE_CONF_LABEL, cs2).isPresent());
        // Check a file reference request is created
        Collection<FileStorageRequestAggregation> storageReqs = stoReqService.search(ONLINE_CONF_LABEL, cs1);
        Collection<FileStorageRequestAggregation> storageReqs2 = stoReqService.search(ONLINE_CONF_LABEL, cs2);
        Assert.assertEquals("File request should be created", 1, storageReqs.size());
        Assert.assertEquals("File request should be created", 1, storageReqs2.size());
        Assert.assertEquals("",
                            storageReqs.stream().findFirst().get().getGroupIds().stream().findFirst().get(),
                            storageReqs2.stream().findFirst().get().getGroupIds().stream().findFirst().get());

        // Now check for event published
        Mockito.verify(this.publisher, Mockito.times(0)).publish(Mockito.any(FileReferenceEvent.class));

        // Simulate job schedule
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO,
                                                              Lists.newArrayList(ONLINE_CONF_LABEL),
                                                              Lists.newArrayList());
        runAndWaitJob(jobs);
        Assert.assertTrue("File should be referenced", fileRefService.search(ONLINE_CONF_LABEL, cs1).isPresent());
        Assert.assertTrue("File should be referenced", fileRefService.search(ONLINE_CONF_LABEL, cs2).isPresent());
        Assert.assertTrue("File request should be deleted", stoReqService.search(ONLINE_CONF_LABEL, cs1).isEmpty());
        Assert.assertTrue("File request should be deleted", stoReqService.search(ONLINE_CONF_LABEL, cs2).isEmpty());
        // Now check for event published
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(2)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published",
                            FileReferenceEventType.STORED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());
    }

    /**
     * Test request to reference and store a file. An error should be thrown as the destination storage is unknown
     * The file is not stored by the service as the origin storage and the destination storage are identical
     */
    @Test
    public void store_file_unknown_storage() {
        String checksum = UUID.randomUUID().toString();
        String storageDestination = "somewheere";
        // Create a new bus message File reference request
        FilesStorageRequestEvent item = new FilesStorageRequestEvent(FileStorageRequestDto.build("file.name",
                                                                                                 checksum,
                                                                                                 "MD5",
                                                                                                 "application/octet-stream",
                                                                                                 "owner-test",
                                                                                                 SESSION_OWNER,
                                                                                                 SESSION,
                                                                                                 originUrl,
                                                                                                 storageDestination,
                                                                                                 Optional.empty()),
                                                                     UUID.randomUUID().toString());
        List<FilesStorageRequestEvent> items = new ArrayList<>();
        items.add(item);
        storeHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertFalse("File should not be referenced",
                           fileRefService.search(storageDestination, checksum).isPresent());
        // Now check for event published
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published",
                            FileReferenceEventType.STORE_ERROR,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());
        // Check step events were correctly send
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 4, stepEventList.size());
        checkStepEvent(stepEventList.get(0),
                       SessionNotifierPropertyEnum.STORE_REQUESTS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(1),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(2),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(3),
                       SessionNotifierPropertyEnum.REQUESTS_ERRORS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
    }

    /**
     * Test request to reference and store a file. An error should be thrown during storage by plugin
     */
    @Test
    public void store_file_error() {
        String checksum = UUID.randomUUID().toString();
        // Create a new bus message File reference request
        FilesStorageRequestEvent item = new FilesStorageRequestEvent(FileStorageRequestDto.build("error.file.name",
                                                                                                 checksum,
                                                                                                 "MD5",
                                                                                                 "application/octet-stream",
                                                                                                 "owner-test",
                                                                                                 SESSION_OWNER,
                                                                                                 SESSION,
                                                                                                 originUrl,
                                                                                                 ONLINE_CONF_LABEL,
                                                                                                 Optional.empty()),
                                                                     UUID.randomUUID().toString());
        List<FilesStorageRequestEvent> items = new ArrayList<>();
        items.add(item);
        storeHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertFalse("File should not be referenced",
                           fileRefService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        // Now check for event published
        Mockito.verify(publisher, Mockito.times(0)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.clearInvocations(publisher);

        // Simulate job schedule
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO,
                                                              Lists.newArrayList(ONLINE_CONF_LABEL),
                                                              Lists.newArrayList());
        runAndWaitJob(jobs);

        Assert.assertFalse("File should not be referenced",
                           fileRefService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        Assert.assertEquals("File request should be still present",
                            1,
                            stoReqService.search(ONLINE_CONF_LABEL, checksum).size());
        Assert.assertEquals("File request should be in ERROR state",
                            FileRequestStatus.ERROR,
                            stoReqService.search(ONLINE_CONF_LABEL, checksum).stream().findFirst().get().getStatus());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published",
                            FileReferenceEventType.STORE_ERROR,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());

        Assert.assertEquals("File request still present", 1, stoReqService.search(ONLINE_CONF_LABEL, checksum).size());
        Assert.assertEquals("File request in ERROR state",
                            FileRequestStatus.ERROR,
                            stoReqService.search(ONLINE_CONF_LABEL, checksum).stream().findFirst().get().getStatus());

        // Retry same storage request
        storeHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // There should be one storage request. Same as previous error one but updated to to_do thanks to new request
        Collection<FileStorageRequestAggregation> storeRequests = stoReqService.search(ONLINE_CONF_LABEL, checksum);
        Assert.assertEquals("File request still present", 1, storeRequests.size());
        // One in TO_DO state
        Assert.assertEquals("There should be one request in TO_DO state",
                            1L,
                            storeRequests.stream().filter(r -> r.getStatus() == FileRequestStatus.TO_DO).count());
    }

    @Test
    public void retry_byGroupId() {
        String storageDestination = "somewheere";
        String owner = "retry-test";
        Set<FileStorageRequestDto> files = Sets.newHashSet();

        // Create a new bus message File reference request
        files.add(FileStorageRequestDto.build("file1.test",
                                              UUID.randomUUID().toString(),
                                              "MD5",
                                              "application/octet-stream",
                                              owner,
                                              SESSION_OWNER,
                                              SESSION,
                                              originUrl,
                                              storageDestination,
                                              Optional.empty()));
        files.add(FileStorageRequestDto.build("file2.test",
                                              UUID.randomUUID().toString(),
                                              "MD5",
                                              "application/octet-stream",
                                              owner,
                                              SESSION_OWNER,
                                              SESSION,
                                              originUrl,
                                              storageDestination,
                                              Optional.empty()));
        files.add(FileStorageRequestDto.build("file3.test",
                                              UUID.randomUUID().toString(),
                                              "MD5",
                                              "application/octet-stream",
                                              owner,
                                              SESSION_OWNER,
                                              SESSION,
                                              originUrl,
                                              storageDestination,
                                              Optional.empty()));
        FilesStorageRequestEvent item = new FilesStorageRequestEvent(files, UUID.randomUUID().toString());
        List<FilesStorageRequestEvent> items = new ArrayList<>();
        items.add(item);
        storeHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check request in error
        Page<FileStorageRequestAggregation> requests = fileStorageRequestRepo.findByOwnersInAndStatus(Lists.newArrayList(
            owner), FileRequestStatus.ERROR, PageRequest.of(0, 1_000));
        Assert.assertEquals("The 3 requests should be in error", 3, requests.getTotalElements());

        FilesRetryRequestEvent retry = FilesRetryRequestEvent.buildStorageRetry(Lists.newArrayList(owner));
        TenantWrapper<FilesRetryRequestEvent> retryWrapper = TenantWrapper.build(retry, getDefaultTenant());
        retryHandler.handle(retryWrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Check request in {@link FileRequestStatus#TO_DO}
        requests = fileStorageRequestRepo.findByOwnersInAndStatus(Lists.newArrayList(owner),
                                                                  FileRequestStatus.TO_DO,
                                                                  PageRequest.of(0, 1_000));
        Assert.assertEquals("The 3 requests should be in TO_DO", 3, requests.getTotalElements());

        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO,
                                                              Lists.newArrayList(),
                                                              Lists.newArrayList());
        runAndWaitJob(jobs);

        requests = fileStorageRequestRepo.findByOwnersInAndStatus(Lists.newArrayList(owner),
                                                                  FileRequestStatus.ERROR,
                                                                  PageRequest.of(0, 1_000));
        Assert.assertEquals("The 3 requests should be in error again", 3, requests.getTotalElements());
    }

    @Test
    public void retry_byOwners() {
        String storageDestination = "somewheere";
        List<String> owners = Lists.newArrayList("retry-test", "retry-test-2", "retry-test-3");
        Set<FileStorageRequestDto> files = Sets.newHashSet();
        // Create a new bus message File reference request
        files.add(FileStorageRequestDto.build("file1.test",
                                              UUID.randomUUID().toString(),
                                              "MD5",
                                              "application/octet-stream",
                                              owners.get(0),
                                              SESSION_OWNER,
                                              SESSION,
                                              originUrl,
                                              storageDestination,
                                              Optional.empty()));
        files.add(FileStorageRequestDto.build("file2.test",
                                              UUID.randomUUID().toString(),
                                              "MD5",
                                              "application/octet-stream",
                                              owners.get(1),
                                              SESSION_OWNER,
                                              SESSION,
                                              originUrl,
                                              storageDestination,
                                              Optional.empty()));
        files.add(FileStorageRequestDto.build("file3.test",
                                              UUID.randomUUID().toString(),
                                              "MD5",
                                              "application/octet-stream",
                                              owners.get(2),
                                              SESSION_OWNER,
                                              SESSION,
                                              originUrl,
                                              storageDestination,
                                              Optional.empty()));
        FilesStorageRequestEvent item = new FilesStorageRequestEvent(files, UUID.randomUUID().toString());
        List<FilesStorageRequestEvent> items = new ArrayList<>();
        items.add(item);
        storeHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check request in error
        Page<FileStorageRequestAggregation> requests = fileStorageRequestRepo.findByOwnersInAndStatus(owners,
                                                                                                      FileRequestStatus.ERROR,
                                                                                                      PageRequest.of(0,
                                                                                                                     1_000));
        Assert.assertEquals("The 3 requests should be in error", 3, requests.getTotalElements());

        FilesRetryRequestEvent retry = FilesRetryRequestEvent.buildStorageRetry(owners);
        TenantWrapper<FilesRetryRequestEvent> retryWrapper = TenantWrapper.build(retry, getDefaultTenant());
        retryHandler.handle(retryWrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Check request in {@link FileRequestStatus#TO_DO}
        requests = fileStorageRequestRepo.findByOwnersInAndStatus(owners,
                                                                  FileRequestStatus.TO_DO,
                                                                  PageRequest.of(0, 1_000));
        Assert.assertEquals("The 3 requests should be in TO_DO", 3, requests.getTotalElements());

        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO,
                                                              Lists.newArrayList(),
                                                              Lists.newArrayList());
        runAndWaitJob(jobs);

        requests = fileStorageRequestRepo.findByOwnersInAndStatus(owners,
                                                                  FileRequestStatus.ERROR,
                                                                  PageRequest.of(0, 1_000));
        Assert.assertEquals("The 3 requests should be in error again", 3, requests.getTotalElements());

        // Check step events were correctly send (check only for the first request)
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 24, stepEventList.size());
        checkStepEvent(stepEventList.get(0),
                       SessionNotifierPropertyEnum.STORE_REQUESTS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(1),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(2),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(3),
                       SessionNotifierPropertyEnum.REQUESTS_ERRORS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(12),
                       SessionNotifierPropertyEnum.REQUESTS_ERRORS,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(13),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(18),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
        checkStepEvent(stepEventList.get(19),
                       SessionNotifierPropertyEnum.REQUESTS_ERRORS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION,
                       "1");
    }
}