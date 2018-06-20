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

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.Sets;
import com.google.gson.Gson;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.StaticProperties;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.opensearch.service.IOpenSearchService;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.search.domain.plugin.ISearchEngine;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.rest.SearchEngineController;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.description.OpenSearchDescriptionBuilder;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.exception.UnsupportedMediaTypesException;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.SearchParameter;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.geo.GeoTimeExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.media.MediaExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.regards.RegardsExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.formatter.IOpenSearchResponseBuilder;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.formatter.atom.AtomResponseBuilder;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.formatter.geojson.GeojsonResponseBuilder;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;

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

    private static final String PARAMETERS_CONFIGURATION = "parametersConfiguration";

    /**
     * Query parser
     */
    @Autowired
    protected IAttributeFinder finder;

    /**
     * Query parser
     */
    @Autowired
    private IOpenSearchService openSearchService;

    @Autowired
    private OpenSearchDescriptionBuilder descriptionBuilder;

    /**
     * Business search service
     */
    @Autowired
    protected ICatalogSearchService searchService;

    @Autowired
    private Gson gson;

    @Autowired
    private OpenSearchConfiguration configuration;

    /**
     * To build resource links
     */
    @Autowired
    private IResourceService resourceService;

    @PluginParameter(name = "searchTitle", label = "Title of responses associated to this search engine",
            description = "Search title for response metadatas. Used to construct metadatas for atom+xml and geo+json responses.",
            defaultValue = "Open search engine title")
    private final String searchTitle = "Open search engine title";

    @PluginParameter(name = "searchDescription", label = "Description of responses associated to this search engine",
            description = "Description for response metadatas. Used to construct metadatas for atom+xml and geo+json responses.",
            defaultValue = "Open search engine description")
    private final String searchDescription = "Open search engine description";

    @PluginParameter(name = OpenSearchEngine.TIME_EXTENSION_PARAMETER, label = "Open search time extension")
    private GeoTimeExtension timeExtension;

    @PluginParameter(name = OpenSearchEngine.REGARDS_EXTENSION_PARAMETER, label = "Open search regards extension")
    private RegardsExtension regardsExtension;

    @PluginParameter(name = OpenSearchEngine.MEDIA_EXTENSION_PARAMETER, label = "Open search media extension")
    private MediaExtension mediaExtension;

    @PluginParameter(name = OpenSearchEngine.PARAMETERS_CONFIGURATION, label = "Parameters configuration")
    private final List<OpenSearchParameterConfiguration> paramConfigurations = Lists.newArrayList();

    /**
     * TODO : To remove !!!
     */
    public void init() {
        paramConfigurations.clear();
        OpenSearchParameterConfiguration planetParameter = new OpenSearchParameterConfiguration();
        planetParameter.setAttributeModelJsonPath("properties.planet");
        planetParameter.setName("planet");
        planetParameter.setOptionsEnabled(true);
        planetParameter.setOptionsCardinality(10);
        paramConfigurations.add(planetParameter);

        OpenSearchParameterConfiguration startTimeParameter = new OpenSearchParameterConfiguration();
        startTimeParameter.setAttributeModelJsonPath("properties.TimePeriod.startDate");
        startTimeParameter.setName("start");
        startTimeParameter.setNamespace("time");
        paramConfigurations.add(startTimeParameter);
        OpenSearchParameterConfiguration endTimeParameter = new OpenSearchParameterConfiguration();
        endTimeParameter.setAttributeModelJsonPath("properties.TimePeriod.stopDate");
        endTimeParameter.setName("end");
        endTimeParameter.setNamespace("time");
        paramConfigurations.add(endTimeParameter);

        timeExtension = new GeoTimeExtension();
        timeExtension.setActivated(true);

        regardsExtension = new RegardsExtension();
        regardsExtension.setActivated(true);

        mediaExtension = new MediaExtension();
        mediaExtension.setActivated(true);
    }

    @Override
    public boolean supports(SearchType searchType) {
        switch (searchType) {
            case COLLECTIONS:
            case DATAOBJECTS:
            case DATAOBJECTS_RETURN_DATASETS:
            case DATASETS:
            case DOCUMENTS:
                return true;
            case ALL:
            default:
                return false;
        }
    }

    @Override
    public ResponseEntity<Object> search(SearchContext context) throws ModuleException {
        init();
        FacetPage<AbstractEntity> facetPage = searchService
                .search(parse(context.getQueryParams()), context.getSearchType(), null, context.getPageable());
        try {
            return ResponseEntity.ok(formatResponse(facetPage, context));
        } catch (UnsupportedMediaTypesException e) {
            throw new ModuleException(e);
        }
    }

    @Override
    public ResponseEntity<Object> getEntity(SearchContext context) throws ModuleException {
        // Retrieve entity
        AbstractEntity entity = searchService.get(context.getUrn().get());
        FacetPage<AbstractEntity> facetPage = new FacetPage<>(Arrays.asList(entity), Sets.newHashSet(),
                context.getPageable(), 1);
        try {
            return ResponseEntity.ok(formatResponse(facetPage, context));
        } catch (UnsupportedMediaTypesException e) {
            throw new ModuleException(e);
        }
    }

    @Override
    public ICriterion parse(MultiValueMap<String, String> queryParams) throws ModuleException {
        // First parse q parameter for searchTerms if any.
        ICriterion searchTermsCriterion = openSearchService.parse(queryParams);
        // Then parse all parameters (open search parameters extension)
        return ICriterion.and(searchTermsCriterion, parseParametersExt(queryParams));
    }

    @Override
    public ResponseEntity<OpenSearchDescription> extra(SearchContext context) throws ModuleException {
        init();
        if (context.getExtra().isPresent() && context.getExtra().get().equals(EXTRA_DESCRIPTION)) {
            return ResponseEntity.ok(descriptionBuilder
                    .build(context, parseParametersExt(context.getQueryParams()),
                           Arrays.asList(mediaExtension, regardsExtension, timeExtension), paramConfigurations));
        } else {
            return ISearchEngine.super.extra(context);
        }
    }

    /**
     * Format search response for the given {@link MediaType} in the {@link SearchContext}
     * @param page search response
     * @param context {@link SearchContext} containing MediaType
     * @return formated response
     * @throws UnsupportedMediaTypesException
     */
    private Object formatResponse(FacetPage<AbstractEntity> page, SearchContext context)
            throws UnsupportedMediaTypesException {
        IOpenSearchResponseBuilder<?> builder = getBuilder(context);
        // TODO : Replace with real url
        builder.addMetadata(UUID.randomUUID().toString(), searchTitle, searchDescription,
                            "http://www.regards.com/opensearch-description.xml", context, configuration, page,
                            SearchEngineController.buildPaginationLinks(resourceService, page, context));
        page.getContent().stream()
                .forEach(e -> builder.addEntity(e, paramConfigurations,
                                                SearchEngineController.buildEntityLinks(resourceService, context, e)));
        return builder.build();
    }

    /**
     * Parse openSearch query to find all parameters from standard open search parameters extension.
     * @param queryParams
     * @return {@link ICriterion}
     */
    private ICriterion parseParametersExt(MultiValueMap<String, String> queryParams) {
        // Find AttributeModel for each parameter
        List<SearchParameter> attributes = buildParameters(queryParams);

        return ICriterion.and(timeExtension.buildCriterion(attributes), mediaExtension.buildCriterion(attributes),
                              regardsExtension.buildCriterion(attributes));
    }

    private List<SearchParameter> buildParameters(MultiValueMap<String, String> queryParams) {
        List<SearchParameter> searchParameters = Lists.newArrayList();
        for (Entry<String, List<String>> queryParam : queryParams.entrySet()) {
            try {
                AttributeModel attributeModel = finder.findByName(queryParam.getKey());
                if (attributeModel.getId() != null) {
                    attributeModel.buildJsonPath(StaticProperties.PROPERTIES);
                } else {
                    // Standard static attributes. Not a real attribute. So jsonPath = name;
                    attributeModel.setJsonPath(attributeModel.getName());
                }
                // Search configuration if any
                OpenSearchParameterConfiguration conf = paramConfigurations.stream()
                        .filter(p -> p.getAttributeModelJsonPath().equals(attributeModel.getJsonPath())).findFirst()
                        .orElse(null);
                searchParameters.add(new SearchParameter(attributeModel, conf, queryParam.getValue()));
            } catch (OpenSearchUnknownParameter e) {
                LOGGER.warn("Parameter not found.", e);
            }
        }
        return searchParameters;
    }

    /**
     * Retrieve a response builder from existing ones matching the {@link MediaType} from the {@link SearchContext}
     * @param context {@link SearchContext}
     * @return {@link IOpenSearchResponseBuilder}
     * @throws UnsupportedMediaTypesException
     */
    private IOpenSearchResponseBuilder<?> getBuilder(SearchContext context) throws UnsupportedMediaTypesException {
        IOpenSearchResponseBuilder<?> responseBuilder;
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
        // TODO Auto-generated method stub
        return ISearchEngine.super.getPropertyValues(context);
    }

}
