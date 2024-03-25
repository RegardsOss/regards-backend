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
package fr.cnes.regards.modules.fileaccess.service.handler;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.fileaccess.amqp.input.FileStorageRequestReadyToProcessEvent;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageResponseEvent;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageWorkerRequestEvent;
import fr.cnes.regards.modules.fileaccess.dto.IStoragePluginConfigurationDto;
import fr.cnes.regards.modules.fileaccess.dto.input.FileStorageMetaInfoDto;
import fr.cnes.regards.modules.fileaccess.service.StoragePluginConfigurationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

/**
 * @author Thibaud Michaudel
 **/
public class FileStorageRequestReadyToProcessEventHandlerTest {

    private FilesStorageRequestReadyToProcessEventHandler handler;

    private StoragePluginConfigurationService storagePluginConfigurationService;

    private ISubscriber subscriber;

    private IPublisher publisher;

    private IRuntimeTenantResolver tenantResolver;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);

        storagePluginConfigurationService = Mockito.mock(StoragePluginConfigurationService.class);
        tenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        subscriber = Mockito.mock(ISubscriber.class);
        publisher = Mockito.mock(IPublisher.class);

        Mockito.when(storagePluginConfigurationService.getByName("Storage1"))
               .thenReturn(Optional.of(new TestStoragePluginConfigurationDto("Storage1Value")));
        Mockito.when(storagePluginConfigurationService.getByName("Storage2"))
               .thenReturn(Optional.of(new TestStoragePluginConfigurationDto("Storage2Value")));
        Mockito.when(tenantResolver.getTenant()).thenReturn("Tenant1");
        handler = new FilesStorageRequestReadyToProcessEventHandler(subscriber,
                                                                    storagePluginConfigurationService,
                                                                    publisher,
                                                                    tenantResolver);
    }

    @Test
    public void test_3_ok_1_nok() {

        FileStorageMetaInfoDto metaInfoDto = new FileStorageMetaInfoDto("text/plain", "RAWDATA", 0, 0);
        // Given
        FileStorageRequestReadyToProcessEvent event1 = new FileStorageRequestReadyToProcessEvent(1L,
                                                                                                 "checksum1",
                                                                                                 "MD5",
                                                                                                 "http://url1.com",
                                                                                                 "Storage1",
                                                                                                 "/sub/dir",
                                                                                                 " owner1",
                                                                                                 "session1",
                                                                                                 metaInfoDto);
        FileStorageRequestReadyToProcessEvent event2 = new FileStorageRequestReadyToProcessEvent(2L,
                                                                                                 "checksum2",
                                                                                                 "MD5",
                                                                                                 "http://url2.com",
                                                                                                 "Storage1",
                                                                                                 "/sub/dir",
                                                                                                 " owner1",
                                                                                                 "session1",
                                                                                                 metaInfoDto);
        FileStorageRequestReadyToProcessEvent event3 = new FileStorageRequestReadyToProcessEvent(3L,
                                                                                                 "checksum3",
                                                                                                 "MD5",
                                                                                                 "http://url3.com",
                                                                                                 "Storage2",
                                                                                                 "/sub/dir",
                                                                                                 " owner1",
                                                                                                 "session1",
                                                                                                 metaInfoDto);
        FileStorageRequestReadyToProcessEvent event4 = new FileStorageRequestReadyToProcessEvent(4L,
                                                                                                 "checksum4",
                                                                                                 "MD5",
                                                                                                 "http://url4.com",
                                                                                                 "Storage3",
                                                                                                 "/sub/dir",
                                                                                                 " owner1",
                                                                                                 "session1",
                                                                                                 metaInfoDto);

        // When
        handler.handleBatch(List.of(event1, event2, event3, event4));

        // Then
        ArgumentCaptor<List> publishedEventsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(publisher, Mockito.times(2)).publish(publishedEventsCaptor.capture());
        List<List> allEvents = publishedEventsCaptor.getAllValues();

        // Ok
        Optional<List> oOkList = allEvents.stream()
                                          .filter(l -> l.get(0) instanceof StorageWorkerRequestEvent)
                                          .findFirst();
        Assertions.assertTrue(oOkList.isPresent());
        List okList = oOkList.get();
        Assertions.assertEquals(3, okList.size(), "There should be 3 requests");

        // Nok
        Optional<List> oNokList = allEvents.stream().filter(l -> l.get(0) instanceof StorageResponseEvent).findFirst();
        Assertions.assertTrue(oNokList.isPresent());
        List<StorageResponseEvent> nokList = oNokList.get();
        Assertions.assertEquals(1, nokList.size(), "There should be only one error");
        StorageResponseEvent error = nokList.get(0);
        Assertions.assertEquals(4L, error.getRequestId(), "The request in error is not the expected one");
        Assertions.assertEquals(FilesStorageRequestReadyToProcessEventHandler.UNKNOWN_STORAGE_LOCATION,
                                error.getErrorType(),
                                "The error is not the expected one");

        // Perf
        Mockito.verify(storagePluginConfigurationService, Mockito.times(3)).getByName(Mockito.anyString());

    }

    private class TestStoragePluginConfigurationDto implements IStoragePluginConfigurationDto {

        private final String value;

        public TestStoragePluginConfigurationDto(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}
