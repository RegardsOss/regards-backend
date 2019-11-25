/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.RequestDto;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeEnum;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestsParameters;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.test.IngestServiceTest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * {@link RequestDto} REST API testing
 *
 * @author Léo Mieulet
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=request_controller_it" })
@ContextConfiguration(classes = { AIPControllerIT.Config.class })
public class RequestControllerIT extends AbstractRegardsTransactionalIT {
    @Configuration
    @EnableScheduling
    static class Config {
    }
    @Autowired
    private ApplicationEventPublisher springPublisher;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IngestServiceTest ingestServiceTest;

    @Autowired
    private IAIPUpdatesCreatorRepository aipUpdatesCreatorRepository;

    @Autowired
    private IAIPService aipService;

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

        String session = "Session 1";
        String sessionOwner = "# Session owner 25";
        SearchRequestsParameters body = SearchRequestsParameters.build();
        for (int i = 0; i < 1000; i = i + 1) {
            AIPUpdatesCreatorRequest someRequest = AIPUpdatesCreatorRequest.build(AIPUpdateParametersDto.build(
                    SearchAIPsParameters.build().withSession(SESSION_0).withSessionOwner(SESSION_OWNER_0),
                    TAG_2, TAG_3, CATEGORIES_2, CATEGORIES_0, Lists.newArrayList(STORAGE_3)
            ));
            aipUpdatesCreatorRepository.save(someRequest);
        }

        // Add request parameters documentation
        requestBuilderCustomizer.documentRequestBody(getSearchBodyDescriptors());
        // Add response body documentation
        requestBuilderCustomizer.documentResponseBody(documentResultingRequest());

        performDefaultPost(RequestController.TYPE_MAPPING, body, requestBuilderCustomizer,
                "Should retrieve Request");
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


        params.add(constrainedFields.withPath("requestType", "requestType",
                "Request type filter")
                .type(JSON_STRING_TYPE)
                .optional().attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                        .value("Optional. Multiple values allowed. Allowed values : " + queryTypeValues.toString())));

        params.add(constrainedFields.withPath("state", "state",
                "State")
                .type(JSON_STRING_TYPE)
                .optional().attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                        .value("Optional. Multiple values allowed. Allowed values : " + stateValues.toString())));

        params.add(constrainedFields.withPath("creationDate.from", "from",
                "ISO Date time filtering on creation date")
                .optional()
                .type(JSON_STRING_TYPE)
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                        .value(OffsetDateTime.class.getSimpleName()))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                        .value("Optional. Required format : yyyy-MM-dd'T'HH:mm:ss.SSSZ")));

        params.add(constrainedFields.withPath("creationDate.to", "to",
                "ISO Date time filtering on creation date")
                .optional()
                .type(JSON_STRING_TYPE)
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                        .value(OffsetDateTime.class.getSimpleName()))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                        .value("Optional. Required format : yyyy-MM-dd'T'HH:mm:ss.SSSZ")));

        params.add(constrainedFields.withPath("providerId", "providerId",
                "Provider id filter").optional()
                .type(JSON_STRING_TYPE)
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                        .value("Optional. If you add the % character, we will use the like operator to match entities")));


        params.add(constrainedFields.withPath("sessionOwner", "sessionOwner",
                "Session owner filter").optional()
                .type(JSON_STRING_TYPE)
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional.")));


        params.add(constrainedFields.withPath("session","session",
                "Session filter").optional()
                .type(JSON_STRING_TYPE)
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional.")));
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

        fields.add(constrainedFields.withPath(prefix + "remoteStepDeadline", "remoteStepDeadline", "Date of Request timeout")
                .type(OffsetDateTime.class.getSimpleName()).optional());

        fields.add(constrainedFields.withPath(prefix + "state", "state", "Request state. Allowed values : " + stateValues.toString())
                .type(JSON_STRING_TYPE).optional());

        fields.add(constrainedFields.withPath(prefix + "dtype", "dtype", "Request type. Allowed values : " + queryTypeValues.toString())
                .type(JSON_STRING_TYPE).optional());

        fields.add(constrainedFields.withPath(prefix + "sessionOwner", "sessionOwner", "Session owner")
                .type(JSON_STRING_TYPE).optional());

        fields.add(constrainedFields.withPath(prefix + "session", "session", "Session")
                .type(JSON_STRING_TYPE).optional());

        fields.add(constrainedFields.withPath(prefix + "providerId", "providerId", "Provider id")
                .type(JSON_STRING_TYPE).optional());

        fields.add(constrainedFields.withPath(prefix + "errors", "errors", "List of errors associated to the request")
                .type(JSON_ARRAY_TYPE).optional());

        return fields;
    }


}