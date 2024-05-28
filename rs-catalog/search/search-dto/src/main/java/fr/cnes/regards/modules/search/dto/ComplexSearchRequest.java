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
package fr.cnes.regards.modules.search.dto;

import fr.cnes.regards.framework.urn.DataType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

/**
 * POJO Containig information to handle a new search on catalog from complex search system controller
 *
 * @author Sébastien Binda
 */
public class ComplexSearchRequest {

    /**
     * Search requests to handle
     */
    @Valid
    @Size(min = 0, max = 100, message = "Number of requests in one Complex search request must be between 0 and 100")
    private List<SearchRequest> requests;

    /**
     * {@link DataType}s to retrieve from search requests
     */
    private List<DataType> dataTypes;

    private int page = 0;

    private int size = 20;

    public ComplexSearchRequest(List<DataType> dataTypes) {
        super();
        this.dataTypes = dataTypes;
        this.requests = Lists.newArrayList();
    }

    public ComplexSearchRequest(List<DataType> dataTypes, int page, int size) {
        super();
        this.dataTypes = dataTypes;
        this.requests = Lists.newArrayList();
        this.page = page;
        this.size = size;
    }

    public List<SearchRequest> getRequests() {
        return requests;
    }

    public void setRequests(List<SearchRequest> requests) {
        this.requests = requests;
    }

    public List<DataType> getDataTypes() {
        return dataTypes;
    }

    public void setDataTypes(List<DataType> dataTypes) {
        this.dataTypes = dataTypes;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

}
