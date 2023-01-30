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
package fr.cnes.regards.modules.ltamanager.rest.submission;

import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ltamanager.amqp.output.LtaWorkerRequestDtoEvent;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.settings.DatatypeParameter;
import fr.cnes.regards.modules.ltamanager.domain.settings.LtaSettings;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.ProductFileDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseStatus;
import fr.cnes.regards.modules.ltamanager.service.submission.creation.SubmissionCreateService;
import fr.cnes.regards.modules.model.client.IModelClient;
import fr.cnes.regards.modules.model.domain.Model;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static fr.cnes.regards.modules.ltamanager.service.submission.creation.SubmissionCreateService.LTA_CONTENT_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Test for {@link SubmissionCreateController}
 */
@TestPropertySource(locations = { "classpath:application-test.properties" },
    properties = { "spring.jpa.properties.hibernate.default_schema=submission_controller_it" })
public class SubmissionCreateControllerIT extends AbstractRegardsIT {

    private static final String DATATYPE = "DatatypeTest";

    private static final String MODEL = "modelSample";

    private static final String STORE_PATH_CONFIG = "/storePathExample";

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private ISubmissionRequestRepository requestRepository;

    @Autowired
    protected IDynamicTenantSettingService settingService;

    @MockBean
    private IModelClient modelClient;

    @SpyBean
    private SubmissionCreateService createService;

    @SpyBean
    private IPublisher publisher;

    @Autowired
    private Gson gson;

