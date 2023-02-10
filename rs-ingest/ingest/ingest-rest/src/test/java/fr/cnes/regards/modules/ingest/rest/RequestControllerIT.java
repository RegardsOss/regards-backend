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
package fr.cnes.regards.modules.ingest.rest;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdatesCreatorRepository;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdatesCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.sip.VersioningMode;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.*;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.test.IngestServiceIT;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * {@link RequestDto} REST API testing
 *
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=request_controller_it",
                                   "regards.aips.save-metadata.bulk.delay=100",
                                   "regards.ingest.aip.delete.bulk.delay=100" })
@ContextConfiguration(classes = { AIPControllerIT.Config.class })
@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)
public class RequestControllerIT extends AbstractRegardsTransactionalIT {

    @Configuration
    @EnableScheduling
    static class Config {

    }

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IngestServiceIT ingestServiceTest;

    @Autowired
    private IAIPUpdatesCreatorRepository aipUpdatesCreatorRepository;

    @Before
    public void init() throws Exception {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Clean everything
        ingestServiceTest.init();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    @Test
    public void searchRequests() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();

        List<String> CATEGORIES_0 = Lists.newArrayList("CATEGORY", "CATEGORY00", "CATEGORY01");
        List<String> CATEGORIES_2 = Lists.newArrayList("CATEGORY2");

        List<String> TAG_2 = Lists.newArrayList("plop", "ping");
        List<String> TAG_3 = Lists.newArrayList("toto");

        String STORAGE_3 = "Pentagon";

        String SESSION_OWNER_0 = "NASA";

        String SESSION_0 = OffsetDateTime.now().toString();

        for (int i = 0; i < 1000; i = i + 1) {
            AIPUpdatesCreatorRequest someRequest = AIPUpdatesCreatorRequest.build(AIPUpdateParametersDto.build(
                SearchAIPsParameters.build().withSession(SESSION_0).withSessionOwner(SESSION_OWNER_0),
                TAG_2,
                TAG_3,
                CATEGORIES_2,
                CATEGORIES_0,
                Lists.newArrayList(STORAGE_3)));
            aipUpdatesCreatorRepository.save(someRequest);
        }

        // Add request parameters documentation
        requestBuilderCustomizer.documentRequestBody(getSearchBodyDescriptors());
        // Add response body documentation
        requestBuilderCustomizer.documentResponseBody(documentResultingRequest());

        performDefaultPost(RequestController.TYPE_MAPPING,
                           new SearchAbstractRequestParameters(),
                           requestBuilderCustomizer,
                           "Should retrieve Request");
    }

    @Test
    public void chooseVersioning() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusNoContent();
        ChooseVersioningRequestParameters body = ChooseVersioningRequestParameters.build()
                                                                                  .withNewVersioningMode(VersioningMode.INC_VERSION);

        // Add request parameters documentation
        requestBuilderCustomizer.documentRequestBody(getVersioningBodyDescriptors());

