/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.indexer.service.Searches;
import fr.cnes.regards.modules.opensearch.service.IOpenSearchService;
import fr.cnes.regards.modules.search.domain.SearchRequest;
import fr.cnes.regards.modules.search.rest.engine.SearchEngineDispatcher;

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

    @Autowired
    private SearchEngineDispatcher dispatcher;

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
        PageRequest pageReq = PageRequest.of(pageIndex, nbEntitiesByPage);
        return searchService.search(searchKey, pageReq, ICriterion.or(idCrits));
    }

    @Override
    public Page<DataObject> getDataObjects(SearchRequest searchRequest, int pageIndex, int nbEntitiesByPage)
            throws ModuleException {
        SimpleSearchKey<DataObject> searchKey = Searches.onSingleEntity(EntityType.DATA);
        ICriterion crit = dispatcher.computeComplexCriterion(searchRequest);
        PageRequest pageReq = PageRequest.of(pageIndex, nbEntitiesByPage);
        return searchService.search(searchKey, pageReq, crit);
    }

}
