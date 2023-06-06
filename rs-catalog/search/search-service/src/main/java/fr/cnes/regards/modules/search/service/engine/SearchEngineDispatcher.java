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
package fr.cnes.regards.modules.search.service.engine;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.dam.domain.entities.StaticProperties;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import fr.cnes.regards.modules.search.domain.plugin.*;
import fr.cnes.regards.modules.search.dto.SearchRequest;
import fr.cnes.regards.modules.search.service.IBusinessSearchService;
import fr.cnes.regards.modules.search.service.ISearchEngineConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.*;

/**
 * Search engine service dispatcher.<br/>
 * <p>
 * Each methods acts as a proxy for search engine.<br/>
 * <ul>
 * <li>First, context debugging may be display</li>
 * <li>The system look for a plugin instance</li>
 * <li>The system dispatches search to plugin instance</li>
 * </ul>
 *
 * @author Marc Sordi
 */
@Service
public class SearchEngineDispatcher implements ISearchEngineDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchEngineDispatcher.class);

    @Autowired
    private Validator validator;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private ISearchEngineConfigurationService searchEngineService;

    /**
     * Business search service
     */
    @Autowired
    protected IBusinessSearchService searchService;

    @SuppressWarnings("unchecked")
    @Override
    public <T> ResponseEntity<T> dispatchRequest(SearchContext context, IEntityLinkBuilder linkBuilder)
        throws ModuleException {

        // Validate search context
        Errors errors = new MapBindingResult(new HashMap<>(), "searchContext");
        validator.validate(context, errors);
        if (errors.hasErrors()) {
            StringJoiner joiner = new StringJoiner(". ");
            errors.getAllErrors().forEach(error -> {
                joiner.add(error.toString());
            });
            throw new EntityInvalidException(joiner.toString());
        }

        // Debugging
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Handling request for engine : {}", context.getEngineType());
            LOGGER.debug("Search type : {}", context.getSearchType());
            if (context.getDatasetUrn().isPresent()) {
                LOGGER.debug("Searching data objects on dataset : {}", context.getDatasetUrn().get().toString());
            }
            if (context.getExtra().isPresent()) {
                LOGGER.debug("Handling request extra path : {}", context.getExtra().get());
            }
            if (context.getUrn().isPresent()) {
                LOGGER.debug("Getting entity with URN : {}", context.getUrn().get().toString());
            }
            if (!context.getPropertyNames().isEmpty()) {
                LOGGER.debug("Search values for properties : {}",
                             context.getPropertyNames().stream().reduce("", (result, name) -> result + "," + name));
            }
            if (context.getMaxCount().isPresent()) {
                LOGGER.debug("Maximum result count for property values : {}", context.getMaxCount().get());
            }
            if (context.getBoundCalculation()) {
                LOGGER.debug("Search for properties bounds");
            }
            if (context.getDateTypes().isPresent()) {
                context.getDateTypes().get().forEach(dataType -> LOGGER.debug("Summary data type : {}", dataType));
            }
            context.getHeaders().forEach((key, values) -> LOGGER.debug("Header : {} -> {}", key, values.toString()));
            if (context.getQueryParams() != null) {
                context.getQueryParams()
                       .forEach((key, values) -> LOGGER.debug("Query param : {} -> {}", key, values.toString()));
            }
            LOGGER.debug(context.getPageable() == null ? "No pagination" : context.getPageable().toString());
        }

        // Retrieve search engine plugin from search context
        ISearchEngine<?, ?, ?, ?> searchEngine = getSearchEngine(context.getDatasetUrn(), context.getEngineType());
        ISearchEngine<?, ?, ?, ?> searchEngineParser = getSearchEngineParser(context, searchEngine);
        if (context.getExtra().isPresent()) {
            return (ResponseEntity<T>) searchEngine.extra(context, linkBuilder);
        } else if (context.getUrn().isPresent()) {
            return (ResponseEntity<T>) searchEngine.getEntity(context, linkBuilder);
        } else if (!context.getPropertyNames().isEmpty() && !context.getBoundCalculation()) {
            return (ResponseEntity<T>) searchEngine.getPropertyValues(context);
        } else if (!context.getPropertyNames().isEmpty() && context.getBoundCalculation()) {
            return (ResponseEntity<T>) searchEngine.getPropertiesBounds(context);
        } else if (context.getDateTypes().isPresent()) {
            return (ResponseEntity<T>) searchEngine.getSummary(context);
        } else {
            return (ResponseEntity<T>) searchEngine.search(context, searchEngineParser, linkBuilder);
        }
    }

    private ISearchEngine<?, ?, ?, ?> getSearchEngineParser(SearchContext context,
                                                            ISearchEngine<?, ?, ?, ?> searchEngine)
        throws ModuleException {
        if (context.getEngineRequestParserType() != null) {
            try {
                return getSearchEngine(context.getDatasetUrn(), context.getEngineRequestParserType());
            } catch (ModuleException e) {
                LOGGER.error(e.getMessage(), e);
                return searchEngine;
            }
        } else {
            return searchEngine;
        }

    }

    @Override
    public ISearchEngine<?, ?, ?, ?> getSearchEngine(Optional<UniformResourceName> datasetUrn, String engineType)
        throws ModuleException {
        SearchEngineConfiguration conf = searchEngineService.retrieveConf(datasetUrn, engineType);
        try {
            return pluginService.getPlugin(conf.getConfiguration().getBusinessId());
        } catch (NotAvailablePluginConfigurationException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

    /**
     * Compute a {@link SearchRequest} to a {@link ICriterion}
     */
    @Override
    public ICriterion computeComplexCriterion(SearchRequest searchRequest) throws ModuleException {
        UniformResourceName datasetUrn = null;
        if (searchRequest.getDatasetUrn() != null) {
            datasetUrn = UniformResourceName.fromString(searchRequest.getDatasetUrn());
        }
        ISearchEngine<?, ?, ?, ?> searchEngine = this.getSearchEngine(Optional.ofNullable(datasetUrn),
                                                                      searchRequest.getEngineType());

        // Open search request
        SearchContext context = SearchContext.build(SearchType.DATAOBJECTS,
                                                    searchRequest.getEngineType(),
                                                    SearchEngineMappings.getJsonHeaders(),
                                                    searchRequest.getSearchParameters(),
                                                    PageRequest.of(0, 1));
        if (searchRequest.getDatasetUrn() != null) {
            context = context.withDatasetUrn(UniformResourceName.fromString(searchRequest.getDatasetUrn()));
        }
        ICriterion reqCrit = searchEngine.parse(context);

        // Date criterion
        if (searchRequest.getSearchDateLimit() != null) {
            reqCrit = ICriterion.and(reqCrit,
                                     ICriterion.lt(StaticProperties.CREATION_DATE_PATH,
                                                   searchRequest.getSearchDateLimit()));
        }

        // Include ids criterion
        if ((searchRequest.getEntityIdsToInclude() != null) && !searchRequest.getEntityIdsToInclude().isEmpty()) {
            List<String> values = new ArrayList<>();
            for (String ipId : searchRequest.getEntityIdsToInclude()) {
                values.add(ipId);
            }
            reqCrit = ICriterion.and(reqCrit, ICriterion.in(StaticProperties.IP_ID, StringMatchType.KEYWORD, values));
            values.clear();
        }

        // Exclude ids criterion
        if ((searchRequest.getEntityIdsToExclude() != null) && !searchRequest.getEntityIdsToExclude().isEmpty()) {
            List<String> values = new ArrayList<>();
            for (String ipId : searchRequest.getEntityIdsToExclude()) {
                values.add(ipId);
            }
            reqCrit = ICriterion.and(reqCrit,
                                     ICriterion.not(ICriterion.in(StaticProperties.IP_ID,
                                                                  StringMatchType.KEYWORD,
                                                                  values)));
            values.clear();
        }

        return reqCrit;
    }
}
