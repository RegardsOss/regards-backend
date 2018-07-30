/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.Sets;
import com.google.gson.Gson;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.entities.domain.StaticProperties;
import fr.cnes.regards.modules.entities.domain.attribute.AbstractAttribute;
import fr.cnes.regards.modules.entities.domain.attribute.DateAttribute;
import fr.cnes.regards.modules.entities.domain.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.opensearch.service.parser.QueryParser;
import fr.cnes.regards.modules.search.domain.plugin.ISearchEngine;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.rest.SearchEngineController;
import fr.cnes.regards.modules.search.rest.engine.plugin.legacy.LegacySearchEngine;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.description.DescriptionBuilder;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.exception.ExtensionException;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.exception.UnsupportedMediaTypesException;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.SearchParameter;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.geo.GeoTimeExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.media.MediaExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.regards.RegardsExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.formatter.IResponseBuilder;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.formatter.atom.AtomResponseBuilder;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.formatter.geojson.GeojsonResponseBuilder;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.service.IBusinessSearchService;

/**
 * OpenSearch engine plugin
 * @author Marc Sordi
 * @author Sébastien Binda
 */
@Plugin(id = OpenSearchEngine.ENGINE_ID, author = "REGARDS Team", contact = "regards@c-s.fr",
        description = "Native search engine", licence = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss",
        version = "1.0.0")
