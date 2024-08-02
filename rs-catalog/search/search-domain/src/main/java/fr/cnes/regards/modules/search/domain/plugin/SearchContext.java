/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Search context for search engine.<br/>
 * Use {@link #build(SearchType, String, HttpHeaders, MultiValueMap, Pageable)} to initialize a new context.<br/>
 * Additional optional properties can be set using {@link #withDatasetUrn(UniformResourceName)},
 * {@link #withExtra(String)},
 * {@link #withUrn(UniformResourceName)}, {@link #withPropertyName(String)}, {@link #withMaxCount(Integer)},
 * {@link #withDataTypes(List)}.
 *
 * @author Marc Sordi
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
     * Optional type of request parser. The response formater is the given engineType.
     * If this parameter is null, so the parser is the same engine as the response formater.
     */
    private String engineRequestParserType;

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
    private final Set<String> propertyNames = Sets.newHashSet();

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
    private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

    /**
     * Indicates if the search context is a bound calculation or not.
     */
    private Boolean boundCalculation = false;

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

    public Set<String> getPropertyNames() {
        return this.propertyNames;
    }

    public void setPropertyNames(Collection<String> propertyNames) {
        this.propertyNames.addAll(propertyNames);
    }

    public void addPropertyName(String propertyName) {
        this.propertyNames.add(propertyName);
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
        this.queryParams.clear();
        this.queryParams.putAll(queryParams);
    }

    public Pageable getPageable() {
        return pageable;
    }

    public void setPageable(Pageable pageable) {
        this.pageable = pageable;
    }

    public Boolean getBoundCalculation() {
        return boundCalculation;
    }

    public void setBoundCalculation(Boolean boundCalculation) {
        this.boundCalculation = boundCalculation;
    }

    /**
     * Search context builder
     */
    public static SearchContext build(SearchType searchType,
                                      String engineType,
                                      HttpHeaders headers,
                                      MultiValueMap<String, String> queryParams,
                                      Pageable pageable) {
        SearchContext context = new SearchContext();
        context.setSearchType(searchType);
        context.setEngineType(engineType);
        context.setHeaders(headers);
        if (queryParams != null) {
            List<String> parser = queryParams.get(SearchEngineMappings.SEARCH_REQUEST_PARSER);
            if ((parser != null) && !parser.isEmpty()) {
                context.setEngineRequestParserType(parser.get(0));
            }
            // Filter spring pagination parameters if any
            MultiValueMap<String, String> queryParamsPaginationLess = new LinkedMultiValueMap<>();
            queryParamsPaginationLess.putAll(queryParams);
            // Remove authentication parameters if any
            queryParamsPaginationLess.remove("token");
            queryParamsPaginationLess.remove("scope");
            queryParamsPaginationLess.remove(SearchEngineMappings.SEARCH_REQUEST_PARSER);
            context.setQueryParams(queryParamsPaginationLess);
        }
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
        this.propertyNames.add(propertyName);
        return this;
    }

    public SearchContext withPropertyNames(Collection<String> propertyNames) {
        if ((propertyNames != null) && !propertyNames.isEmpty()) {
            this.propertyNames.addAll(propertyNames);
        }
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

    public String getEngineRequestParserType() {
        return engineRequestParserType;
    }

    public void setEngineRequestParserType(String engineType) {
        this.engineRequestParserType = engineType;
    }

    public SearchContext withBoundCalculation() {
        this.boundCalculation = true;
        return this;
    }

}
