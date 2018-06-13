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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.compress.utils.Lists;
import org.assertj.core.util.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.opensearch.service.IOpenSearchService;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.search.domain.plugin.ISearchEngine;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.rest.SearchEngineController;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.description.OpenSearchDescBuilder;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.exception.UnsupportedMediaTypesException;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.geo.GeoTimeExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.media.MediaExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.regards.RegardsExtension;
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
public class OpenSearchEngine implements ISearchEngine<Object, OpenSearchDescription, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchEngine.class);

    public static final String ENGINE_ID = "opensearch";

    public static final String TIME_EXTENSION_PARAMETER = "timeExtension";

    public static final String REGARDS_EXTENSION_PARAMETER = "regardsExtension";

    public static final String MEDIA_EXTENSION_PARAMETER = "mediaExtension";

    public static final String EXTRA_DESCRIPTION = "opensearchDescription.xml";

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
    private OpenSearchDescBuilder descriptionBuilder;

    @Autowired
    private List<IOpenSearchResponseBuilder<?>> responseBuilders;

    /**
     * Business search service
     */
    @Autowired
    protected ICatalogSearchService searchService;

    @PluginParameter(name = "searchTitle", label = "Title of responses associated to this search engine",
            description = "Search title for response metadatas. Used to construct metadatas for atom+xml and geo+json responses.",
            defaultValue = "Open search engire title")
    private String searchTitle;

    @PluginParameter(name = "searchDescription", label = "Description of responses associated to this search engine",
            description = "Description for response metadatas. Used to construct metadatas for atom+xml and geo+json responses.",
            defaultValue = "Open search engire description")
    private String searchDescription;

    @PluginParameter(name = OpenSearchEngine.TIME_EXTENSION_PARAMETER, label = "Open search time extension")
    private GeoTimeExtension timeExtension;

    @PluginParameter(name = OpenSearchEngine.REGARDS_EXTENSION_PARAMETER, label = "Open search regards extension")
    private RegardsExtension regardsExtension;

    @PluginParameter(name = OpenSearchEngine.MEDIA_EXTENSION_PARAMETER, label = "Open search media extension")
    private MediaExtension mediaExtension;

    private final List<OpenSearchParameterConfiguration> parameters = Lists.newArrayList();

    public void init() {
        timeExtension = new GeoTimeExtension();
        timeExtension.setActivated(true);
        timeExtension.setTimeStartAttribute("properties.TimePeriod.startDate");
        timeExtension.setTimeEndAttribute("properties.TimePeriod.stopDate");

        regardsExtension = new RegardsExtension();
        regardsExtension.setActivated(true);

        mediaExtension = new MediaExtension();
        mediaExtension.setActivated(true);

        this.initialize();
    }

    @PluginInit
    public void initialize() {
        responseBuilders.stream().forEach(b -> b.addExtension(timeExtension));
        responseBuilders.stream().forEach(b -> b.addExtension(regardsExtension));
        responseBuilders.stream().forEach(b -> b.addExtension(mediaExtension));
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
                // TODO handle for all ?
            default:
                return false;
        }
    }

    @Override
    public ResponseEntity<Object> search(SearchContext context) throws ModuleException {
        // TODO Remove Init when Plugin instanciation is done
        // ......
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

        // TODO Remove Init when Plugin instanciation is done
        // ......
        init();

        if (context.getExtra().isPresent() && context.getExtra().get().equals(EXTRA_DESCRIPTION)) {
            try {
                return ResponseEntity
                        .ok(descriptionBuilder.build(getEntityType(context), getEndpoint(context),
                                                     Arrays.asList(mediaExtension, regardsExtension, timeExtension)));
            } catch (UnsupportedEncodingException e) {
                throw new ModuleException(e);
            }
        } else {
            return ISearchEngine.super.extra(context);
        }
    }

    private EntityType getEntityType(SearchContext context) {
        switch (context.getSearchType()) {
            case COLLECTIONS:
                return EntityType.COLLECTION;
            case DATAOBJECTS:
                return EntityType.DATA;
            case DATASETS:
                return EntityType.DATASET;
            case DOCUMENTS:
                return EntityType.DOCUMENT;
            case ALL:
            case DATAOBJECTS_RETURN_DATASETS:
            default:
                throw new UnsupportedOperationException(String.format("Unsupproted entity type for open search. %s",
                                                                      context.getSearchType().toString()));
        }
    }

    private String getEndpoint(SearchContext context) {
        String engineMapping = SearchEngineController.TYPE_MAPPING.replace(SearchEngineController.ENGINE_TYPE_PARAMETER,
                                                                           ENGINE_ID);
        switch (context.getSearchType()) {
            case COLLECTIONS:
                return engineMapping + SearchEngineController.SEARCH_COLLECTIONS_MAPPING;
            case DATAOBJECTS:
                return engineMapping + SearchEngineController.SEARCH_DATAOBJECTS_MAPPING;
            case DATASETS:
                return engineMapping + SearchEngineController.SEARCH_DATASETS_MAPPING;
            case DOCUMENTS:
                return engineMapping + SearchEngineController.SEARCH_DOCUMENTS_MAPPING;
            case ALL:
            case DATAOBJECTS_RETURN_DATASETS:
            default:
                throw new UnsupportedOperationException(String.format("Unsupproted entity type for open search. %s",
                                                                      context.getSearchType().toString()));
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
        builder.addMetadata(UUID.randomUUID().toString(), searchTitle, searchDescription,
                            "http://www.regards.com/opensearch-description.xml", context, page);
        page.getContent().stream().forEach(builder::addEntity);
        return builder.build();
    }

    /**
     * Parse openSearch query to find all parameters from standard open search parameters extension.
     * @param queryParams
     * @return {@link ICriterion}
     */
    private ICriterion parseParametersExt(MultiValueMap<String, String> queryParams) {
        List<ICriterion> criteria = new ArrayList<>();
        for (Entry<String, List<String>> queryParam : queryParams.entrySet()) {
            // Get couple parameter name/values
            String paramName = queryParam.getKey();
            List<String> values = queryParam.getValue();
            // Find associated attribute configuration from plugin conf
            Optional<OpenSearchParameterConfiguration> oParam = parameters.stream()
                    .filter(p -> p.getName().equals(paramName)).findFirst();
            // TODO handle extensions
            try {
                if (oParam.isPresent()) {
                    OpenSearchParameterConfiguration conf = oParam.get();
                    // Parse attribute value to create associated ICriterion using parameter configuration
                    criteria.add(AttributeCriterionBuilder.build(conf, values, finder));
                } else {
                    criteria.add(AttributeCriterionBuilder.build(paramName, ParameterOperator.EQ, values, finder));
                }
            } catch (OpenSearchUnknownParameter e) {
                LOGGER.error("Invalid public attribute {}. Unknown type.", paramName);
            }
        }
        return criteria.isEmpty() ? ICriterion.all() : ICriterion.and(criteria);
    }

    /**
     * Retrieve a response builder from existing ones matching the {@link MediaType} from the {@link SearchContext}
     * @param context {@link SearchContext}
     * @return {@link IOpenSearchResponseBuilder}
     * @throws UnsupportedMediaTypesException
     */
    private IOpenSearchResponseBuilder<?> getBuilder(SearchContext context) throws UnsupportedMediaTypesException {
        Optional<IOpenSearchResponseBuilder<?>> builder = responseBuilders.stream()
                .filter(b -> b.supports(context.getHeaders().getAccept())).findFirst();
        if (builder.isPresent()) {
            return builder.get();
        } else {
            throw new UnsupportedMediaTypesException(context.getHeaders().getAccept());
        }
    }
}