public class OpenSearchEngine implements ISearchEngine<Object, OpenSearchDescription, Object, List<String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchEngine.class);

    public static final String ENGINE_ID = "opensearch";

    public static final String TIME_EXTENSION_PARAMETER = "timeExtension";

    public static final String REGARDS_EXTENSION_PARAMETER = "regardsExtension";

    public static final String MEDIA_EXTENSION_PARAMETER = "mediaExtension";

    public static final String EXTRA_DESCRIPTION = "opensearchDescription.xml";

    public static final String PARAMETERS_CONFIGURATION = "parametersConfiguration";

    public static final String ENGINE_PARAMETERS = "engineConfiguration";

    /**
     * Query parser
     */
    @Autowired
    protected IAttributeFinder finder;

    @Autowired
    private DescriptionBuilder descriptionBuilder;

    /**
     * Business search service
     */
    @Autowired
    protected IBusinessSearchService searchService;

    @Autowired
    private Gson gson;

    @Autowired
    private Configuration configuration;

    @PluginParameter(name = ENGINE_PARAMETERS, label = "Search engine global configuration")
    private EngineConfiguration engineConfiguration;

    /**
     * To build resource links
     */
    @Autowired
    private IResourceService resourceService;

    @PluginParameter(name = TIME_EXTENSION_PARAMETER, label = "Open search time extension")
    private GeoTimeExtension timeExtension;

    @PluginParameter(name = REGARDS_EXTENSION_PARAMETER, label = "Open search regards extension")
    private RegardsExtension regardsExtension;

    @PluginParameter(name = MEDIA_EXTENSION_PARAMETER, label = "Open search media extension")
    private MediaExtension mediaExtension;

    @PluginParameter(name = PARAMETERS_CONFIGURATION, label = "Parameters configuration")
    private final List<ParameterConfiguration> paramConfigurations = Lists.newArrayList();

    @Override
    public boolean supports(SearchType searchType) {
        return true;
    }

    @Override
    public ResponseEntity<Object> search(SearchContext context, ISearchEngine<?, ?, ?, ?> parser)
            throws ModuleException {
        FacetPage<EntityFeature> facetPage = searchService.search(parser.parse(context), context.getSearchType(), null,
                                                                  context.getPageable());
        try {
            return ResponseEntity.ok(formatResponse(facetPage, context));
        } catch (UnsupportedMediaTypesException e) {
            throw new ModuleException(e);
        }
    }

    @Override
    public ResponseEntity<Object> getEntity(SearchContext context) throws ModuleException {
        // Retrieve entity
        EntityFeature entity = searchService.get(context.getUrn().get());
        FacetPage<EntityFeature> facetPage = new FacetPage<>(Arrays.asList(entity), Sets.newHashSet(),
                context.getPageable(), 1);
        try {
            return ResponseEntity.ok(formatResponse(facetPage, context));
        } catch (UnsupportedMediaTypesException e) {
            throw new ModuleException(e);
        }
    }

    public ICriterion parse(MultiValueMap<String, String> queryParams) throws ModuleException {
        // First parse q parameter for searchTerms if any.
        QueryParser queryParser = new QueryParser(finder);
        ICriterion searchTermsCriterion = queryParser.parse(queryParams);
        // Then parse all parameters (open search parameters extension)
        try {
            return ICriterion.and(searchTermsCriterion, parseParametersExt(queryParams));
        } catch (ExtensionException e) {
            throw new ModuleException(e);
        }
    }

    @Override
    public ResponseEntity<OpenSearchDescription> extra(SearchContext context) throws ModuleException {
        if (context.getExtra().isPresent() && context.getExtra().get().equals(EXTRA_DESCRIPTION)) {

            // If the descriptor is asked for a specific dataset, first get the dataset.
            // The dataset will be used to set specific metadatas into the descriptor like title, tags, ...
            Optional<EntityFeature> dataset = Optional.empty();
            if (context.getDatasetUrn().isPresent()) {
                // Search dataset entity
                dataset = Optional.of(searchService.get(context.getDatasetUrn().get()));
            }

            return ResponseEntity.ok(descriptionBuilder
                    .build(context, parse(context), Arrays.asList(mediaExtension, regardsExtension, timeExtension),
                           paramConfigurations, engineConfiguration, dataset));
        } else {
            return ISearchEngine.super.extra(context);
        }
    }

    /**
     * Parse request parameters and and add dataset context if necessary
     */
    @Override
    public ICriterion parse(SearchContext context) throws ModuleException {
        // Convert parameters to business criterion
        ICriterion criterion = parse(context.getQueryParams());
        // Manage dataset URN path parameter as criterion
        if (context.getDatasetUrn().isPresent()) {
            criterion = ICriterion
                    .and(criterion,
                         ICriterion.eq(StaticProperties.FEATURE_TAGS_PATH, context.getDatasetUrn().get().toString()));
        }
        return criterion;
    }

    /**
     * Format search response for the given {@link MediaType} in the {@link SearchContext}
     * @param page search response
     * @param context {@link SearchContext} containing MediaType
     * @return formated response
     * @throws UnsupportedMediaTypesException
     */
    private Object formatResponse(FacetPage<EntityFeature> page, SearchContext context)
            throws UnsupportedMediaTypesException {
        IResponseBuilder<?> builder = getBuilder(context);
        builder.addMetadata(UUID.randomUUID().toString(), engineConfiguration,
                            SearchEngineController
                                    .buildExtraLink(resourceService, context, Link.REL_SELF, EXTRA_DESCRIPTION)
                                    .getHref(),
                            context, configuration, page,
                            SearchEngineController.buildPaginationLinks(resourceService, page, context));
        page.getContent().stream()
                .forEach(e -> builder.addEntity(e, getEntityLastUpdateDate(e), paramConfigurations,
                                                SearchEngineController.buildEntityLinks(resourceService, context, e)));
        return builder.build();
    }

    /**
     * Retrieve the last update date of the given entity.
     * @param entity {@link EntityFeature}
     * @return Optional<OffsetDateTime>
     */
    private Optional<OffsetDateTime> getEntityLastUpdateDate(EntityFeature entity) {
        Optional<OffsetDateTime> date = Optional.empty();
        if (engineConfiguration.getEntityLastUpdateDatePropertyPath() != null) {
            AbstractAttribute<?> dateAttribute = entity
                    .getProperty(engineConfiguration.getEntityLastUpdateDatePropertyPath());
            if (dateAttribute instanceof DateAttribute) {
                DateAttribute dateAttr = (DateAttribute) dateAttribute;
                return Optional.ofNullable(dateAttr.getValue());
            }
        }
        return date;
    }

    /**
     * Parse openSearch query to find all parameters from standard open search parameters extension.
     * @param queryParams
     * @return {@link ICriterion}
     * @throws ExtensionException
     */
    private ICriterion parseParametersExt(MultiValueMap<String, String> queryParams) throws ExtensionException {
        // Find AttributeModel for each parameter
        List<SearchParameter> attributes = buildParameters(queryParams);

        return ICriterion.and(timeExtension.buildCriterion(attributes), mediaExtension.buildCriterion(attributes),
                              regardsExtension.buildCriterion(attributes));
    }

    /**
     * Build {@link SearchParameter}s by reading given queryParams.
     * @param queryParams Map key=parameter name value=parameter value.
     * @return {@link SearchParameter}s
     */
    private List<SearchParameter> buildParameters(MultiValueMap<String, String> queryParams) {
        List<SearchParameter> searchParameters = Lists.newArrayList();
        for (Entry<String, List<String>> queryParam : queryParams.entrySet()) {
            try {
                // Do not handle special query parameter (q) here.
                if (!queryParam.getKey().equals(configuration.getQueryParameterName())) {
                    AttributeModel attributeModel = finder.findByName(queryParam.getKey());
                    if (attributeModel.isDynamic()) {
                        attributeModel.buildJsonPath(StaticProperties.FEATURE_PROPERTIES);
                    } else {
                        // Standard static attributes. Not a real attribute. So jsonPath = name;
                        attributeModel.setJsonPath(attributeModel.getName());
                    }
                    // Search configuration if any
                    ParameterConfiguration conf = paramConfigurations.stream()
                            .filter(p -> p.getAttributeModelJsonPath().equals(attributeModel.getJsonPath())).findFirst()
                            .orElse(null);
                    searchParameters
                            .add(new SearchParameter(queryParam.getKey(), attributeModel, conf, queryParam.getValue()));
                }
            } catch (OpenSearchUnknownParameter e) {
                LOGGER.warn("Parameter not found in REGARDS models attributes.");
                // Adding unknown parameters in search parameters in case an IOpenSearchExtension can handle it.
                searchParameters.add(new SearchParameter(queryParam.getKey(), null, null, queryParam.getValue()));
            }
        }
        return searchParameters;
    }

    /**
     * Retrieve a response builder from existing ones matching the {@link MediaType} from the {@link SearchContext}
     * @param context {@link SearchContext}
     * @return {@link IResponseBuilder}
     * @throws UnsupportedMediaTypesException
     */
    private IResponseBuilder<?> getBuilder(SearchContext context) throws UnsupportedMediaTypesException {
        IResponseBuilder<?> responseBuilder;
        if (context.getHeaders().getAccept().contains(MediaType.APPLICATION_ATOM_XML)) {
            responseBuilder = new AtomResponseBuilder(gson);
        } else if (context.getHeaders().getAccept().contains(MediaType.APPLICATION_JSON)) {
            responseBuilder = new GeojsonResponseBuilder();
        } else {
            throw new UnsupportedMediaTypesException(context.getHeaders().getAccept());
        }
        responseBuilder.addExtension(timeExtension);
        responseBuilder.addExtension(mediaExtension);
        responseBuilder.addExtension(regardsExtension);
        return responseBuilder;
    }

    @Override
    public ResponseEntity<List<String>> getPropertyValues(SearchContext context) throws ModuleException {
        // Convert parameters to business criterion considering dataset
        ICriterion criterion = parse(context);
        // Extract optional request parameters
        String partialText = context.getQueryParams().getFirst(LegacySearchEngine.PARTIAL_TEXT);
        // Do business search
        List<String> values = searchService.retrieveEnumeratedPropertyValues(criterion, context.getSearchType(),
                                                                             context.getPropertyName().get(),
                                                                             context.getMaxCount().get(), partialText);
        // Build response
        return ResponseEntity.ok(values);
    }

    @Override
    public ResponseEntity<DocFilesSummary> getSummary(SearchContext context) throws ModuleException {
        // Convert parameters to business criterion considering dataset
        ICriterion criterion = parse(context);
        // Compute summary
        DocFilesSummary summary = searchService.computeDatasetsSummary(criterion, context.getSearchType(),
                                                                       context.getDatasetUrn().orElseGet(null),
                                                                       context.getDateTypes().get());
        // Build response
        return ResponseEntity.ok(summary);
    }

}
