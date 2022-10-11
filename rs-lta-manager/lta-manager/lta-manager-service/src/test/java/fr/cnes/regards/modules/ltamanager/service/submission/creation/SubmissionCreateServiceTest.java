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
package fr.cnes.regards.modules.ltamanager.service.submission.creation;

import com.google.gson.GsonBuilder;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ltamanager.amqp.output.LtaWorkerRequestDtoEvent;
import fr.cnes.regards.modules.ltamanager.amqp.output.SubmissionResponseDtoEvent;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.settings.DatatypeParameter;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.ProductFileDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseStatus;
import fr.cnes.regards.modules.ltamanager.service.settings.LtaSettingService;
import fr.cnes.regards.modules.workermanager.dto.events.in.RequestEvent;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static fr.cnes.regards.modules.ltamanager.service.submission.creation.SubmissionCreateService.LTA_CONTENT_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * Test for {@link SubmissionCreateService}
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class SubmissionCreateServiceTest {

    private static final String OWNER = "user";

    @Mock
    private ISubmissionRequestRepository requestRepository;

    @Mock
    private LtaSettingService settingService;

    @Spy
    private CreateDatatypeService createDatatypeService;

    @Spy
    private IPublisher publisher;

    @Spy
    private IRuntimeTenantResolver tenantResolver;

    // Class under test
    private SubmissionCreateService createService;

    private AutoCloseable closable;

    @Before
    public void init() {
        closable = openMocks(this);
        createService = new SubmissionCreateService(requestRepository,
                                                    settingService,
                                                    createDatatypeService,
                                                    publisher,
                                                    tenantResolver);
    }

    @Test
    public void create_submission_requests_in_success() {
        // ---- GIVEN ----
        // - SubmissionRequestDtos (product requests to send) -
        List<ProductFileDto> files = List.of(new ProductFileDto(DataType.RAWDATA,
                                                                "http://localhost/notexisting",
                                                                "example.raw",
                                                                "f016852239a8a919f05f6d2225c5aaca",
                                                                MediaType.APPLICATION_OCTET_STREAM));
        // Submission request with store path
        String simpleStorePath = "/path/subpath";
        SubmissionRequestDto requestDtoWithSimpleStorePath = new SubmissionRequestDto(UUID.randomUUID().toString(),
                                                                                      EntityType.DATA.toString(),
                                                                                      IGeometry.point(IGeometry.position(
                                                                                          10.0,
                                                                                          20.0)),
                                                                                      files,
                                                                                      null,
                                                                                      null,
                                                                                      simpleStorePath,
                                                                                      "sessionSimpleStore",
                                                                                      false);

        // Submission request without a store path (it will be built via config)
        Map<String, Object> properties = new GsonBuilder().create().fromJson("""
                                                                                 {
                                                                                   "key1": "value1",
                                                                                   "subProperties":  {
                                                                                      "subKey1": "nestedValue1",
                                                                                      "subKey2": "nestedValue2"
                                                                                   }
                                                                                 }
                                                                                 """, Map.class);
        SubmissionRequestDto requestDtoWithProperties = new SubmissionRequestDto(UUID.randomUUID().toString(),
                                                                                 EntityType.DATA.toString(),
                                                                                 IGeometry.point(IGeometry.position(10.0,
                                                                                                                    20.0)),
                                                                                 files,
                                                                                 null,
                                                                                 properties,
                                                                                 null,
                                                                                 null,
                                                                                 false);

        OffsetDateTime now = OffsetDateTime.now();
        Path expectedBuiltPath = Paths.get(String.format("/%d/%d/%d/value1/nestedValue1/nestedValue2/pathExample",
                                                         now.getYear(),
                                                         now.getMonthValue(),
                                                         now.getDayOfMonth()));
        // Submission request with replaceMode
        SubmissionRequestDto requestDtoWithReplace = new SubmissionRequestDto(UUID.randomUUID().toString(),
                                                                              EntityType.DATA.toString(),
                                                                              IGeometry.point(IGeometry.position(10.0,
                                                                                                                 20.0)),
                                                                              files,
                                                                              null,
                                                                              null,
                                                                              simpleStorePath,
                                                                              null,
                                                                              true);

        requestDtoWithSimpleStorePath.setOwner(OWNER);
        requestDtoWithProperties.setOwner(OWNER);
        requestDtoWithReplace.setOwner(OWNER);

        List<SubmissionRequestDto> requestDtos = List.of(requestDtoWithSimpleStorePath,
                                                         requestDtoWithProperties,
                                                         requestDtoWithReplace);

        // - Mocks -
        // call to database
        AtomicReference<List<SubmissionRequest>> refRequestsCreated = new AtomicReference<>(new ArrayList<>());
        Mockito.when(requestRepository.saveAll(any())).thenAnswer(res -> {
            refRequestsCreated.set(res.getArgument(0));
            return refRequestsCreated.get();
        });

        // call to lta datatype settings
        String modelSample = "modelSample";
        String configStorePath = "/${YEAR}/${MONTH}/${DAY}/${PROPERTY(key1)}/${PROPERTY(subProperties.subKey1)}/${PROPERTY(subProperties.subKey2)}/pathExample";
        Mockito.when(settingService.getDatypesConfig(any()))
               .thenReturn(Map.of(EntityType.DATA.toString(), new DatatypeParameter(modelSample, configStorePath)));
        // call to lta storage setting
        String storage = "ATL";
        Mockito.when(settingService.getStorageConfig(any())).thenReturn(storage);
        // ---- WHEN ----
        // main method is called (submission request dtos are sent to save submission requests)
        List<SubmissionResponseDtoEvent> dtoResponses = createService.handleSubmissionRequestsCreation(requestDtos);

        // ---- THEN ----
        // - Check that requests have been correctly built -
        List<SubmissionRequest> requestsCreated = refRequestsCreated.get();
        Assertions.assertThat(requestsCreated).hasSize(3);

        for (SubmissionRequest requestCreated : requestsCreated) {
            String productId = requestCreated.getProduct().getId();
            if (productId.equals(requestDtoWithSimpleStorePath.getId())) {
                Assertions.assertThat(requestCreated.getStorePath()).isEqualTo(Paths.get(simpleStorePath));
                Assertions.assertThat(requestCreated.getSession()).isEqualTo("sessionSimpleStore");
            } else if (productId.equals(requestDtoWithProperties.getId())) {
                Assertions.assertThat(requestCreated.getStorePath()).isEqualTo(expectedBuiltPath);
                Assertions.assertThat(requestCreated.getSession())
                          .isEqualTo("user-" + OffsetDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE));
            } else if (productId.equals(requestDtoWithReplace.getId())) {
                Assertions.assertThat(requestCreated.isReplaceMode()).isTrue();
            }
        }
        // - Check all request are in validated status -
        Assertions.assertThat(Set.of(SubmissionRequestState.VALIDATED))
                  .isEqualTo(requestsCreated.stream().map(SubmissionRequest::getStatus).collect(Collectors.toSet()));
        // - Check dto answers -
        Assertions.assertThat(dtoResponses).hasSize(3);

        String tenant = tenantResolver.getTenant();
        // - Expected worker requests -
        List<LtaWorkerRequestDtoEvent> expectedWorkerReq = new ArrayList<>();
        // worker request with store path
        Path dataTypeStorePath = Paths.get(configStorePath);
        LtaWorkerRequestDtoEvent expectedReqWithPath = new LtaWorkerRequestDtoEvent(storage,
                                                                                    dataTypeStorePath,
                                                                                    modelSample,
                                                                                    requestDtoWithSimpleStorePath,
                                                                                    false);
        SubmissionRequest reqCreatedWithStorePath = requestsCreated.get(0);
        expectedReqWithPath.setWorkerHeaders(LTA_CONTENT_TYPE,
                                             tenant,
                                             reqCreatedWithStorePath.getRequestId(),
                                             reqCreatedWithStorePath.getOwner(),
                                             reqCreatedWithStorePath.getSession());
        // worker request without store path
        LtaWorkerRequestDtoEvent expectedReqWithoutPath = new LtaWorkerRequestDtoEvent(storage,
                                                                                       expectedBuiltPath,
                                                                                       modelSample,
                                                                                       requestDtoWithProperties,
                                                                                       false);
        SubmissionRequest reqCreatedWithoutStorePath = requestsCreated.get(1);
        expectedReqWithoutPath.setWorkerHeaders(LTA_CONTENT_TYPE,
                                                tenant,
                                                reqCreatedWithoutStorePath.getRequestId(),
                                                reqCreatedWithoutStorePath.getOwner(),
                                                reqCreatedWithoutStorePath.getSession());
        // worker request with replace mode
        SubmissionRequest reqCreatedWithReplace = requestsCreated.get(2);
        LtaWorkerRequestDtoEvent expectedReqWithReplace = new LtaWorkerRequestDtoEvent(storage,
                                                                                       dataTypeStorePath,
                                                                                       modelSample,
                                                                                       requestDtoWithReplace,
                                                                                       true);
        expectedReqWithReplace.setWorkerHeaders(LTA_CONTENT_TYPE,
                                                tenant,
                                                reqCreatedWithReplace.getRequestId(),
                                                reqCreatedWithReplace.getOwner(),
                                                reqCreatedWithReplace.getSession());

        expectedWorkerReq.add(expectedReqWithPath);
        expectedWorkerReq.add(expectedReqWithoutPath);
        expectedWorkerReq.add(expectedReqWithReplace);
        // check worker responses have been sent
        Mockito.verify(publisher)
               .publish(expectedWorkerReq, "regards.broadcast." + RequestEvent.class.getName(), Optional.empty());
    }

    @Test
    public void create_submission_requests_in_error_placeholders() {
        // ---- GIVEN ----
        // - SubmissionRequestDtos (product requests to send) -
        List<ProductFileDto> files = List.of(new ProductFileDto(DataType.RAWDATA,
                                                                "http://localhost/notexisting",
                                                                "example.raw",
                                                                "f016852239a8a919f05f6d2225c5aaca",
                                                                MediaType.APPLICATION_OCTET_STREAM));
        SubmissionRequestDto requestDtoWithSimpleStorePath = new SubmissionRequestDto(UUID.randomUUID().toString(),
                                                                                      EntityType.DATA.toString(),
                                                                                      IGeometry.point(IGeometry.position(
                                                                                          10.0,
                                                                                          20.0)),
                                                                                      files,
                                                                                      null,
                                                                                      null,
                                                                                      null,
                                                                                      "sessionSimpleStore",
                                                                                      false);

        Map<String, Object> properties = new GsonBuilder().create().fromJson("""
                                                                                 {
                                                                                   "key1": "value1",
                                                                                   "subProperties":  {
                                                                                      "subKey1": "nestedValue1"
                                                                                   }
                                                                                 }
                                                                                 """, Map.class);
        SubmissionRequestDto requestDtoWithProperties = new SubmissionRequestDto(UUID.randomUUID().toString(),
                                                                                 EntityType.DATA.toString(),
                                                                                 IGeometry.point(IGeometry.position(10.0,
                                                                                                                    20.0)),
                                                                                 files,
                                                                                 null,
                                                                                 properties,
                                                                                 null,
                                                                                 null,
                                                                                 false);

        requestDtoWithSimpleStorePath.setOwner(OWNER);
        requestDtoWithProperties.setOwner(OWNER);

        List<SubmissionRequestDto> requestDtos = List.of(requestDtoWithSimpleStorePath, requestDtoWithProperties);

        // - Mocks -
        // call to lta settings
        Mockito.when(settingService.getDatypesConfig(any()))
               .thenReturn(Map.of(EntityType.DATA.toString(),
                                  new DatatypeParameter("modelSample",
                                                        "/${YEAR}/${MONTH}/${DAY}/${PROPERTY(key1)}/${PROPERTY"
                                                        + "(subProperties.subKey1)}/${PROPERTY(subProperties.subKey2)}/pathExample")));

        // ---- WHEN ----
        // main method is called (submission request dtos are sent to save submission requests)
        List<SubmissionResponseDtoEvent> dtoResponses = createService.handleSubmissionRequestsCreation(requestDtos);

        // THEN
        // check that requests have been correctly built
        // check dto answers
        Assertions.assertThat(dtoResponses).hasSize(2);
        Assertions.assertThat(dtoResponses.stream()
                                          .map(SubmissionResponseDto::getResponseStatus)
                                          .collect(Collectors.toSet()))
                  .isEqualTo(Set.of(SubmissionResponseStatus.DENIED));
        // check no worker requests have been sent
        Mockito.verifyNoInteractions(publisher);
    }

    @After
    public void closeMocks() throws Exception {
        closable.close();
    }

}
