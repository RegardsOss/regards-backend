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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.fileaccess.service.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnes.regards.framework.amqp.Publisher;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageResponseEvent;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageWorkerRequestEvent;
import fr.cnes.regards.modules.fileaccess.dto.output.StorageResponseErrorEnum;
import fr.cnes.regards.modules.fileaccess.dto.output.worker.StorageWorkerResponseDto;
import fr.cnes.regards.modules.fileaccess.dto.output.worker.type.FileMetadata;
import fr.cnes.regards.modules.fileaccess.dto.output.worker.type.FileProcessingMetadata;
import fr.cnes.regards.modules.fileaccess.service.FileStorageService;
import fr.cnes.regards.modules.workermanager.amqp.events.out.ResponseEvent;
import fr.cnes.regards.modules.workermanager.amqp.events.out.ResponseStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

/**
 * Tests of possibles cases of responses of storage-worker. Responses must be transformed and re-published by amqp.
 *
 * @author tguillou
 */
public class WorkerResponseEventHandlerTest {

    private Publisher publisher;

    private WorkerResponseEventHandler workerResponseEventHandler;

    private FileStorageService fileStorageService;

    @Before
    public void before() {
        publisher = Mockito.mock(Publisher.class);
        Mockito.doCallRealMethod().when(publisher).publish(Mockito.anyList());
        fileStorageService = new FileStorageService(publisher);
        workerResponseEventHandler = new WorkerResponseEventHandler(null, fileStorageService);
    }

    @Test
    public void test_worker_success_nominal() {
        // GIVEN
        StorageWorkerResponseDto storageWorkerResponseDto = buildWorkerResponseContent("checksum", "cachePath");
        List<ResponseEvent> workerResponse = buildResponseWithContent(ResponseStatus.SUCCESS,
                                                                      "store-coucou",
                                                                      storageWorkerResponseDto);
        // WHEN
        workerResponseEventHandler.handleBatch(workerResponse);
        // THEN
        List<StorageResponseEvent> eventPublished = getEventPublished();
        Assertions.assertEquals(1, eventPublished.size());
        Assertions.assertTrue(eventPublished.get(0).isRequestSuccessful());
        Assertions.assertEquals("checksum", eventPublished.get(0).getChecksum());
    }

    @Test
    public void test_worker_success_no_cache() {
        // GIVEN
        StorageWorkerResponseDto storageWorkerResponseDto = buildWorkerResponseContent("checksum", null);
        List<ResponseEvent> workerResponse = buildResponseWithContent(ResponseStatus.SUCCESS,
                                                                      "store-coucou",
                                                                      storageWorkerResponseDto);
        // WHEN
        workerResponseEventHandler.handleBatch(workerResponse);
        // THEN
        List<StorageResponseEvent> eventPublished = getEventPublished();
        Assertions.assertEquals(1, eventPublished.size());
        StorageResponseEvent storageResponseEvent = eventPublished.get(0);
        Assertions.assertTrue(storageResponseEvent.isRequestSuccessful());
        Assertions.assertEquals("checksum", storageResponseEvent.getChecksum());
        Assertions.assertFalse(storageResponseEvent.isStoredInCache());
    }

    @Test
    public void test_worker_success_in_cache() {
        // GIVEN
        StorageWorkerResponseDto storageWorkerResponseDto = buildWorkerResponseContent("checksum", "someCachePath");
        List<ResponseEvent> workerResponse = buildResponseWithContent(ResponseStatus.SUCCESS,
                                                                      "store-coucou",
                                                                      storageWorkerResponseDto);
        // WHEN
        workerResponseEventHandler.handleBatch(workerResponse);
        // THEN
        List<StorageResponseEvent> eventPublished = getEventPublished();
        Assertions.assertEquals(1, eventPublished.size());
        StorageResponseEvent storageResponseEvent = eventPublished.get(0);
        Assertions.assertTrue(storageResponseEvent.isRequestSuccessful());
        Assertions.assertEquals("checksum", storageResponseEvent.getChecksum());
        Assertions.assertTrue(storageResponseEvent.isStoredInCache());
    }

    @Test
    public void test_worker_success_empty_response() {
        // GIVEN
        List<ResponseEvent> workerResponse = buildWorkerResponse(ResponseStatus.SUCCESS, "store-coucou");
        // WHEN
        workerResponseEventHandler.handleBatch(workerResponse);
        // THEN
        List<StorageResponseEvent> eventPublished = getEventPublished();
        Assertions.assertEquals(1, eventPublished.size());
        Assertions.assertEquals(StorageResponseErrorEnum.WORKER_RESPONSE_EMPTY, eventPublished.get(0).getErrorType());
    }

    @Test
    public void test_worker_error() {
        // GIVEN
        List<ResponseEvent> workerResponse = buildWorkerResponse(ResponseStatus.ERROR, "store-coucou");
        // WHEN
        workerResponseEventHandler.handleBatch(workerResponse);
        // THEN
        List<StorageResponseEvent> eventPublished = getEventPublished();
        Assertions.assertEquals(1, eventPublished.size());
        Assertions.assertEquals(StorageResponseErrorEnum.WORKER_ERROR, eventPublished.get(0).getErrorType());
        Assertions.assertFalse(eventPublished.get(0).isRequestSuccessful());
    }

    @Test
    public void test_worker_error_wrong_content_type() {
        // GIVEN a worker response which not respect content-type pattern : store-*
        StorageWorkerResponseDto storageWorkerResponseDto = buildWorkerResponseContent("checksum", "someCachePath");
        List<ResponseEvent> workerResponse = buildResponseWithContent(ResponseStatus.SUCCESS,
                                                                      "wrongContentType",
                                                                      storageWorkerResponseDto);
        // WHEN
        workerResponseEventHandler.handleBatch(workerResponse);
        // THEN worker has done nothing, so no response.
        Mockito.verify(publisher, Mockito.never()).publish(Mockito.any(StorageResponseEvent.class));
    }

    private static StorageWorkerResponseDto buildWorkerResponseContent(String checksum, String cachePath) {
        FileMetadata fileMetadata = new FileMetadata("checksum", "algorithm", "url", 67L);
        FileProcessingMetadata fileProcessingMetadata = new FileProcessingMetadata(true,
                                                                                   cachePath,
                                                                                   "filename",
                                                                                   "storeParentUrl",
                                                                                   "storeParentPath");
        return new StorageWorkerResponseDto(fileProcessingMetadata, fileMetadata);
    }

    private List<ResponseEvent> buildWorkerResponse(ResponseStatus status, String contentType) {
        ResponseEvent event = ResponseEvent.build(status, "1", "type", "owner");
        event.getMessageProperties().setHeader(StorageWorkerRequestEvent.CONTENT_TYPE_HEADER, contentType);
        return List.of(event);
    }

    private List<ResponseEvent> buildResponseWithContent(ResponseStatus status,
                                                         String contentType,
                                                         StorageWorkerResponseDto content) {
        ResponseEvent event = ResponseEvent.build(status, "1", "type", "owner");
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String contentAsString = objectMapper.writeValueAsString(content);
            event.withContent(contentAsString.getBytes());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        event.getMessageProperties().setHeader(StorageWorkerRequestEvent.CONTENT_TYPE_HEADER, contentType);
        return List.of(event);
    }

    private List<StorageResponseEvent> getEventPublished() {
        ArgumentCaptor<StorageResponseEvent> requestCaptor = ArgumentCaptor.forClass(StorageResponseEvent.class);
        Mockito.verify(publisher).publish(requestCaptor.capture());
        return requestCaptor.getAllValues();
    }
}