        performDefaultPut(RequestController.TYPE_MAPPING + RequestController.VERSIONING_CHOICE_PATH,
                          body,
                          requestBuilderCustomizer,
                          "Should schedule a job to update versioning mode of manual requests requests");
    }

    @Test
    public void chooseVersioning422() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        ChooseVersioningRequestParameters body = ChooseVersioningRequestParameters.build()
                                                                                  .withNewVersioningMode(VersioningMode.MANUAL);

        // Add request parameters documentation
        requestBuilderCustomizer.documentRequestBody(getVersioningBodyDescriptors());

        performDefaultPut(RequestController.TYPE_MAPPING + RequestController.VERSIONING_CHOICE_PATH,
                          body,
                          requestBuilderCustomizer,
                          "Should be refused as manual is the mode we are replacing requests");
    }

    private List<FieldDescriptor> getVersioningBodyDescriptors() {
        List<FieldDescriptor> params = getSearchBodyDescriptors();
        ConstrainedFields constrainedFields = new ConstrainedFields(ChooseVersioningRequestParameters.class);
        params.add(constrainedFields.withPath("newVersioningMode", "newVersioningMode", "new versioning mode to apply")
                                    .type(JSON_STRING_TYPE)
                                    .optional()
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(JSON_STRING_TYPE))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional. Multiple values allowed. Allowed values : "
                                                                 + Arrays.stream(VersioningMode.values())
                                                                         .map(mode -> mode.toString())
                                                                         .collect(Collectors.joining(", ")))));
        return params;
    }

    @Test
    public void retryRequests() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        SearchAbstractRequestParameters body = new SearchAbstractRequestParameters();

        // Add request parameters documentation
        requestBuilderCustomizer.documentRequestBody(getSearchBodyDescriptors());

        performDefaultPost(RequestController.TYPE_MAPPING + RequestController.REQUEST_RETRY_PATH,
                           body,
                           requestBuilderCustomizer,
                           "Should schedule a job to retry requests");
    }

    @Test
    public void deleteRequests() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        SearchAbstractRequestParameters body = new SearchAbstractRequestParameters();

        // Add request parameters documentation
        requestBuilderCustomizer.documentRequestBody(getSearchBodyDescriptors());

        performDefaultPost(RequestController.TYPE_MAPPING + RequestController.REQUEST_DELETE_PATH,
                           body,
                           requestBuilderCustomizer,
                           "Should schedule a job to delete requests");
    }

    private List<FieldDescriptor> getSearchBodyDescriptors() {

        List<FieldDescriptor> params = new ArrayList<>();

        StringJoiner queryTypeValues = new StringJoiner(", ");
        for (RequestTypeEnum state : RequestTypeEnum.values()) {
            queryTypeValues.add(state.name());
        }

        StringJoiner stateValues = new StringJoiner(", ");
        for (InternalRequestState state : InternalRequestState.values()) {
            stateValues.add(state.name());
        }

        ConstrainedFields constrainedFields = new ConstrainedFields(SearchRequestsParameters.class);

        params.add(constrainedFields.withPath("requestType", "requestType", "Request type filter")
                                    .type(JSON_STRING_TYPE)
                                    .optional()
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(JSON_STRING_TYPE))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional. Multiple values allowed. Allowed values : "
                                                                 + queryTypeValues.toString())));

        params.add(constrainedFields.withPath("state", "state", "State")
                                    .type(JSON_STRING_TYPE)
                                    .optional()
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(JSON_STRING_TYPE))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional. Multiple values allowed. Allowed values : "
                                                                 + stateValues.toString())));

        params.add(constrainedFields.withPath("stateExcluded", "stateExcluded", "State excluded (ignored)")
                                    .type(JSON_STRING_TYPE)
                                    .optional()
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(JSON_STRING_TYPE))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional. Multiple values allowed. Allowed values : "
                                                                 + stateValues.toString())));

        params.add(constrainedFields.withPath("creationDate.from", "from", "ISO Date time filtering on creation date")
                                    .optional()
                                    .type(JSON_STRING_TYPE)
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(OffsetDateTime.class.getSimpleName()))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value(
                                                              "Optional. Required format : yyyy-MM-dd'T'HH:mm:ss.SSSZ")));

        params.add(constrainedFields.withPath("creationDate.to", "to", "ISO Date time filtering on creation date")
                                    .optional()
                                    .type(JSON_STRING_TYPE)
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(OffsetDateTime.class.getSimpleName()))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value(
                                                              "Optional. Required format : yyyy-MM-dd'T'HH:mm:ss.SSSZ")));

        params.add(constrainedFields.withPath("providerId", "providerId", "Provider id filter")
                                    .optional()
                                    .type(JSON_STRING_TYPE)
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(JSON_STRING_TYPE))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value(
                                                              "Optional. If you add the % character, we will use the like operator to match entities")));

        params.add(constrainedFields.withPath("sessionOwner", "sessionOwner", "Session owner filter")
                                    .optional()
                                    .type(JSON_STRING_TYPE)
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(JSON_STRING_TYPE))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional.")));

        params.add(constrainedFields.withPath("session", "session", "Session filter")
                                    .optional()
                                    .type(JSON_STRING_TYPE)
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(JSON_STRING_TYPE))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional.")));
        return params;
    }

    private List<FieldDescriptor> documentResultingRequest() {

        ConstrainedFields constrainedFields = new ConstrainedFields(RequestDto.class);
        List<FieldDescriptor> fields = new ArrayList<>();

        String prefix = "content[].content.";

        StringJoiner queryTypeValues = new StringJoiner(", ");
        for (RequestTypeEnum state : RequestTypeEnum.values()) {
            queryTypeValues.add(state.name());
        }

        StringJoiner stateValues = new StringJoiner(", ");
        for (InternalRequestState state : InternalRequestState.values()) {
            stateValues.add(state.name());
        }

        fields.add(constrainedFields.withPath(prefix + "id", "id", "Request id"));

        fields.add(constrainedFields.withPath(prefix + "creationDate", "creationDate", "Date of Request creation")
                                    .type(OffsetDateTime.class.getSimpleName()));

        fields.add(constrainedFields.withPath(prefix + "remoteStepDeadline",
                                              "remoteStepDeadline",
                                              "Date of Request timeout")
                                    .type(OffsetDateTime.class.getSimpleName())
                                    .optional());

        fields.add(constrainedFields.withPath(prefix + "state",
                                              "state",
                                              "Request state. Allowed values : " + stateValues.toString())
                                    .type(JSON_STRING_TYPE)
                                    .optional());

        fields.add(constrainedFields.withPath(prefix + "dtype",
                                              "dtype",
                                              "Request type. Allowed values : " + queryTypeValues.toString())
                                    .type(JSON_STRING_TYPE)
                                    .optional());

        fields.add(constrainedFields.withPath(prefix + "sessionOwner", "sessionOwner", "Session owner")
                                    .type(JSON_STRING_TYPE)
                                    .optional());

        fields.add(constrainedFields.withPath(prefix + "session", "session", "Session")
                                    .type(JSON_STRING_TYPE)
                                    .optional());

        fields.add(constrainedFields.withPath(prefix + "providerId", "providerId", "Provider id")
                                    .type(JSON_STRING_TYPE)
                                    .optional());

        fields.add(constrainedFields.withPath(prefix + "errors", "errors", "List of errors associated to the request")
                                    .type(JSON_ARRAY_TYPE)
                                    .optional());

        return fields;
    }

    @After
    public void doAfter() {
        ingestServiceTest.init();
    }

}