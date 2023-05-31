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
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ltamanager.amqp.output.LtaWorkerRequestDtoEvent;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.settings.DatatypeParameter;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.dto.submission.LtaDataType;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.ProductFileDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseStatus;
import fr.cnes.regards.modules.ltamanager.service.settings.LtaSettingService;
import fr.cnes.regards.modules.workermanager.amqp.events.in.RequestEvent;
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

    public static final String SIMPLE_STORE_PATH = "/path/subpath";

    public static final String LTA_STORAGE = "ATL";

    public static final String CONFIG_STORE_PATH = "/${YEAR}/${MONTH}/${DAY}/${PROPERTY(key1)}/${PROPERTY(subProperties.subKey1)}/${PROPERTY(subProperties.subKey2)}/pathExample";

    public static final String MODEL_SAMPLE = "modelSample";

    private static final String OWNER = "user";

    @Mock
    private ISubmissionRequestRepository requestRepository;

    @Mock
    private LtaSettingService settingService;

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
        CreateDatatypeService createDatatypeService = new CreateDatatypeService(requestRepository);

        createService = new SubmissionCreateService(requestRepository,
                                                    settingService,
                                                    createDatatypeService,
                                                    publisher,
                                                    tenantResolver);
    }

    @Test
    public void create_submission_requests_in_success() {
        // ---- GIVEN ----
        // Submission request with store path
        SubmissionRequestDto requestDtoWithSimpleStorePath = createRequestWithSimpleStorePath();

        // Submission request without a store path (it will be built via config)
        SubmissionRequestDto requestDtoWithProperties = createRequestWithProperties();

        // Submission request with replaceMode
        SubmissionRequestDto requestDtoWithReplace = createRequestWithReplace();

        // - Mocks -
        // call to database
        AtomicReference<List<SubmissionRequest>> refRequestsCreated = new AtomicReference<>(new ArrayList<>());
        Mockito.when(requestRepository.saveAll(any())).thenAnswer(res -> {
            refRequestsCreated.set(res.getArgument(0));
            return refRequestsCreated.get();
        });

        // call to lta datatype settings
        Mockito.when(settingService.getDatypesConfig(any()))
               .thenReturn(Map.of(EntityType.DATA.toString(), new DatatypeParameter(MODEL_SAMPLE, CONFIG_STORE_PATH)));
        // call to lta storage setting
        Mockito.when(settingService.getStorageConfig(any())).thenReturn(LTA_STORAGE);

        // ---- WHEN ----
        // main method is called (submission request dtos are sent to save submission requests)
        List<SubmissionResponseDto> dtoResponses = createService.handleSubmissionRequestsCreation(List.of(
            requestDtoWithSimpleStorePath,
            requestDtoWithProperties,
            requestDtoWithReplace));

        // ---- THEN ----
        // - Check that requests have been correctly built -
        List<SubmissionRequest> requestsCreated = refRequestsCreated.get();
        Assertions.assertThat(requestsCreated).hasSize(3);

        OffsetDateTime now = OffsetDateTime.now();
        Path expectedBuiltPath = Paths.get(String.format("/%d/%d/%d/value1/nestedValue1/nestedValue2/pathExample",
                                                         now.getYear(),
                                                         now.getMonthValue(),
                                                         now.getDayOfMonth()));

        checkCreatedRequests(requestDtoWithSimpleStorePath,
                             requestDtoWithProperties,
                             requestDtoWithReplace,
                             requestsCreated,
                             expectedBuiltPath);

        // - Check all request are in validated status -
        Assertions.assertThat(Set.of(SubmissionRequestState.VALIDATED))
                  .isEqualTo(requestsCreated.stream().map(SubmissionRequest::getStatus).collect(Collectors.toSet()));
        // - Check dto answers -
        Assertions.assertThat(dtoResponses).hasSize(3);

        String tenant = tenantResolver.getTenant();

        // - Expected worker requests -
        // worker request with store path
        Path configStorePath = Paths.get(CONFIG_STORE_PATH);
        LtaWorkerRequestDtoEvent expectedReqWithPath = createExpectedRequest(requestDtoWithSimpleStorePath,
                                                                             requestsCreated.get(0),
                                                                             configStorePath,
                                                                             tenant,
                                                                             false);
        // worker request without store path
        LtaWorkerRequestDtoEvent expectedReqWithoutPath = createExpectedRequest(requestDtoWithProperties,
                                                                                requestsCreated.get(1),
                                                                                expectedBuiltPath,
                                                                                tenant,
                                                                                false);

        // worker request with replace mode
        LtaWorkerRequestDtoEvent expectedReqWithReplace = createExpectedRequest(requestDtoWithReplace,
                                                                                requestsCreated.get(2),
                                                                                configStorePath,
                                                                                tenant,
                                                                                true);

        // - Expected worker requests -
        List<LtaWorkerRequestDtoEvent> expectedWorkerReq = new ArrayList<>();
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
        // - SubmissionRequestDtos (product requests to send)
        SubmissionRequestDto requestDtoWithSimpleStorePath = createRequestWithSimpleStorePath();
        requestDtoWithSimpleStorePath.setStorePath(null);

        SubmissionRequestDto requestDtoWithProperties = createRequestWithProperties();
        requestDtoWithProperties.setProperties(new GsonBuilder().create().fromJson("""
                                                                                       {
                                                                                         "key1": "value1",
                                                                                         "subProperties":  {
                                                                                            "subKey1": "nestedValue1"
                                                                                         }
                                                                                       }
                                                                                       """, Map.class));
        // - Mocks -
        // call to lta settings
        Mockito.when(settingService.getDatypesConfig(any()))
               .thenReturn(Map.of(EntityType.DATA.toString(),
                                  new DatatypeParameter("modelSample",
                                                        "/${YEAR}/${MONTH}/${DAY}/${PROPERTY(key1)}/${PROPERTY"
                                                        + "(subProperties.subKey1)}/${PROPERTY(subProperties.subKey2)}/pathExample")));

        // ---- WHEN ----
        // main method is called (submission request dtos are sent to save submission requests)
        List<SubmissionResponseDto> dtoResponses = createService.handleSubmissionRequestsCreation(List.of(
            requestDtoWithSimpleStorePath,
            requestDtoWithProperties));

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

    private SubmissionRequestDto createRequestWithSimpleStorePath() {
        SubmissionRequestDto requestDtoWithSimpleStorePath = new SubmissionRequestDto("test simple path",
                                                                                      UUID.randomUUID().toString(),
                                                                                      EntityType.DATA.toString(),
                                                                                      IGeometry.point(IGeometry.position(
                                                                                          10.0,
                                                                                          20.0)),
                                                                                      getProductFileDtos(),
                                                                                      null,
                                                                                      "URN:AIP:DATA:example:12345678-9abc-def0-1234-56789abcdef0:V1",
                                                                                      null,
                                                                                      SIMPLE_STORE_PATH,
                                                                                      "sessionSimpleStore",
                                                                                      false);
        requestDtoWithSimpleStorePath.setOwner(OWNER);
        return requestDtoWithSimpleStorePath;
    }

    private SubmissionRequestDto createRequestWithProperties() {
        Map<String, Object> properties = new GsonBuilder().create().fromJson("""
                                                                                 {
                                                                                   "key1": "value1",
                                                                                   "subProperties":  {
                                                                                      "subKey1": "nestedValue1",
                                                                                      "subKey2": "nestedValue2"
                                                                                   }
                                                                                 }
                                                                                 """, Map.class);
        SubmissionRequestDto requestDtoWithProperties = new SubmissionRequestDto("test with properties",
                                                                                 UUID.randomUUID().toString(),
                                                                                 EntityType.DATA.toString(),
                                                                                 IGeometry.point(IGeometry.position(10.0,
                                                                                                                    20.0)),
                                                                                 getProductFileDtos(),
                                                                                 null,
                                                                                 "URN:AIP:DATA:example:12345678-9abc-def0-1234-56789abcdef0:V1",
                                                                                 properties,
                                                                                 null,
                                                                                 null,
                                                                                 false);
        requestDtoWithProperties.setOwner(OWNER);
        return requestDtoWithProperties;
    }

    private SubmissionRequestDto createRequestWithReplace() {
        SubmissionRequestDto requestDtoWithReplace = new SubmissionRequestDto("test with replace",
                                                                              UUID.randomUUID().toString(),
                                                                              EntityType.DATA.toString(),
                                                                              IGeometry.point(IGeometry.position(10.0,
                                                                                                                 20.0)),
                                                                              getProductFileDtos(),
                                                                              null,
                                                                              "URN:AIP:DATA:example:12345678-9abc-def0-1234-56789abcdef0:V1",
                                                                              null,
                                                                              SIMPLE_STORE_PATH,
                                                                              null,
                                                                              true);
        requestDtoWithReplace.setOwner(OWNER);

        return requestDtoWithReplace;
    }

    private static List<ProductFileDto> getProductFileDtos() {
        // - SubmissionRequestDtos (product requests to send) -
        return List.of(new ProductFileDto(LtaDataType.RAWDATA,
                                          "http://localhost/notexisting",
                                          "example.raw",
                                          "f016852239a8a919f05f6d225c5aaca",
                                          MediaType.APPLICATION_OCTET_STREAM));
    }

    private static void checkCreatedRequests(SubmissionRequestDto requestDtoWithSimpleStorePath,
                                             SubmissionRequestDto requestDtoWithProperties,
                                             SubmissionRequestDto requestDtoWithReplace,
                                             List<SubmissionRequest> requestsCreated,
                                             Path expectedBuiltPath) {
        for (SubmissionRequest requestCreated : requestsCreated) {
            String id = requestCreated.getProduct().getProductId();
            if (id.equals(requestDtoWithSimpleStorePath.getProductId())) {
                Assertions.assertThat(requestCreated.getStorePath()).isEqualTo(Paths.get(SIMPLE_STORE_PATH));
                Assertions.assertThat(requestCreated.getSession()).isEqualTo("sessionSimpleStore");
            } else if (id.equals(requestDtoWithProperties.getProductId())) {
                Assertions.assertThat(requestCreated.getStorePath()).isEqualTo(expectedBuiltPath);
                Assertions.assertThat(requestCreated.getSession())
                          .isEqualTo("user-" + OffsetDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE));
            } else if (id.equals(requestDtoWithReplace.getProductId())) {
                Assertions.assertThat(requestCreated.isReplaceMode()).isTrue();
            }
        }
    }

    private static LtaWorkerRequestDtoEvent createExpectedRequest(SubmissionRequestDto requestDtoWithSimpleStorePath,
                                                                  SubmissionRequest reqCreatedWithStorePath,
                                                                  Path storePath,
                                                                  String tenant,
                                                                  boolean replace) {
        LtaWorkerRequestDtoEvent expectedReqWithPath = new LtaWorkerRequestDtoEvent(LTA_STORAGE,
                                                                                    storePath,
                                                                                    MODEL_SAMPLE,
                                                                                    requestDtoWithSimpleStorePath,
                                                                                    replace);
        expectedReqWithPath.setWorkerHeaders(LTA_CONTENT_TYPE,
                                             tenant,
                                             reqCreatedWithStorePath.getCorrelationId(),
                                             reqCreatedWithStorePath.getOwner(),
                                             reqCreatedWithStorePath.getSession());
        return expectedReqWithPath;
    }

    @After
    public void closeMocks() throws Exception {
        closable.close();
    }

}