    @Before
    public void init() throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        tenantResolver.forceTenant(getDefaultTenant());
        clearContext();
        initConfiguration();
    }

    private void clearContext() {
        requestRepository.deleteAll();
        Mockito.reset(publisher);
    }

    private SubmissionRequestDto initInputSubmissionRequest() {
        InputStream inputStream = SubmissionCreateController.class.getClassLoader()
                                                                  .getResourceAsStream("inputs/INPUT_SAMPLE.json");
        assert inputStream != null;
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        return gson.fromJson(reader, SubmissionRequestDto.class);
    }

    private void initConfiguration()
        throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        String modelName = "modelSample";
        Model model = Model.build(modelName, "", EntityType.DATA);
        model.setId(1L);
        Mockito.when(modelClient.getModel(modelName)).thenReturn(ResponseEntity.ok(EntityModel.of(model)));
        // init settings to prevent errors during the validation of SubmissionRequestDto
        settingService.update(LtaSettings.DATATYPES_KEY,
                              Map.of(DATATYPE, new DatatypeParameter(MODEL, STORE_PATH_CONFIG)));
    }

    @Test
    @Purpose("Check a submission request is correctly created after having received a product creation request")
    public void create_submission_requests_in_success() throws Exception {
        // GIVEN
        SubmissionRequestDto submissionRequestDto = initInputSubmissionRequest();

        // WHEN
        // send post request with dto content to create submission request
        RequestBuilderCustomizer requestBuilderCustomizer = customizer();
        requestBuilderCustomizer.expectStatus(HttpStatus.CREATED);

        ResultActions response = performDefaultPost(AbstractSubmissionController.ROOT_PATH,
                                                    submissionRequestDto,
                                                    requestBuilderCustomizer,
                                                    "Error creating request dto");

        // THEN
        List<SubmissionRequest> requestsSaved = requestRepository.findAll();
        // check a request has been created following the dto request receiving
        Assert.assertEquals(1, requestsSaved.size());
        // check response of the post request is correctly formed
        SubmissionRequest requestSaved = requestsSaved.get(0);
        response.andExpect(MockMvcResultMatchers.jsonPath("$.content").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.length()", Matchers.is(5)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.correlationId",
                                                          Matchers.equalTo(requestSaved.getCorrelationId())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.expires",
                                                          Matchers.equalTo(OffsetDateTimeAdapter.format(requestSaved.getCreationDate()
                                                                                                                    .plusHours(
                                                                                                                        LtaSettings.DEFAULT_SUCCESS_EXPIRATION_HOURS)))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.responseStatus",
                                                          Matchers.equalTo(SubmissionResponseStatus.GRANTED.toString())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.session",
                                                          Matchers.equalTo(String.format("%s-%s",
                                                                                         "default_user",
                                                                                         OffsetDateTime.now()
                                                                                                       .format(
                                                                                                           DateTimeFormatter.BASIC_ISO_DATE)))));

        // check worker request has been sent
        ArgumentCaptor<List<LtaWorkerRequestDtoEvent>> captorPublished = ArgumentCaptor.forClass(List.class);
        Mockito.verify(publisher).publish(captorPublished.capture(), anyString(), any(Optional.class));
        List<LtaWorkerRequestDtoEvent> capturedPublishedEvents = captorPublished.getValue();
        SubmissionRequestDto productSave = requestSaved.getProduct();
        productSave.setOwner(getDefaultUserEmail()); // set owner because message properties are not persisted
        LtaWorkerRequestDtoEvent expectedReqWithPath = new LtaWorkerRequestDtoEvent(LtaSettings.DEFAULT_STORAGE,
                                                                                    Paths.get(STORE_PATH_CONFIG),
                                                                                    MODEL,
                                                                                    productSave,
                                                                                    false);
        expectedReqWithPath.setWorkerHeaders(LTA_CONTENT_TYPE,
                                             tenantResolver.getTenant(),
                                             requestSaved.getCorrelationId(),
                                             requestSaved.getOwner(),
                                             requestSaved.getSession());
        Assertions.assertThat(capturedPublishedEvents).hasSameElementsAs(List.of(expectedReqWithPath));
    }

    @Test
    @Purpose("Test is a HTTP 422 error is sent back if submission request dtos are not formed as expected")
    public void create_submission_requests_in_error_422_malformed_input_requests() {
        // GIVEN
        // submissionRequestDto (product request to send)
        // put not valid url and checksum to trigger the error
        List<ProductFileDto> files = List.of(new ProductFileDto(DataType.RAWDATA,
                                                                "not valid url!",
                                                                "example.raw",
                                                                "f016852239a8a919f05f6d2225c5aac",
                                                                MediaType.APPLICATION_OCTET_STREAM));
        SubmissionRequestDto submissionRequestDtoMalformed = new SubmissionRequestDto("Test RequestDto",
                                                                                      UUID.randomUUID().toString(),
                                                                                      EntityType.DATA.toString(),
                                                                                      IGeometry.point(IGeometry.position(
                                                                                          10.0,
                                                                                          20.0)),
                                                                                      files);

        // WHEN
        // send post request with dto content to create submission request
        RequestBuilderCustomizer requestBuilderCustomizer = customizer();
        requestBuilderCustomizer.expectStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        requestBuilderCustomizer.expectToHaveSize("$.messages", 2);

        performDefaultPost(AbstractSubmissionController.ROOT_PATH,
                           submissionRequestDtoMalformed,
                           requestBuilderCustomizer,
                           "Error creating request dto");

        // THEN
        // request should be in error (see expect in requestBuilderCustomizer)
    }

    @Test
    @Purpose("Check a submission request is denied if it contains a correlationId that already exists.")
    public void create_submission_requests_in_error_already_exists() throws Exception {
        // GIVEN
        // send post request with dto content to create submission request
        performDefaultPost(AbstractSubmissionController.ROOT_PATH,
                           initInputSubmissionRequest(),
                           customizer().expectStatus(HttpStatus.CREATED),
                           "Error creating request dto");

        // WHEN
        // send back the same request. It should be rejected because the correlationId is the same.
        performDefaultPost(AbstractSubmissionController.ROOT_PATH,
                           initInputSubmissionRequest(),
                           customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY),
                           "Error request was not denied!");
    }

}
