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
package fr.cnes.regards.modules.order.domain.basket;

import org.springframework.util.MultiValueMap;

import javax.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * POJO Containing information to add entity into user basket
 *
 * @author Sébastien Binda
 */
public class BasketSelectionRequest {

    /**
     * Engine type (i.e ISearchEngine plugin id) to use for searches.
     */
    @NotBlank(message = "Engine type may not be empty")
    private String engineType;

    /**
     * URN identifier of the dataset on which the search is. Can be null to search on all datasets.
     */
    private String datasetUrn;

    /**
     * Catalog search request permitting to retrieve data or null if only IP_IDs must be retrieved
     */
    private MultiValueMap<String, String> searchParameters;

    /**
     * A set of IP_ID to exclude
     */
    private Set<String> entityIdsToExclude;

    /**
     * A set of IP_ID to include
     */
    private Set<String> entityIdsToInclude;

    private final OffsetDateTime selectionDate = OffsetDateTime.now();

    public Set<String> getEntityIdsToExclude() {
        return entityIdsToExclude;
    }

    public void setEntityIdsToExclude(Set<String> entityIdsToExclude) {
        this.entityIdsToExclude = entityIdsToExclude;
    }

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public MultiValueMap<String, String> getSearchParameters() {
        return searchParameters;
    }

    public void setSearchParameters(MultiValueMap<String, String> searchParameters) {
        this.searchParameters = searchParameters;
    }

    public String getDatasetUrn() {
        return datasetUrn;
    }

    public void setDatasetUrn(String datasetUrn) {
        this.datasetUrn = datasetUrn;
    }

    public OffsetDateTime getSelectionDate() {
        return selectionDate;
    }

    public Set<String> getEntityIdsToInclude() {
        return entityIdsToInclude;
    }

    public void setEntityIdsToInclude(Set<String> entityIdsToInclude) {
        this.entityIdsToInclude = entityIdsToInclude;
    }
}
