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
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.fileaccess.amqp.input.FileStorageRequestReadyToProcessEvent;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageResponseEvent;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageWorkerRequestEvent;
import fr.cnes.regards.modules.fileaccess.dto.AbstractStoragePluginConfigurationDto;
import fr.cnes.regards.modules.fileaccess.dto.input.FileStorageMetaInfoDto;
import fr.cnes.regards.modules.fileaccess.dto.output.StorageResponseErrorEnum;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestAggregationDto;
import fr.cnes.regards.modules.fileaccess.plugin.domain.*;
import fr.cnes.regards.modules.fileaccess.plugin.dto.FileCacheRequestDto;
import fr.cnes.regards.modules.fileaccess.plugin.dto.FileDeletionRequestDto;
import fr.cnes.regards.modules.fileaccess.service.StoragePluginConfigurationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Thibaud Michaudel
 **/
public class FileStorageRequestReadyToProcessEventHandlerTest {

    public static final String VALIDATION_ERROR = "Simulated error";

    public static final String STORAGE_NAME = "Storage1";

    private FilesStorageRequestReadyToProcessEventHandler handler;

    private StoragePluginConfigurationService storagePluginConfigurationService;

    private ISubscriber subscriber;

    private IPublisher publisher;

    private IRuntimeTenantResolver tenantResolver;

