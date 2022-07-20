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
package fr.cnes.regards.modules.feature.documentation;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.SearchSelectionMode;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPagedModel;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Sébastien Binda
 */
public class RequestsControllerDocumentationHelper {

    public static ParameterDescriptor featureRequestTypeEnumDoc(String name) {
        ParameterDescriptor param = RequestDocumentation.parameterWithName(name)
                                                        .description("Request type")
                                                        .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                              .value("String"),
                                                                    Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                              .value(Arrays.toString(
                                                                                  FeatureRequestTypeEnum.values())));
        return param;
    }

    public static List<ParameterDescriptor> paginationDoc() {
        List<ParameterDescriptor> params = Lists.newArrayList();
        params.add(RequestDocumentation.parameterWithName("page")
                                       .optional()
                                       .description("Number of the page to retrieve")
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                             .value("Integer")));
        params.add(RequestDocumentation.parameterWithName("size")
                                       .optional()
                                       .description("Number of elements by page")
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                             .value("Integer")));
        return params;
    }

    public static List<FieldDescriptor> featureRequestsSelectionDTODoc() {
        ConstrainedFields fields = new ConstrainedFields(FeatureRequestsSelectionDTO.class);
        List<FieldDescriptor> fd = new ArrayList<FieldDescriptor>();
        fd.add(fields.withPath("filters.source", "Source of the request").type("String").optional());
        fd.add(fields.withPath("filters.session", "Session of the request").type("String").optional());
        fd.add(fields.withPath("filters.providerId", "ProviderId of the associated feature").type("String").optional());
        fd.add(fields.withPath("filters.from", "Search for requests with registrationDate greather than this parameter")
                     .type("Date ISO-8601")
                     .optional());
        fd.add(fields.withPath("filters.to", "Search for requests with registrationDate lower than this parameter")
                     .type("Date ISO-8601")
                     .optional());
        fd.add(fields.withPath("filters.state", "State of the requests to search for").type("String").optional());
        fd.add(fields.withPath("requestIds",
                               "Array of requests ids to search for or to exclude form search. Depends on requestIdSelectionMode.")
                     .type("String[]")
                     .optional());
        fd.add(fields.withPath("requestIdSelectionMode",
                               "requestIdSelectionMode",
                               "requestIds selection mode. Default value = " + SearchSelectionMode.INCLUDE,
                               Arrays.toString(SearchSelectionMode.values())).type("String").optional());
        return fd;
    }

    public static List<ParameterDescriptor> featureRequestSearchParametersDoc() {
        List<ParameterDescriptor> params = Lists.newArrayList();
        params.add(RequestDocumentation.parameterWithName("source")
                                       .optional()
                                       .description("Source of the requests to search for")
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                             .value("String")));
        params.add(RequestDocumentation.parameterWithName("session")
                                       .optional()
                                       .description("Session of the requests to search for")
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                             .value("String")));
        params.add(RequestDocumentation.parameterWithName("providerId")
                                       .optional()
                                       .description("providerId of the requests to search for")
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                             .value("String")));
        params.add(RequestDocumentation.parameterWithName("from")
                                       .optional()
                                       .description(
                                           "Search for requests with registrationDate greather than this parameter")
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                             .value("Date ISO-8601"))
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                             .value(OffsetDateTime.now().toString())));
        params.add(RequestDocumentation.parameterWithName("to")
                                       .optional()
                                       .description(
                                           "Search for requests with registrationDate lower than this parameter")
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                             .value("Date ISO-8601"))
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                             .value(OffsetDateTime.now().toString())));
        params.add(RequestDocumentation.parameterWithName("state")
                                       .optional()
                                       .description("state of the requests to search for")
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String"),
                                                   Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                             .value(Arrays.toString(RequestState.values()))));
        return params;
    }

    public static List<FieldDescriptor> featureRequestDTOResponseDoc() {
        List<FieldDescriptor> fd = Lists.newArrayList();
        ConstrainedFields fields = new ConstrainedFields(RequestsPagedModel.class);
        fd.add(fields.withPath("metadata", "Pagination information"));
        fd.add(fields.withPath("info.nbErrors",
                               "Total number of requests in ERROR state matching the search parameters")
                     .type("Integer"));
        fd.add(fields.withPath("content", "List of results requests"));
        fd.add(fields.withPath("content[].content", "Request"));
        fd.add(fields.withPath("content[].content.id", "Request unique identifier").type("Long"));
        fd.add(fields.withPath("content[].content.urn", "Associated feature Uniform Resource Name")
                     .type("String")
                     .optional());
        fd.add(fields.withPath("content[].content.providerId", "Associated feature provider identifier")
                     .type("String")
                     .optional());
        fd.add(fields.withPath("content[].content.state",
                               "state",
                               "Associated feature provider identifier",
                               Arrays.toString(RequestState.values())).type("String"));
        fd.add(fields.withPath("content[].content.processing", "Does the request is processing ?").type("Boolean"));
        fd.add(fields.withPath("content[].content.registrationDate",
                               "registrationDate",
                               "Request registration date",
                               OffsetDateTime.now().toString()).type("Date ISO-8601"));
        fd.add(fields.withPath("content[].content.type",
                               "type",
                               "Request type",
                               Arrays.toString(FeatureRequestTypeEnum.values())).type("String"));
        fd.add(fields.withPath("content[].content.source", "Source of the request").type("String").optional());
        fd.add(fields.withPath("content[].content.session", "Session of the request").type("String").optional());
        fd.add(fields.withPath("content[].links", "Hateoas links fot he current request"));
        fd.add(fields.withPath("links", "Hateoas links information about results"));
        return fd;

    }

    /**
     * @return
     */
    public static List<FieldDescriptor> requestHandledResponseDoc() {
        List<FieldDescriptor> fd = Lists.newArrayList();
        ConstrainedFields fields = new ConstrainedFields(RequestHandledResponse.class);
        fd.add(fields.withPath("totalHandled", "Total number of requests handled"));
        fd.add(fields.withPath("totalRequested", "Number of requests requested"));
        fd.add(fields.withPath("message", "Completion message information"));
        return fd;
    }

}
