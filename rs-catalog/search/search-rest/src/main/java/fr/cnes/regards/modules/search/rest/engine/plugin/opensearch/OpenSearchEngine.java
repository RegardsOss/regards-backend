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

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.rometools.modules.opensearch.OpenSearchModule;
import com.rometools.modules.opensearch.entity.OSQuery;
import com.rometools.modules.opensearch.impl.OpenSearchModuleImpl;
import com.rometools.rome.feed.atom.Link;
import com.rometools.rome.feed.module.Module;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.attribute.AbstractAttribute;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.opensearch.service.IOpenSearchService;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.search.domain.plugin.ISearchEngine;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
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
            return formatAtomResponseRome(page);
        } else {
            return formatGeoJsonResponse(page);
        }
    }

    private Object formatGeoJsonResponse(FacetPage<AbstractEntity> page) {
        // TODO Auto-generated method stub
        return null;
    }

    private Object formatAtomResponseRome(FacetPage<AbstractEntity> page) {
        com.rometools.rome.feed.atom.Feed feed = new com.rometools.rome.feed.atom.Feed("atom_1.0");

        feed.setId("id1234");
        feed.setTitle("Concretepage.com");
        List<Link> links = new ArrayList<>();
        Link link = new Link();
        link.setHref("http://www.concretepage.com");
        links.add(link);
        feed.setAlternateLinks(links);
        feed.setEntries(page.getContent().stream().map(this::buildFeedAtomEntry).collect(Collectors.toList()));

        // Add the opensearch module, you would get information like totalResults from the
        // return results of your search
        List<Module> mods = feed.getModules();
        OpenSearchModule osm = new OpenSearchModuleImpl();
        osm.setItemsPerPage(1);
        osm.setStartIndex(1);
        osm.setTotalResults(1024);
        osm.setItemsPerPage(50);

        OSQuery query = new OSQuery();
        query.setRole("superset");
        query.setSearchTerms("Java Syndication");
        query.setStartPage(1);
        osm.addQuery(query);

        Link link2 = new Link();
        link2.setHref("http://www.bargainstriker.com/opensearch-description.xml");
        link2.setType("application/opensearchdescription+xml");
        osm.setLink(link2);

        mods.add(osm);

        feed.setModules(mods);

        return feed;
    }

    private com.rometools.rome.feed.atom.Entry buildFeedAtomEntry(AbstractEntity entity) {
        com.rometools.rome.feed.atom.Entry entry = new com.rometools.rome.feed.atom.Entry();
        entry.setId(entity.getIpId().toString());
        if (entity.getCreationDate() != null) {
            entry.setPublished(Date.valueOf(entity.getCreationDate().toLocalDate()));
        }
        entry.setTitle(entity.getLabel());
        entry.setForeignMarkup(entity.getProperties().stream().map(this::buildContentAttributeRome)
                .collect(Collectors.toList()));
        return entry;
    }

    private Element buildContentAttributeRome(AbstractAttribute<?> att) {
        Element element = new Element(att.getName());
        element.addContent(gson.toJson(att.getValue()));
        return element;
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
