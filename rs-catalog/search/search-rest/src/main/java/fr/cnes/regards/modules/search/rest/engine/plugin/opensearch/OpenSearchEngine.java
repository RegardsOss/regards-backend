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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.rometools.rome.feed.atom.Feed;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.opensearch.service.IOpenSearchService;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.search.domain.plugin.ISearchEngine;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.atom.OpenSearchResponseBuilder;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;

/**
 * OpenSearch engine plugin
 * @author Marc Sordi
 * @author Sébastien Binda
 */
@Plugin(id = "OpenSearchEngine", author = "REGARDS Team", contact = "regards@c-s.fr",
        description = "Native search engine", licence = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss",
        version = "1.0.0")
public class OpenSearchEngine implements ISearchEngine<Object, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchEngine.class);

    public static final String PARAMETERS_CONFIGURATION_PARAM = "parameters";

    /**
     * Query parser
     */
    @Autowired
    protected IAttributeFinder finder;

    /**
     * Query parser
     */
    @Autowired
    protected IOpenSearchService openSearchService;

    @Autowired
    private Gson gson;

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

    @PluginParameter(name = OpenSearchEngine.PARAMETERS_CONFIGURATION_PARAM, label = "Open search available parameters",
            keylabel = "Parameter name")
    private final List<OpenSearchParameterConfiguration> parameters = Lists.newArrayList();

    @Override
    public boolean supports(SearchType searchType) {
        switch (searchType) {
            case ALL:
            case COLLECTIONS:
            case DATAOBJECTS:
            case DATAOBJECTS_RETURN_DATASETS:
            case DATASETS:
            case DOCUMENTS:
                return true;
            default:
                return false;
        }
    }

    @Override
    public ResponseEntity<Object> search(SearchContext context) throws ModuleException {
        FacetPage<AbstractEntity> facetPage = searchService
                .search(parse(context.getQueryParams()), context.getSearchType(), null, context.getPageable());
        return ResponseEntity.ok(formatResponse(facetPage, context));
    }

    private Object formatResponse(FacetPage<AbstractEntity> page, SearchContext context) {
        if (context.getHeaders().getAccept().contains(MediaType.APPLICATION_ATOM_XML)) {
            return formatAtomResponseRome(context, page);
        } else {
            return formatGeoJsonResponse(page);
        }
    }

    private Object formatAtomResponseRome(SearchContext context, FacetPage<AbstractEntity> page) {
        Feed feed = OpenSearchResponseBuilder
                .buildFeedMetadata(UUID.randomUUID().toString(), searchTitle, searchDescription,
                                   "http://www.regards.com/opensearch-description.xml", context, page);
        feed.setEntries(page.getContent().stream()
                .map(e -> OpenSearchResponseBuilder.buildFeedAtomEntry(feed, e, gson))
                .collect(Collectors.toList()));
        return feed;
    }

    private Object formatGeoJsonResponse(FacetPage<AbstractEntity> page) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ICriterion parse(MultiValueMap<String, String> queryParams) throws ModuleException {
        // First parse q parameter for searchTerms if any.
        ICriterion searchTermsCriterion = openSearchService.parse(queryParams);
        // Then parse all parameters (open search parameters extension)
        return ICriterion.and(searchTermsCriterion, parseParametersExtension(queryParams));
    }

    /**
     * Parse openSearch query to find all parameters from standard open search parameters extension.
     * @param queryParams
     * @return {@link ICriterion}
     */
    private ICriterion parseParametersExtension(MultiValueMap<String, String> queryParams) {
        List<ICriterion> criteria = new ArrayList<>();
        for (Entry<String, List<String>> queryParam : queryParams.entrySet()) {
            // Get couple parameter name/values
            String paramName = queryParam.getKey();
            List<String> values = queryParam.getValue();
            // Find associated attribute configuration from plugin conf
            Optional<OpenSearchParameterConfiguration> oParam = parameters.stream()
                    .filter(p -> p.getName().equals(paramName)).findFirst();
            if (oParam.isPresent()) {
                OpenSearchParameterConfiguration conf = oParam.get();
                try {
                    // Parse attribute value to create associated ICriterion using parameter configuration
                    criteria.add(AttributeCriterionBuilder.build(conf, values, finder));
                } catch (OpenSearchUnknownParameter e) {
                    LOGGER.error("Invalid regards attribute {} mapped to OpenSearchEngine parameter : {}",
                                 conf.getAttributeModelId(), paramName);
                }
            } else {
                LOGGER.error("No regards attribute found for OpenSearchEngine parameter : {}", paramName);
            }
        }
        return criteria.isEmpty() ? ICriterion.all() : ICriterion.and(criteria);
    }
}
