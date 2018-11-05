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
package fr.cnes.regards.modules.search.domain;

import java.time.OffsetDateTime;
import java.util.Collection;

import org.springframework.util.MultiValueMap;

/**
 * POJO Containig information to handle a new search on catalog from complex search system controller
 * @author Sébastien Binda
 */
public class SearchRequest {

    /**
     * Engine to use for the research
     */
    private final String engineType;

    /**
     * Dataset urn identifier on which search must be applyed. If null search is applyed on the whole catalog
     */
    private final String datasetUrn;

    /**
     * Search engine query parameters
     */
    private final MultiValueMap<String, String> searchParameters;

    /**
     * Additional entity ids to return with the search results.
     */
    private final Collection<String> entityIdsToInclude;

    /**
     * Entity ids to exclud from search results.
     */
    private final Collection<String> entityIdsToExclude;

    /**
     * Maximum creation date of researched entities. If null no date criterion is added to the search.
     */
    private final OffsetDateTime searchDateLimit;

    public SearchRequest(String engineType, String datasetUrn, MultiValueMap<String, String> searchParameters,
            Collection<String> entityIdsToInclude, Collection<String> entityIdsToExclude,
            OffsetDateTime searchDateLimit) {
        super();
        this.engineType = engineType;
        this.datasetUrn = datasetUrn;
        this.searchParameters = searchParameters;
        this.entityIdsToInclude = entityIdsToInclude;
        this.entityIdsToExclude = entityIdsToExclude;
        this.searchDateLimit = searchDateLimit;
    }

    public String getEngineType() {
        return engineType;
    }

    public String getDatasetUrn() {
        return datasetUrn;
    }

    public MultiValueMap<String, String> getSearchParameters() {
        return searchParameters;
    }

    public OffsetDateTime getSearchDateLimit() {
        return searchDateLimit;
    }

    public Collection<String> getEntityIdsToExclude() {
        return entityIdsToExclude;
    }

    public Collection<String> getEntityIdsToInclude() {
        return entityIdsToInclude;
    }

}