    private IPluginService pluginService;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);

        storagePluginConfigurationService = Mockito.mock(StoragePluginConfigurationService.class);
        tenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        subscriber = Mockito.mock(ISubscriber.class);
        publisher = Mockito.mock(IPublisher.class);
        pluginService = Mockito.mock(IPluginService.class);

        Mockito.when(storagePluginConfigurationService.getByName(STORAGE_NAME))
               .thenReturn(Optional.of(new TestStoragePluginConfigurationDto("Storage1Value")));
        Mockito.when(storagePluginConfigurationService.getByName("Storage2"))
               .thenReturn(Optional.of(new TestStoragePluginConfigurationDto("Storage2Value")));
        Mockito.when(tenantResolver.getTenant()).thenReturn("Tenant1");
        handler = new FilesStorageRequestReadyToProcessEventHandler(subscriber,
                                                                    storagePluginConfigurationService,
                                                                    publisher,
                                                                    tenantResolver,
                                                                    pluginService);
    }

    @Test
    public void test_3_ok_1_nok() {

        FileStorageMetaInfoDto metaInfoDto = new FileStorageMetaInfoDto("text/plain", "RAWDATA", 0, 0);
        // Given
        FileStorageRequestReadyToProcessEvent event1 = new FileStorageRequestReadyToProcessEvent(1L,
                                                                                                 "checksum1",
                                                                                                 "MD5",
                                                                                                 "http://url1.com",
                                                                                                 STORAGE_NAME,
                                                                                                 "/sub/dir",
                                                                                                 " owner1",
                                                                                                 "session1",
                                                                                                 false,
                                                                                                 metaInfoDto,
                                                                                                 false);
        FileStorageRequestReadyToProcessEvent event2 = new FileStorageRequestReadyToProcessEvent(2L,
                                                                                                 "checksum2",
                                                                                                 "MD5",
                                                                                                 "http://url2.com",
                                                                                                 STORAGE_NAME,
                                                                                                 "/sub/dir",
                                                                                                 " owner1",
                                                                                                 "session1",
                                                                                                 false,
                                                                                                 metaInfoDto,
                                                                                                 false);
        FileStorageRequestReadyToProcessEvent event3 = new FileStorageRequestReadyToProcessEvent(3L,
                                                                                                 "checksum3",
                                                                                                 "MD5",
                                                                                                 "http://url3.com",
                                                                                                 "Storage2",
                                                                                                 "/sub/dir",
                                                                                                 " owner1",
                                                                                                 "session1",
                                                                                                 false,
                                                                                                 metaInfoDto,
                                                                                                 false);
        FileStorageRequestReadyToProcessEvent event4 = new FileStorageRequestReadyToProcessEvent(4L,
                                                                                                 "checksum4",
                                                                                                 "MD5",
                                                                                                 "http://url4.com",
                                                                                                 "Storage3",
                                                                                                 "/sub/dir",
                                                                                                 " owner1",
                                                                                                 "session1",
                                                                                                 false,
                                                                                                 metaInfoDto,
                                                                                                 false);

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
        Assertions.assertEquals(StorageResponseErrorEnum.UNKNOWN_STORAGE_LOCATION,
                                error.getErrorType(),
                                "The error is not the expected one");

        // Perf
        Mockito.verify(storagePluginConfigurationService, Mockito.times(3)).getByName(Mockito.anyString());

    }

    @Test
    public void test_reference_ok() throws ModuleException {

        // Given
        String pluginBusinessId = STORAGE_NAME;
        String url = "http://url1.com";

        Mockito.when(pluginService.getPlugin(pluginBusinessId)).thenReturn(new TestStorageLocation(true));

        FileStorageMetaInfoDto metaInfoDto = new FileStorageMetaInfoDto("text/plain", "RAWDATA", 0, 0);

        FileStorageRequestReadyToProcessEvent event = new FileStorageRequestReadyToProcessEvent(1L,
                                                                                                "checksum1",
                                                                                                "MD5",
                                                                                                url,
                                                                                                pluginBusinessId,
                                                                                                "/sub/dir",
                                                                                                " owner1",
                                                                                                "session1",
                                                                                                false,
                                                                                                metaInfoDto,
                                                                                                true);

        // When
        handler.handleBatch(List.of(event));

        // Then
        ArgumentCaptor<List> publishedEventsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(publishedEventsCaptor.capture());
        List<List> allEvents = publishedEventsCaptor.getAllValues();
        Optional<List> oEventList = allEvents.stream()
                                             .filter(l -> l.get(0) instanceof StorageResponseEvent)
                                             .findFirst();
        Assertions.assertTrue(oEventList.isPresent());
        List<StorageResponseEvent> eventList = oEventList.get();
        Assertions.assertEquals(1, eventList.size(), "There should be only one success");
        Assertions.assertEquals(url, eventList.get(0).getUrl(), "The url should be the same one as the request");
        Assertions.assertEquals(null, eventList.get(0).getErrorType(), "There should be no error");

    }

    @Test
    public void test_reference_nok() throws ModuleException {

        // Given
        String pluginBusinessId = "storage-test";
        String url = "http://url1.com";

        Mockito.when(pluginService.getPlugin(pluginBusinessId)).thenReturn(new TestStorageLocation(false));

        FileStorageMetaInfoDto metaInfoDto = new FileStorageMetaInfoDto("text/plain", "RAWDATA", 0, 0);

        FileStorageRequestReadyToProcessEvent event = new FileStorageRequestReadyToProcessEvent(1L,
                                                                                                "checksum1",
                                                                                                "MD5",
                                                                                                url,
                                                                                                pluginBusinessId,
                                                                                                "/sub/dir",
                                                                                                " owner1",
                                                                                                "session1",
                                                                                                false,
                                                                                                metaInfoDto,
                                                                                                true);

        // When
        handler.handleBatch(List.of(event));

        // Then
        ArgumentCaptor<List> publishedEventsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(publishedEventsCaptor.capture());
        List<List> allEvents = publishedEventsCaptor.getAllValues();
        Optional<List> oEventList = allEvents.stream()
                                             .filter(l -> l.get(0) instanceof StorageResponseEvent)
                                             .findFirst();
        Assertions.assertTrue(oEventList.isPresent());
        List<StorageResponseEvent> eventList = oEventList.get();
        Assertions.assertEquals(1, eventList.size(), "There should be only one error");
        Assertions.assertEquals(url, eventList.get(0).getUrl(), "The url should be the same one as the request");
        Assertions.assertNotNull(eventList.get(0).getErrorType(), "There should be an error");

    }

    private class TestStoragePluginConfigurationDto extends AbstractStoragePluginConfigurationDto {

        private final String value;

        public TestStoragePluginConfigurationDto(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private class TestStorageLocation implements IStorageLocation {

        private boolean valid;

        private TestStorageLocation(boolean valid) {
            this.valid = valid;
        }

        @Override
        public boolean isValidUrl(String urlToValidate, Set<String> errors) {
            if (!valid) {
                errors.add(VALIDATION_ERROR);
            }
            return valid;
        }

        @Override
        public PreparationResponse<FileStorageWorkingSubset, FileStorageRequestAggregationDto> prepareForStorage(
            Collection<FileStorageRequestAggregationDto> fileReferenceRequests) {
            return null;
        }

        @Override
        public PreparationResponse<FileDeletionWorkingSubset, FileDeletionRequestDto> prepareForDeletion(Collection<FileDeletionRequestDto> fileDeletionRequests) {
            return null;
        }

        @Override
        public PreparationResponse<FileRestorationWorkingSubset, FileCacheRequestDto> prepareForRestoration(Collection<FileCacheRequestDto> requests) {
            return null;
        }

        @Override
        public void delete(FileDeletionWorkingSubset workingSet, IDeletionProgressManager progressManager) {

        }

        @Override
        public void store(FileStorageWorkingSubset workingSet, IStorageProgressManager progressManager) {

        }

        @Override
        public boolean allowPhysicalDeletion() {
            return false;
        }
    }

}
