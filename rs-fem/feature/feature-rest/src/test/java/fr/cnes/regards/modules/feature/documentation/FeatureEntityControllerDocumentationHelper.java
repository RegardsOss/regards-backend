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
import fr.cnes.regards.modules.feature.dto.FeaturesSelectionDTO;
import fr.cnes.regards.modules.feature.dto.SearchSelectionMode;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPagedModel;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Sébastien Binda
 */
public class FeatureEntityControllerDocumentationHelper {

    public static List<FieldDescriptor> featureSelectionDTODoc() {
        ConstrainedFields fields = new ConstrainedFields(FeaturesSelectionDTO.class);
        List<FieldDescriptor> fd = new ArrayList<FieldDescriptor>();
        fd.add(fields.withPath("filters.source", "Source of the feature").type("String").optional());
        fd.add(fields.withPath("filters.session", "Session of the feature").type("String").optional());
        fd.add(fields.withPath("filters.providerId", "ProviderId of the feature").type("String").optional());
        fd.add(fields.withPath("filters.from", "Search for features with lastupdate date greather than this parameter")
                     .type("Date ISO-8601")
                     .optional());
        fd.add(fields.withPath("filters.to", "Search for features with lastupdate date lower than this parameter")
                     .type("Date ISO-8601")
                     .optional());
        fd.add(fields.withPath("filters.model", "Model of the features to search for").type("String").optional());
        fd.add(fields.withPath("featureIds",
                               "Array of feature ids to search for or to exclude form search. Depends on featureIdsSelectionMode.")
                     .type("String[]")
                     .optional());
        fd.add(fields.withPath("featureIdsSelectionMode",
                               "featureIdsSelectionMode",
                               "featureIds selection mode. Default value = " + SearchSelectionMode.INCLUDE,
                               Arrays.toString(SearchSelectionMode.values())).type("String").optional());
        return fd;
    }

    public static List<ParameterDescriptor> featuresSearchParametersDoc() {
        List<ParameterDescriptor> params = Lists.newArrayList();
        params.add(RequestDocumentation.parameterWithName("source")
                                       .optional()
                                       .description("Source of the features to search for")
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                             .value("String")));
        params.add(RequestDocumentation.parameterWithName("session")
                                       .optional()
                                       .description("Session of the features to search for")
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                             .value("String")));
        params.add(RequestDocumentation.parameterWithName("providerId")
                                       .optional()
                                       .description("providerId of the features to search for")
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                             .value("String")));
        params.add(RequestDocumentation.parameterWithName("from")
                                       .optional()
                                       .description(
                                           "Search for features with lastUpdate date greather than this parameter")
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                             .value("Date ISO-8601"))
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                             .value(OffsetDateTime.now().toString())));
        params.add(RequestDocumentation.parameterWithName("to")
                                       .optional()
                                       .description("Search for features with lastUpdate date lower than this parameter")
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                             .value("Date ISO-8601"))
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                             .value(OffsetDateTime.now().toString())));
        params.add(RequestDocumentation.parameterWithName("model")
                                       .optional()
                                       .description("model of the features to search for")
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                             .value("String")));
        params.add(RequestDocumentation.parameterWithName("full")
                                       .optional()
                                       .description(
                                           "Returned feature are complet (with all geojson content) if true. Default true.")
                                       .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                             .value("Boolean")));
        return params;
    }

    public static List<FieldDescriptor> featureEntityDTOPageResponseDoc() {
        List<FieldDescriptor> fd = Lists.newArrayList();
        ConstrainedFields fields = new ConstrainedFields(RequestsPagedModel.class);
        fd.add(fields.withPath("metadata", "Pagination information"));
        fd.add(fields.withPath("content", "Current page of features"));
        fd.addAll(featureEntityDTOResponseDoc(Optional.of("content[].content.")));
        fd.add(fields.withPath("content[].links", "Hateoas links fot he current feature"));
        fd.add(fields.withPath("links", "Hateoas links information about results"));
        return fd;

    }

    public static List<FieldDescriptor> featureEntityDTOResponseDoc(Optional<String> prefix) {
        List<FieldDescriptor> fd = Lists.newArrayList();
        ConstrainedFields fields = new ConstrainedFields(RequestsPagedModel.class);
        fd.add(fields.withPath(prefix.orElse("") + "id", "Feature identifier").type("Long"));
        fd.add(fields.withPath(prefix.orElse("") + "urn", "Feature Uniforme Resource Name").type("String"));
        fd.add(fields.withPath(prefix.orElse("") + "providerId", "Feature provider identifier").type("String"));
        fd.add(fields.withPath(prefix.orElse("") + "source", "Source of the feature").type("Boolean"));
        fd.add(fields.withPath(prefix.orElse("") + "session", "Acquisition session name of the feature")
                     .type("String"));
        fd.add(fields.withPath(prefix.orElse("") + "version", "Feature version").type("Integer"));
        fd.add(fields.withPath(prefix.orElse("") + "lastUpdate",
                               "lastUpdate",
                               "Last update date of the feature",
                               OffsetDateTime.now().toString()).type("Date ISO-8601"));
        fd.add(fields.withPath(prefix.orElse("") + "feature", "Feature content geojson format").type("Geojson"));
        return fd;

    }

}
