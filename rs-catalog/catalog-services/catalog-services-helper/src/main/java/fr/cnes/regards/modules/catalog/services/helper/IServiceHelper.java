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
package fr.cnes.regards.modules.catalog.services.helper;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.search.domain.SearchRequest;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Service Helper interface
 *
 * @author Sébastien Binda
 */
public interface IServiceHelper {

    /**
     * Search for all {@link DataObject}s with given internal identifiers (ipId)
     *
     * @param entityIds        internal identifiers of entity to retrieve.
     * @param pageIndex        index of the page to retrieve.
     * @param nbEntitiesByPage number of entities by page.
     * @return {@link Page}<{@link DataObject}>
     */
    Page<DataObject> getDataObjects(List<String> entityIds, int pageIndex, int nbEntitiesByPage);

    /**
     * Search for all {@link DataObject}s corresponding to the given open search query
     *
     * @param searchRequest    {@link SearchRequest}
     * @param pageIndex        index of the page to retrieve.
     * @param nbEntitiesByPage number of entities by page.
     * @return {@link Page}<{@link DataObject}>
     * @throws {@link ModuleException} Invalid request
     */
    Page<DataObject> getDataObjects(SearchRequest searchRequest, int pageIndex, int nbEntitiesByPage)
        throws ModuleException;

}
