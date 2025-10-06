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
package fr.cnes.regards.modules.storage.service.file.handler;

import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.modules.fileaccess.dto.request.FileGroupRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.request.FileReferenceRequestDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEventType;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceRequestArgs;
import fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceRequestPublisher;
import fr.cnes.regards.modules.storage.service.file.request.AbstractFileReferenceRequestServiceIT;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum.DEC;
import static fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum.INC;
import static fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceConstants.*;
import static fr.cnes.regards.modules.storage.service.session.SessionNotifierPropertyEnum.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests" },
                    locations = { "classpath:application-test.properties" })
public class ReferenceEventHandlerIT extends AbstractFileReferenceRequestServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceEventHandlerIT.class);

    @Autowired
    private FileReferenceRequestPublisher referenceRequestPublisher;

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
    public void add_file_reference() {
        // GIVEN
        final FileReferenceRequestArgs args = newReferenceRequestArgs();
        // Create a new bus message File reference request
        final FilesReferenceEvent event = newFilesReferenceEvent(args);

        // WHEN
        // publish and run the request
        referenceRequestPublisher.publishReferenceEvents(1, event);

        // THEN

        // Check file is well referenced
        final FileReference fileRef = referenceService.search(args.getStorage(), args.getChecksum()).orElse(null);
        assertThat(fileRef).as("File should be present").isNotNull();
        assertThat(fileRef.isReferenced()).as("File should be referenced").isTrue();

        // check for event published
        final ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        final FileReferenceEventType type = getFileReferenceEvent(argumentCaptor.getAllValues()).getType();
        assertThat(type).as("File reference event STORED should be published").isEqualTo(FileReferenceEventType.STORED);

        // Check step events were correctly send
        final List<StepPropertyUpdateRequestEvent> stepEvents = getStepPropertyEvents(argumentCaptor.getAllValues());
        assertThat(stepEvents).as("Unexpected number of StepPropertyUpdateRequestEvents").hasSize(5);
        checkStepEvent(stepEvents.get(0), REQUESTS_RUNNING, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEvents.get(1), REFERENCE_REQUESTS, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEvents.get(2), REQUESTS_RUNNING, DEC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEvents.get(3), REFERENCE_REQUESTS, DEC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEvents.get(4), REFERENCED_FILES, INC, SESSION1_OWNER, SESSION1, "1");
    }

    @Test
    public void add_file_reference_same_checksum() {
        // GIVEN
        final FileReferenceRequestArgs args1 = newReferenceRequestArgs();
        final FileReferenceRequestArgs args2 = args1.withFileName("file.name.2");

        // Create a request to reference a file with the same checksum
        final String groupId = UUID.randomUUID().toString();
        final FilesReferenceEvent event1 = newFilesReferenceEvent(groupId, args1);
        final FilesReferenceEvent event2 = newFilesReferenceEvent(groupId, args2);

        // WHEN
        referenceRequestPublisher.publishReferenceEvents(1, event1, event2);

        // THEN

        // Check file is well referenced
        final FileReference fileRef = referenceService.search(args1.getStorage(), args1.getChecksum()).orElse(null);
        assertThat(fileRef).as("File should be present").isNotNull();
        assertThat(fileRef.isReferenced()).as("File should be referenced").isTrue();

        // check for event published
        final ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(2)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());

        // reference event STORED
        final FileReferenceEventType type = getFileReferenceEvent(argumentCaptor.getAllValues()).getType();
        assertThat(type).as("File reference event STORED should be published").isEqualTo(FileReferenceEventType.STORED);

        // group GRANTED
        final FileGroupRequestStatus state = getFileRequestsGroupEvent(argumentCaptor.getAllValues()).getState();
        assertThat(state).isEqualTo(FileGroupRequestStatus.GRANTED);

        // Check step events were correctly send
        final List<StepPropertyUpdateRequestEvent> stepEvents = getStepPropertyEvents(argumentCaptor.getAllValues());
        assertThat(stepEvents).as("Unexpected number of StepPropertyUpdateRequestEvents").hasSize(9);
        // 2 request to be process
        checkStepEvent(stepEvents.get(0), REQUESTS_RUNNING, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEvents.get(1), REQUESTS_RUNNING, INC, SESSION1_OWNER, SESSION1, "1");

        // success referencing request 1 new file
        checkStepEvent(stepEvents.get(2), REFERENCE_REQUESTS, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEvents.get(3), REQUESTS_RUNNING, DEC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEvents.get(4), REFERENCE_REQUESTS, DEC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEvents.get(5), REFERENCED_FILES, INC, SESSION1_OWNER, SESSION1, "1");

        // success referencing request 2 new file
        checkStepEvent(stepEvents.get(6), REFERENCE_REQUESTS, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEvents.get(7), REQUESTS_RUNNING, DEC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEvents.get(8), REFERENCE_REQUESTS, DEC, SESSION1_OWNER, SESSION1, "1");
    }

    @Test
    public void add_file_reference_no_checksum() {

        // GIVEN
        final FileReferenceRequestArgs args = newReferenceRequestArgs();

        // Create a new bus message File reference request
        final String groupId = UUID.randomUUID().toString();
        final FilesReferenceEvent event = newFilesReferenceEvent(groupId, args);
        // hack to reset to null the checksum. constructor does not allow nullity.
        event.getFiles().iterator().next().setChecksum(null);
        // Publish request.
        referenceRequestPublisher.publishReferenceEvents(0, event);

        // Check file is not referenced
        final FileReference fileRef = referenceService.search(args.getStorage(), args.getChecksum()).orElse(null);
        assertThat(fileRef).as("File should be present").isNull();

        // check for event published
        final ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(0)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());

        final FileGroupRequestStatus state = getFileRequestsGroupEvent(argumentCaptor.getAllValues()).getState();
        assertThat(state).isEqualTo(FileGroupRequestStatus.DENIED);

        final FileReferenceEvent foundEvent = getFileReferenceEvent(argumentCaptor.getAllValues());
        assertThat(foundEvent).isNull();

        // Check step events were correctly send
        final List<StepPropertyUpdateRequestEvent> stepEvents = getStepPropertyEvents(argumentCaptor.getAllValues());
        assertThat(stepEvents).as("Unexpected number of StepPropertyUpdateRequestEvents").hasSize(1);

        checkStepEvent(stepEvents.get(0), REQUESTS_REFUSED, INC, SESSION1_OWNER, SESSION1, "1");
    }

    /**
     * Test request to reference a file already stored.
     * storage and checksum identical.
     */
    @Test
    @SneakyThrows
    public void add_file_reference_already_exists() {
        // GIVEN
        final FileReference fileRef = this.generateStoredFileReference(RandomChecksumUtils.generateRandomChecksum(),
                                                                       OWNER1,
                                                                       FILE_REF_NAME,
                                                                       ONLINE_CONF_LABEL,
                                                                       Optional.empty(),
                                                                       Optional.empty(),
                                                                       SESSION1_OWNER,
                                                                       SESSION1);
        final String storage = fileRef.getLocation().getStorage();
        final String checksum = fileRef.getMetaInfo().getChecksum();

        final FileReference foundFileRef = referenceService.search(storage, checksum).orElse(null);
        assertThat(foundFileRef).as("File should be present").isNotNull();
        assertThat(foundFileRef.isReferenced()).as("File should be referenced").isFalse();

        // One store event should be sent
        Mockito.verify(this.publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));

        // Create a request to reference a file with the same checksum as the one stored before but with a new owner
        final FileReferenceRequestArgs args = newReferenceRequestArgs2().withChecksum(checksum).withStorage(storage);
        final FilesReferenceEvent event = newFilesReferenceEvent(UUID.randomUUID().toString(), args);

        // WHEN
        referenceRequestPublisher.publishReferenceEvents(1, event);

        // THEN
        assertStepEventsWhenAlreadyExists(storage, checksum);
    }

    /**
     * Request to reference a file already stored.
     * Storage are distinct. Checksum are identical.
     */
    @Test
    @SneakyThrows
    public void add_file_reference_already_stored() {

        // GIVEN
        final String checksum = RandomChecksumUtils.generateRandomChecksum();
        this.generateStoredFileReference(checksum,
                                         OWNER1,
                                         FILE_REF_NAME,
                                         ONLINE_CONF_LABEL,
                                         Optional.empty(),
                                         Optional.empty(),
                                         SESSION1_OWNER,
                                         SESSION1);

        // Create reference request with a new storage and same checksum
        final FileReferenceRequestArgs args = newReferenceRequestArgs2().withChecksum(checksum);
        final String groupId = UUID.randomUUID().toString();
        final FilesReferenceEvent event = newFilesReferenceEvent(groupId, args);

        // WHEN
        referenceRequestPublisher.publishReferenceEvents(1, event);

        // THEN

        // Check file is well referenced
        final FileReference newFileRef = referenceService.search(args.getStorage(), checksum).orElse(null);
        assertThat(newFileRef).as("File with same checksum sand new storage should have a new reference").isNotNull();
        assertThat(newFileRef.isReferenced()).isTrue();

        assertStepEventsWhenAlreadyExists(ONLINE_CONF_LABEL, checksum);
    }

    private void assertStepEventsWhenAlreadyExists(String storage, String checksum) {

        // Check file is well referenced
        final FileReference foundFileRef2 = referenceService.search(storage, checksum).orElse(null);
        assertThat(foundFileRef2).as("File should be present").isNotNull();
        assertThat(foundFileRef2.isReferenced()).as("File should be referenced").isFalse();

        // Now check for event published. One for each referenced file
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(2)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());

        // reference event STORED
        final FileReferenceEventType type = getFileReferenceEvent(argumentCaptor.getAllValues()).getType();
        assertThat(type).as("File reference event STORED should be published").isEqualTo(FileReferenceEventType.STORED);

        // Check step events were correctly send
        final List<StepPropertyUpdateRequestEvent> stepEvents = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 9, stepEvents.size());
        // storage request : file getting stored
        checkStepEvent(stepEvents.get(0), STORE_REQUESTS, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEvents.get(1), REQUESTS_RUNNING, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEvents.get(2), REQUESTS_RUNNING, DEC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEvents.get(3), STORED_FILES, INC, SESSION1_OWNER, SESSION1, "1");

        // reference request : file already referenced but a new owner is added
        checkStepEvent(stepEvents.get(4), REQUESTS_RUNNING, INC, SESSION2_OWNER, SESSION2, "1");
        checkStepEvent(stepEvents.get(5), REFERENCE_REQUESTS, INC, SESSION2_OWNER, SESSION2, "1");
        checkStepEvent(stepEvents.get(6), REQUESTS_RUNNING, DEC, SESSION2_OWNER, SESSION2, "1");
        checkStepEvent(stepEvents.get(7), REFERENCE_REQUESTS, DEC, SESSION2_OWNER, SESSION2, "1");
        checkStepEvent(stepEvents.get(8), REFERENCED_FILES, INC, SESSION2_OWNER, SESSION2, "1");
    }

    private FileReferenceRequestArgs newReferenceRequestArgs() {

        return FileReferenceRequestArgs.builder()
                                       .storage(STORAGE1)
                                       .checksum(RandomChecksumUtils.generateRandomChecksum())
                                       .algorithm("MD5")
                                       .fileName(FILE_REF_NAME)
                                       .url("file://" + STORAGE1 + "/location/" + FILE_REF_NAME)
                                       .mimeType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                                       .fileSize(10L)
                                       .owner(OWNER1)
                                       .session(SESSION1)
                                       .sessionOwner(SESSION1_OWNER)
                                       .build();

    }

    private FileReferenceRequestArgs newReferenceRequestArgs2() {

        return FileReferenceRequestArgs.builder()
                                       .storage(STORAGE1)
                                       .checksum(RandomChecksumUtils.generateRandomChecksum())
                                       .algorithm("MD5")
                                       .fileName(FILE_REF_NAME)
                                       .url("file://" + STORAGE1 + "/location/" + FILE_REF_NAME)
                                       .mimeType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                                       .fileSize(10L)
                                       .owner(OWNER2)
                                       .session(SESSION2)
                                       .sessionOwner(SESSION2_OWNER)
                                       .build();

    }

    private FilesReferenceEvent newFilesReferenceEvent(FileReferenceRequestArgs args) {
        return newFilesReferenceEvent(UUID.randomUUID().toString(), args);
    }

    private FilesReferenceEvent newFilesReferenceEvent(String groupId, FileReferenceRequestArgs args) {
        final FileReferenceRequestDto dto = FileReferenceRequestDto.build(args.getFileName(),
                                                                          args.getChecksum(),
                                                                          args.getAlgorithm(),
                                                                          args.getMimeType(),
                                                                          args.getFileSize(),
                                                                          args.getOwner(),
                                                                          args.getStorage(),
                                                                          args.getUrl(),
                                                                          args.getSessionOwner(),
                                                                          args.getSession());
        return new FilesReferenceEvent(dto, groupId);
    }

}