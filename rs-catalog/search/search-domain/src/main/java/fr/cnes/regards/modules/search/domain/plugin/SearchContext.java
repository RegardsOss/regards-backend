/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.domain.plugin;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;

/**
 * Search context for search engine.<br/>
 * Use {@link #build(SearchType, String, HttpHeaders, MultiValueMap, Pageable)} to initialize a new context.<br/>
 * Additional optional properties can be set using {@link #withDatasetUrn(UniformResourceName)},
 * {@link #withExtra(String)},
 * {@link #withUrn(UniformResourceName)}, {@link #withPropertyName(String)}, {@link #withMaxCount(Integer)},
 * {@link #withDataTypes(List)}.
 *
 * @author Marc Sordi
 *
 */
public class SearchContext {

    /**
     * Search type
     */
    @NotNull(message = "Search type is required")
    private SearchType searchType;

    /**
     * Required path parameter representing engine type allowing to retrieve search engine plugin
     */
    @NotBlank(message = "Engine type is required")
    private String engineType;

    /**
     * Optional path parameter to retrieve a single entity idenfied by this URN
     */
    private UniformResourceName urn;

    /**
     * Optional path parameter representing dataset identifier for data object search on this specified dataset
     */
    private UniformResourceName datasetUrn;

    /**
     * Additional optional path parameter for specific route handling.<br/>
     * For example, this parameter is used for OpenSearch description handling.
     */
    private String extra;

    /**
     * Property name to retrieve its available values
     */
    private String propertyName;

    /**
     * Maximum result count for property values
     */
    private Integer maxCount;

    /**
     * List of file type to consider when computing summary
     */
    private List<DataType> dateTypes;

    /**
     * Request HTTP headers, useful for managing return {@link MediaType}
     */
    @NotNull(message = "HTTP headers is required")
    private HttpHeaders headers;

    /**
     * Request query parameters
     */
    private MultiValueMap<String, String> queryParams;

    /**
     * Pagination properties
     */
    private Pageable pageable;

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public Optional<UniformResourceName> getUrn() {
        return Optional.ofNullable(urn);
    }

    public void setUrn(UniformResourceName urn) {
        this.urn = urn;
    }

    public Optional<UniformResourceName> getDatasetUrn() {
        return Optional.ofNullable(datasetUrn);
    }

    public void setDatasetUrn(UniformResourceName datasetUrn) {
        this.datasetUrn = datasetUrn;
    }

    public Optional<String> getExtra() {
        return Optional.ofNullable(extra);
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public Optional<String> getPropertyName() {
        return Optional.ofNullable(propertyName);
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public Optional<Integer> getMaxCount() {
        return Optional.ofNullable(maxCount);
    }

    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }

    public Optional<List<DataType>> getDateTypes() {
        return Optional.ofNullable(dateTypes);
    }

    public void setDateTypes(List<DataType> dateTypes) {
        this.dateTypes = dateTypes;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

    public MultiValueMap<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(MultiValueMap<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public Pageable getPageable() {
        return pageable;
    }

    public void setPageable(Pageable pageable) {
        this.pageable = pageable;
    }

    /**
     * Search context builder
     */
    public static SearchContext build(SearchType searchType, String engineType, HttpHeaders headers,
            MultiValueMap<String, String> queryParams, Pageable pageable) {
        SearchContext context = new SearchContext();
        context.setSearchType(searchType);
        context.setEngineType(engineType);
        context.setHeaders(headers);
        context.setQueryParams(queryParams);
        context.setPageable(pageable);
        return context;
    }

    /**
     * Fluent API
     */
    public SearchContext withUrn(UniformResourceName urn) {
        this.setUrn(urn);
        return this;
    }

    /**
     * Fluent API
     */
    public SearchContext withDatasetUrn(UniformResourceName datasetUrn) {
        this.datasetUrn = datasetUrn;
        return this;
    }

    /**
     * Fluent API
     */
    public SearchContext withExtra(String extra) {
        this.extra = extra;
        return this;
    }

    /**
     * Fluent API
     */
    public SearchContext withPropertyName(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    /**
     * Fluent API
     */
    public SearchContext withMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
        return this;
    }

    /**
     * Fluent API
     */
    public SearchContext withDataTypes(List<DataType> dataTypes) {
        this.dateTypes = dataTypes;
        return this;
    }
}
