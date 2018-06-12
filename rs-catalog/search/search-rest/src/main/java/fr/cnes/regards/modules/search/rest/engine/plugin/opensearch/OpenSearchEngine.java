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

import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
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
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.exception.UnsupportedMediaTypesException;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.geo.GeoTimeExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.media.MediaExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.regards.RegardsExtension;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;

/**
 * OpenSearch engine plugin
 * @author Marc Sordi
 * @author Sébastien Binda
 */
@Plugin(id = "OpenSearchEngine", author = "REGARDS Team", contact = "regards@c-s.fr",
        description = "Native search engine", licence = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss",
        version = "1.0.0")
public class OpenSearchEngine implements ISearchEngine<Object, Void, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchEngine.class);

    public static final String TIME_EXTENSION_PARAMETER = "timeExtension";

    public static final String REGARDS_EXTENSION_PARAMETER = "regardsExtension";

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

    @PluginParameter(name = OpenSearchEngine.REGARDS_EXTENSION_PARAMETER, label = "Open search media extension")
    private MediaExtension mediaExtension;

    private final List<OpenSearchParameterConfiguration> parameters = Lists.newArrayList();

    public void init() {
        timeExtension = new GeoTimeExtension();
        timeExtension.setActivated(true);
        timeExtension.setTimeStartAttribute("TimePeriod.startDate");
        timeExtension.setTimeEndAttribute("TimePeriod.stopDate");

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
        init();
        FacetPage<AbstractEntity> facetPage = searchService
                .search(parse(context.getQueryParams()), context.getSearchType(), null, context.getPageable());
        try {
            return ResponseEntity.ok(formatResponse(facetPage, context));
        } catch (UnsupportedMediaTypesException e) {
            throw new ModuleException(e);
        }
    }

    private Object formatResponse(FacetPage<AbstractEntity> page, SearchContext context)
            throws UnsupportedMediaTypesException {
        IOpenSearchResponseBuilder<?> builder = getBuilder(context);
        builder.addMetadata(UUID.randomUUID().toString(), searchTitle, searchDescription,
                            "http://www.regards.com/opensearch-description.xml", context, page);
        page.getContent().stream().forEach(builder::addEntity);
        return builder.build();
    }

    @Override
    public ICriterion parse(MultiValueMap<String, String> queryParams) throws ModuleException {
        // First parse q parameter for searchTerms if any.
        ICriterion searchTermsCriterion = openSearchService.parse(queryParams);
        // Then parse all parameters (open search parameters extension)
        return ICriterion.and(searchTermsCriterion, parseParametersExt(queryParams));
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

    @Override
    public ResponseEntity<Object> getEntity(SearchContext context) throws ModuleException {
        // TODO Auto-generated method stub
        return null;
    }

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
