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
package fr.cnes.regards.modules.catalog.services.helper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.indexer.service.Searches;
import fr.cnes.regards.modules.opensearch.service.IOpenSearchService;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchParseException;

/**
 * Helper to handle catalog entities searches for all Catalogue service plugins.
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class ServiceHelper implements IServiceHelper {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceHelper.class);

    /**
     * Service to search datas from the catalog.
     */
    private final ISearchService searchService;

    /**
     * Service to handle and parse open search queries.
     */
    private final IOpenSearchService openSearchService;

    /**
     * Get current tenant at runtime and allows tenant forcing. Autowired.
     */
    private final IRuntimeTenantResolver tenantResolver;

    /**
     * Constructor
     * @param searchService
     * @param openSearchService
     * @param tenantResolver
     */
    public ServiceHelper(ISearchService searchService, IOpenSearchService openSearchService,
            IRuntimeTenantResolver tenantResolver) {
        super();
        this.searchService = searchService;
        this.openSearchService = openSearchService;
        this.tenantResolver = tenantResolver;
    }

    @Override
    public Page<DataObject> getDataObjects(List<String> entityIds, int pageIndex, int nbEntitiesByPage) {
        SimpleSearchKey<DataObject> searchKey = Searches.onSingleEntity(EntityType.DATA);
        ICriterion[] idCrits = new ICriterion[entityIds.size()];
        int count = 0;
        for (String id : entityIds) {
            idCrits[count] = ICriterion.eq("ipId", id);
            count++;
        }
        PageRequest pageReq = new PageRequest(pageIndex, nbEntitiesByPage);
        return searchService.search(searchKey, pageReq, ICriterion.or(idCrits));
    }

    @Override
    public Page<DataObject> getDataObjects(String openSearchQuery, int pageIndex, int nbEntitiesByPage)
            throws OpenSearchParseException {
        SimpleSearchKey<DataObject> searchKey = Searches.onSingleEntity(EntityType.DATA);
        String queryParameters = openSearchQuery;
        try {
            queryParameters = URLEncoder.encode(URLDecoder.decode(queryParameters, "UTF-8"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        }
        ICriterion crit = openSearchService.parse(String.format("q=%s", queryParameters));
        PageRequest pageReq = new PageRequest(pageIndex, nbEntitiesByPage);
        return searchService.search(searchKey, pageReq, crit);
    }

}
