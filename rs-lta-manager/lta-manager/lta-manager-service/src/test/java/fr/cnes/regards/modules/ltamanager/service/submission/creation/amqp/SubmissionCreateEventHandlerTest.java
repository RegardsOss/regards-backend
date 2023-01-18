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
package fr.cnes.regards.modules.ltamanager.service.submission.creation.amqp;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ltamanager.amqp.input.SubmissionRequestDtoEvent;
import fr.cnes.regards.modules.ltamanager.amqp.output.LtaWorkerRequestDtoEvent;
import fr.cnes.regards.modules.ltamanager.amqp.output.SubmissionResponseDtoEvent;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.settings.DatatypeParameter;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.ProductFileDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseStatus;
import fr.cnes.regards.modules.ltamanager.service.settings.LtaSettingService;
import fr.cnes.regards.modules.ltamanager.service.submission.creation.CreateDatatypeService;
import fr.cnes.regards.modules.ltamanager.service.submission.creation.SubmissionCreateService;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.validation.Validator;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static fr.cnes.regards.modules.ltamanager.service.submission.creation.SubmissionCreateService.LTA_CONTENT_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * Test for {@link SubmissionCreateEventHandler}
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class SubmissionCreateEventHandlerTest {

    private static final int NB_REQUEST_DTOS = 10;

    private static final int REQ_EXPIRES_IN_HOURS = 1;

    private static final String OWNER = "user";

    private static final String STORE_PATH = "/path/example/to/storage";

    private static final String STORAGE = "ATL";

    private static final String MODEL = "modelSample";

    private static final String STORE_PATH_CONFIG = "/pathExample";

    @Mock
    private ISubmissionRequestRepository requestRepository;
    
    @Spy
    private IPublisher publisher;

    @Spy
    private ISubscriber subscriber;

    @Spy
    private Validator validator;

    @Spy
    private IRuntimeTenantResolver tenantResolver;

    @Mock
    private LtaSettingService settingService;

    // Class under test
    private SubmissionCreateEventHandler requestHandler;

    private AutoCloseable closable;

    private final List<SubmissionRequestDtoEvent> requestDtos = new ArrayList<>();

    @Before
    public void init() {
        closable = openMocks(this);
        CreateDatatypeService createDatatypeService = new CreateDatatypeService(requestRepository);
        
        SubmissionCreateService createService = new SubmissionCreateService(requestRepository,
                                                                            settingService,
                                                                            createDatatypeService,
                                                                            publisher,
                                                                            tenantResolver);
        requestHandler = new SubmissionCreateEventHandler(subscriber, createService, publisher, validator);
        initData();
        initMockBehaviours();
    }

    private void initMockBehaviours() {
        // mock call to lta settings to prevent errors during the validation of submission request dtos
        Mockito.when(settingService.getDatypesConfig(any()))
               .thenReturn(Map.of(EntityType.DATA.toString(), new DatatypeParameter(MODEL, STORE_PATH_CONFIG)));
        Mockito.when(settingService.getRequestExpiresInHoursConfig(any())).thenReturn(REQ_EXPIRES_IN_HOURS);
        Mockito.when(settingService.getStorageConfig(any())).thenReturn(STORAGE);
        Mockito.when(requestRepository.existsByCorrelationId(any())).thenReturn(false);
    }

    private void initData() {
        List<ProductFileDto> files = List.of(new ProductFileDto(DataType.RAWDATA,
                                                                "http://localhost/notexisting",
                                                                "example.raw",
                                                                "f016852239a8a919f05f6d2225c5aaca",
                                                                MediaType.APPLICATION_OCTET_STREAM));
        for (int i = 0; i < NB_REQUEST_DTOS; i++) {
            SubmissionRequestDtoEvent requestDto = new SubmissionRequestDtoEvent("Test RequestDto n°" + i,
                                                                                 UUID.randomUUID().toString(),
                                                                                 EntityType.DATA.toString(),
                                                                                 IGeometry.point(IGeometry.position(10.0,
                                                                                                                    20.0)),
                                                                                 files);
            requestDto.setStorePath(STORE_PATH);
            requestDto.setOwner(OWNER);
            requestDtos.add(requestDto);
        }
    }

    @Test
    @Purpose("Test if submission requests are successfully saved from submission request dtos through amqp")
    public void handle_submission_requests_success() {
        // GIVEN
        // submissionRequestDtos (product requests to send)
        // mock call to database
        AtomicReference<List<SubmissionRequest>> refRequestsCreated = new AtomicReference<>(new ArrayList<>());
        Mockito.when(requestRepository.saveAll(any())).thenAnswer(res -> {
            refRequestsCreated.set(res.getArgument(0));
            return refRequestsCreated.get();
        });

        // WHEN
        requestHandler.handleBatch(requestDtos);

        // THEN
        // check if responses are built as expected, ie, with information related to the submission requests saved
        List<SubmissionResponseDtoEvent> expectedDtosResponses = new ArrayList<>();
        List<LtaWorkerRequestDtoEvent> expectedWorkerRequests = new ArrayList<>();
        for (SubmissionRequest requestCreated : refRequestsCreated.get()) {
            // submission response dto

            SubmissionResponseDtoEvent expectedResponse = new SubmissionResponseDtoEvent(requestCreated.getCorrelationId(),
                                                                                         SubmissionResponseStatus.GRANTED,
                                                                                         requestCreated.getProduct()
                                                                                                       .getId(),
                                                                                         requestCreated.getCreationDate()
                                                                                                       .plusSeconds(
                                                                                                           REQ_EXPIRES_IN_HOURS),
                                                                                         OWNER
                                                                                         + "-"
                                                                                         + OffsetDateTime.now()
                                                                                                         .format(
                                                                                                             DateTimeFormatter.BASIC_ISO_DATE),
                                                                                         null);
            expectedDtosResponses.add(expectedResponse);
            // worker request dto
            LtaWorkerRequestDtoEvent expectedWorkerReq = new LtaWorkerRequestDtoEvent(STORAGE,
                                                                                      Paths.get(STORE_PATH_CONFIG),
                                                                                      MODEL,
                                                                                      requestCreated.getProduct(),
                                                                                      false);
            expectedWorkerReq.setWorkerHeaders(LTA_CONTENT_TYPE,
                                               tenantResolver.getTenant(),
                                               requestCreated.getCorrelationId(),
                                               requestCreated.getOwner(),
                                               requestCreated.getSession());
            expectedWorkerRequests.add(expectedWorkerReq);
        }

        // capture events sent
        ArgumentCaptor<List<ISubscribable>> captorPublished = ArgumentCaptor.forClass(List.class);
        Mockito.verify(publisher).publish(captorPublished.capture(), anyString(), any(Optional.class));
        Mockito.verify(publisher).publish(captorPublished.capture());
        List<List<ISubscribable>> capturedPublishedEvents = captorPublished.getAllValues();
        Assertions.assertThat(capturedPublishedEvents).hasSize(2);
        // worker requests
        Assertions.assertThat(capturedPublishedEvents.get(0)).hasSameElementsAs(expectedWorkerRequests);
        // submission response dtos
        Assertions.assertThat(capturedPublishedEvents.get(1)).hasSameElementsAs(expectedDtosResponses);
    }

    @After
    public void closeMocks() throws Exception {
        closable.close();
    }

}
