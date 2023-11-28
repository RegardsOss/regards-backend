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
package fr.cnes.regards.modules.storage.service.file.flow;

import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEventType;
import fr.cnes.regards.modules.filecatalog.dto.request.FileGroupRequestStatus;
import fr.cnes.regards.modules.filecatalog.dto.request.FileReferenceRequestDto;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import fr.cnes.regards.modules.storage.service.session.SessionNotifierPropertyEnum;
import org.apache.commons.compress.utils.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Test class
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests" },
                    locations = { "classpath:application-test.properties" })
public class ReferenceFileFlowItemIT extends AbstractStorageIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceFileFlowItemIT.class);

    private static final String SESSION_OWNER_1 = "SOURCE 1";

    private static final String SESSION_OWNER_2 = "SOURCE 2";

    private static final String SESSION_1 = "SESSION 1";

    @Autowired
    private ReferenceFlowItemHandler handler;

    @Before
    public void initialize() throws ModuleException {
        Mockito.clearInvocations(publisher);
        super.init();
    }

    /**
     * Test request to reference a file already stored.
     * The file is not stored by the service as the origin storage and the destination storage are identical
     */
    @Test
    public void addFileRefFlowItem() throws InterruptedException {
        String checksum = UUID.randomUUID().toString();
        String storage = "storage";
        // Create a new bus message File reference request
        FilesReferenceEvent item = new FilesReferenceEvent(FileReferenceRequestDto.build("file.name",
                                                                                         checksum,
                                                                                         "MD5",
                                                                                         "application/octet-stream",
                                                                                         10L,
                                                                                         "owner-test",
                                                                                         storage,
                                                                                         "file://storage/location/file.name",
                                                                                         SESSION_OWNER_1,
                                                                                         SESSION_1),
                                                           UUID.randomUUID().toString());
        List<FilesReferenceEvent> items = new ArrayList<>();
        items.add(item);
        long start = System.currentTimeMillis();
        handler.handleBatch(items);
        long finish = System.currentTimeMillis();
        LOGGER.info("Add file reference duration {}ms", finish - start);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Optional<FileReference> fileRef = fileRefService.search(storage, checksum);
        Assert.assertTrue("File should be present", fileRef.isPresent());
        Assert.assertTrue("File should be referenced", fileRef.get().isReferenced());

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
                       SessionNotifierPropertyEnum.REFERENCE_REQUESTS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(1),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(2),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(3),
                       SessionNotifierPropertyEnum.REFERENCED_FILES,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
    }

    @Test
    public void addFileRefFlowItemsWithSameChecksum() throws InterruptedException {
        String checksum = UUID.randomUUID().toString();
        String owner = "new-owner";
        String storage = "somewhere";
        List<FilesReferenceEvent> items = Lists.newArrayList();

        // Create a request to reference a file with the same checksum as the one stored before but with a new owner
        FilesReferenceEvent item = new FilesReferenceEvent(FileReferenceRequestDto.build("file.name",
                                                                                         checksum,
                                                                                         "MD5",
                                                                                         "application/octet-stream",
                                                                                         10L,
                                                                                         owner,
                                                                                         storage,
                                                                                         "file://storage/location/file.name",
                                                                                         SESSION_OWNER_1,
                                                                                         SESSION_1),
                                                           UUID.randomUUID().toString());
        items.add(item);

        // Create a request to reference a file with the same checksum as the one stored before but with a new owner
        FilesReferenceEvent item2 = new FilesReferenceEvent(FileReferenceRequestDto.build("file.name.2",
                                                                                          checksum,
                                                                                          "MD5",
                                                                                          "application/octet-stream",
                                                                                          10L,
                                                                                          owner,
                                                                                          storage,
                                                                                          "file://storage/location/file.name",
                                                                                          SESSION_OWNER_1,
                                                                                          SESSION_1),
                                                            UUID.randomUUID().toString());
        items.add(item2);

        // Publish request
        handler.handleBatch(items);
        Thread.sleep(5_000L);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertTrue("File should be referenced", fileRefService.search(storage, checksum).isPresent());
    }

    @Test
    public void addFileRefFlowItemsWithoutChecksum() throws InterruptedException {

        String checksum = UUID.randomUUID().toString();
        String owner = "new-owner";
        String storage = "somewhere";
        List<FilesReferenceEvent> items = Lists.newArrayList();

        // Create a request to reference a file with the same checksum as the one stored before but with a new owner
        FileReferenceRequestDto req = FileReferenceRequestDto.build("file.name",
                                                                    checksum,
                                                                    "MD5",
                                                                    "application/octet-stream",
                                                                    10L,
                                                                    owner,
                                                                    storage,
                                                                    "file://storage/location/file.name",
                                                                    SESSION_OWNER_1,
                                                                    SESSION_1);
        req.setChecksum(null);
        FilesReferenceEvent item = new FilesReferenceEvent(req, UUID.randomUUID().toString());
        items.add(item);

        // Publish request
        handler.handleBatch(items);
        Thread.sleep(5_000L);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertFalse("File should be referenced", fileRefService.search(storage, checksum).isPresent());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published",
                            FileGroupRequestStatus.DENIED,
                            getFileRequestsGroupEvent(argumentCaptor.getAllValues()).getState());
        // Check step events were correctly send
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 2, stepEventList.size());
        checkStepEvent(stepEventList.get(0),
                       SessionNotifierPropertyEnum.REFERENCE_REQUESTS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(1),
                       SessionNotifierPropertyEnum.REQUESTS_REFUSED,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");

    }

    /**
     * Test request to reference a file already stored.
     * The file is not stored by the service as the origin storage and the destination storage are identical
     */
    @Test
    public void addFileRefFlowItemAlreadyExists() throws InterruptedException, ExecutionException {
        String checksum = UUID.randomUUID().toString();
        String owner = "new-owner";
        FileReference fileRef = this.generateStoredFileReference(checksum,
                                                                 owner,
                                                                 "file.test",
                                                                 ONLINE_CONF_LABEL,
                                                                 Optional.empty(),
                                                                 Optional.empty(),
                                                                 SESSION_OWNER_1,
                                                                 SESSION_1);
        String storage = fileRef.getLocation().getStorage();
        // One store event should be sent
        Mockito.verify(this.publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));

        // Create a request to reference a file with the same checksum as the one stored before but with a new owner
        FilesReferenceEvent item = new FilesReferenceEvent(FileReferenceRequestDto.build("file.name",
                                                                                         checksum,
                                                                                         "MD5",
                                                                                         "application/octet-stream",
                                                                                         10L,
                                                                                         "owner-test",
                                                                                         storage,
                                                                                         "file://storage/location/file.name",
                                                                                         SESSION_OWNER_2,
                                                                                         SESSION_1),
                                                           UUID.randomUUID().toString());
        List<FilesReferenceEvent> items = new ArrayList<>();
        items.add(item);
        handler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertTrue("File should be referenced", fileRefService.search(storage, checksum).isPresent());
        // Now check for event published. One for each referenced file
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
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(1),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(2),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(3),
                       SessionNotifierPropertyEnum.STORED_FILES,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(4),
                       SessionNotifierPropertyEnum.REFERENCE_REQUESTS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_2,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(5),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_2,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(6),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER_2,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(7),
                       SessionNotifierPropertyEnum.REFERENCED_FILES,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_2,
                       SESSION_1,
                       "1");
    }

    /**
     * Test request to reference a file already stored.
     * The file is not stored by the service as the origin storage and the destination storage are identical
     */
    @Test
    public void addFileRefFlowItemWithSameChecksum() throws InterruptedException, ExecutionException {
        String checksum = UUID.randomUUID().toString();
        String owner = "new-owner";
        String storage = "aStorage";
        this.generateStoredFileReference(checksum,
                                         owner,
                                         "file.test",
                                         ONLINE_CONF_LABEL,
                                         Optional.empty(),
                                         Optional.empty(),
                                         SESSION_OWNER_1,
                                         SESSION_1);
        // Create a new bus message File reference request
        FilesReferenceEvent item = new FilesReferenceEvent(FileReferenceRequestDto.build("file.name",
                                                                                         checksum,
                                                                                         "MD5",
                                                                                         "application/octet-stream",
                                                                                         10L,
                                                                                         "owner-test",
                                                                                         storage,
                                                                                         "file://storage/location/file.name",
                                                                                         SESSION_OWNER_2,
                                                                                         SESSION_1),
                                                           UUID.randomUUID().toString());
        List<FilesReferenceEvent> items = new ArrayList<>();
        items.add(item);
        handler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertTrue("File should be referenced", fileRefService.search(storage, checksum).isPresent());
        // Now check for event published. One for each referenced file
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(2)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published",
                            FileReferenceEventType.STORED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());
    }
}