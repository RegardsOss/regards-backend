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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.dam.domain.entities.StaticProperties;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.dto.properties.DateProperty;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.opensearch.service.parser.QueryParser;
import fr.cnes.regards.modules.search.domain.PropertyBound;
import fr.cnes.regards.modules.search.domain.plugin.*;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.service.IBusinessSearchService;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import fr.cnes.regards.modules.search.service.engine.plugin.legacy.LegacySearchEngine;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.description.DescriptionBuilder;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.exception.ExtensionException;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.exception.UnsupportedMediaTypesException;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.SearchParameter;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.geo.GeoTimeExtension;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.media.MediaExtension;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.regards.RegardsExtension;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.IResponseFormatter;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.AtomResponseFormatter;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.geojson.GeojsonResponseFormatter;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.elasticsearch.common.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * OpenSearch engine plugin
 * @author Marc Sordi
 * @author Sébastien Binda
 */
@Plugin(id = OpenSearchEngine.ENGINE_ID, author = "REGARDS Team", contact = "regards@c-s.fr",
        description = "Native search engine", license = "GPLv3", owner = "CSSI", url = "https://github.com/RegardsOss",
        version = "1.0.0", markdown = "OpensearchEngine.md")
public class OpenSearchEngine implements ISearchEngine<Object, OpenSearchDescription, Object, List<String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchEngine.class);

    public static final String ENGINE_ID = "opensearch";

    public static final String TIME_EXTENSION_PARAMETER = "timeExtension";

    public static final String REGARDS_EXTENSION_PARAMETER = "regardsExtension";

    public static final String MEDIA_EXTENSION_PARAMETER = "mediaExtension";

    public static final String EXTRA_DESCRIPTION = "opensearchdescription.xml";

    public static final String PARAMETERS_CONFIGURATION = "parametersConfiguration";

    public static final String ENGINE_PARAMETERS = "engineConfiguration";

    private static final int SEARCH_PAGE_SIZE_LIMIT = 500;

    /**
     * Query parser
     */
    @Autowired
    protected IAttributeFinder finder;

    @Autowired
    private DescriptionBuilder descriptionBuilder;

    /**
     * Business search service
     */
    @Autowired
    protected IBusinessSearchService searchService;

    @Autowired
    private Gson gson;

    @Autowired
    private Configuration configuration;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    protected ICatalogSearchService catalogSearchService;

    @PluginParameter(name = ENGINE_PARAMETERS, label = "Search engine global configuration")
    private EngineConfiguration engineConfiguration;

    /**
     * To build resource links
     */
    @Autowired
    private IResourceService resourceService;

    @PluginParameter(name = TIME_EXTENSION_PARAMETER, label = "Open search time extension")
    private GeoTimeExtension timeExtension;

    @PluginParameter(name = REGARDS_EXTENSION_PARAMETER, label = "Open search regards extension")
    private RegardsExtension regardsExtension;

    @PluginParameter(name = MEDIA_EXTENSION_PARAMETER, label = "Open search media extension")
    private MediaExtension mediaExtension;

    @PluginParameter(name = PARAMETERS_CONFIGURATION, label = "Parameters configuration", optional = true,
            markdown = "OpensearchParameter.md")
    private List<ParameterConfiguration> paramConfigurations = Lists.newArrayList();

    @PluginInit
    public void init() {
        if (paramConfigurations == null) {
            paramConfigurations = Lists.newArrayList();
        }
    }

    @Override
    public boolean supports(SearchType searchType) {
        return true;
    }

    @Override
    public ResponseEntity<Object> search(SearchContext context, ISearchEngine<?, ?, ?, ?> parser,
            IEntityLinkBuilder linkBuilder) throws ModuleException {
        FacetPage<EntityFeature> facetPage = searchService.search(parser.parse(context), context.getSearchType(), null,
                                                                  getPagination(context));
        return ResponseEntity.ok(formatResponse(facetPage, context, linkBuilder));
    }

    @Override
    public ResponseEntity<Object> getEntity(SearchContext context, IEntityLinkBuilder linkBuilder)
            throws ModuleException {
        // Retrieve entity
        EntityFeature entity = searchService.get(context.getUrn().get());
        // add fake pagination for whatever reason it seems we have to response a list and not a single item....
        context.setPageable(PageRequest.of(0, 1));
        FacetPage<EntityFeature> facetPage = new FacetPage<>(Collections.singletonList(entity), Sets.newHashSet(),
                getPagination(context), 1);
        return ResponseEntity.ok(formatResponse(facetPage, context, linkBuilder));
    }

    public ICriterion parse(MultiValueMap<String, String> queryParams) throws ModuleException {
        // First parse q parameter for searchTerms if any.
        QueryParser queryParser = new QueryParser(finder);
        ICriterion searchTermsCriterion = queryParser.parse(queryParams);
        // Then parse all parameters (open search parameters extension)
        return ICriterion.and(searchTermsCriterion, parseParametersExt(queryParams));
    }

    @Override
    public ResponseEntity<OpenSearchDescription> extra(SearchContext context, IEntityLinkBuilder linkBuilder)
            throws ModuleException {
        if (context.getExtra().isPresent() && context.getExtra().get().equalsIgnoreCase(EXTRA_DESCRIPTION)) {

            // If the descriptor is asked for a specific dataset, first get the dataset.
            // The dataset will be used to set specific metadata into the descriptor like title, tags, ...
            Optional<EntityFeature> dataset = Optional.empty();
            if (context.getDatasetUrn().isPresent()) {
                // Search dataset entity
                dataset = Optional.of(searchService.get(context.getDatasetUrn().get()));
            }

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE);
            return new ResponseEntity<>(
                    descriptionBuilder.build(context, parse(context),
                                             Arrays.asList(mediaExtension, regardsExtension, timeExtension),
                                             paramConfigurations, engineConfiguration, dataset, linkBuilder),
                    headers, HttpStatus.OK);
        } else {
            return ISearchEngine.super.extra(context, linkBuilder);
        }
    }

    /**
     * Parse request parameters and add dataset context if necessary
     */
    @Override
    public ICriterion parse(SearchContext context) throws ModuleException {
        // Convert parameters to business criterion
        ICriterion criterion = parse(context.getQueryParams());
        // Manage dataset URN path parameter as criterion
        if (context.getDatasetUrn().isPresent()) {
            criterion = ICriterion
                    .and(criterion,
                         ICriterion.eq(StaticProperties.FEATURE_TAGS_PATH, context.getDatasetUrn().get().toString(), StringMatchType.KEYWORD));
        }
        return criterion;
    }

    /**
     * Format search response for the given {@link MediaType} in the {@link SearchContext}
     * @param page search response
     * @param context {@link SearchContext} containing MediaType
     * @return formatted response
     * @throws UnsupportedMediaTypesException from {@link #getBuilder(SearchContext)}
     */
    private Object formatResponse(FacetPage<EntityFeature> page, SearchContext context, IEntityLinkBuilder linkBuilder)
            throws UnsupportedMediaTypesException {
        IResponseFormatter<?> builder = getBuilder(context);
        builder.addMetadata(UUID.randomUUID().toString(), engineConfiguration, linkBuilder
                .buildExtraLink(resourceService, context, IanaLinkRelations.SELF, EXTRA_DESCRIPTION).getHref(), context,
                            configuration, page, linkBuilder.buildPaginationLinks(resourceService, page, context));
        page.getContent()
                .forEach(e -> builder.addEntity(e, getEntityLastUpdateDate(e), paramConfigurations,
                                                linkBuilder.buildEntityLinks(resourceService, context, e)));
        return builder.build();
    }

    /**
     * Retrieve the last update date of the given entity.
     * @param entity {@link EntityFeature}
     * @return Optional<OffsetDateTime>
     */
    private Optional<OffsetDateTime> getEntityLastUpdateDate(EntityFeature entity) {
        Optional<OffsetDateTime> date = Optional.empty();
        if (engineConfiguration.getEntityLastUpdateDatePropertyPath() != null) {
            IProperty<?> dateAttribute = entity.getProperty(engineConfiguration.getEntityLastUpdateDatePropertyPath());
            if (dateAttribute instanceof DateProperty) {
                DateProperty dateAttr = (DateProperty) dateAttribute;
                return Optional.ofNullable(dateAttr.getValue());
            }
        }
        return date;
    }

    /**
     * Parse openSearch query to find all parameters from standard open search parameters extension.
     */
    private ICriterion parseParametersExt(MultiValueMap<String, String> queryParams) throws ExtensionException {
        // Find AttributeModel for each parameter
        List<SearchParameter> attributes = buildParameters(queryParams);

        return ICriterion.and(timeExtension.buildCriterion(attributes), mediaExtension.buildCriterion(attributes),
                              regardsExtension.buildCriterion(attributes));
    }

    private Pair<AttributeModel, ParameterConfiguration> getParameterAttribute(String queryParam)
            throws OpenSearchUnknownParameter {
        String attributePath;
        ParameterConfiguration conf;
        // Check if parameter key is an alias from configuration
        Optional<ParameterConfiguration> aliasConf = paramConfigurations.stream()
                .filter(p -> queryParam.equals(p.getAllias())).findFirst();
        if (aliasConf.isPresent()) {
            // If it is an alias retrieve regards parameter path from the configuration
            conf = aliasConf.get();
            attributePath = conf.getAttributeModelJsonPath();
        } else {
            // If not retrieve regards parameter path
            attributePath = queryParam;
            // Search configuration if any
            conf = paramConfigurations.stream().filter(p -> p.getAttributeModelJsonPath().equals(attributePath))
                    .findFirst().orElse(null);
        }
        return Pair.of(finder.findByName(attributePath), conf);
    }

    /**
     * Build {@link SearchParameter}s by reading given queryParams.
     * @param queryParams Map key=parameter name value=parameter value.
     * @return {@link SearchParameter}s
     */
    private List<SearchParameter> buildParameters(MultiValueMap<String, String> queryParams) {
        List<SearchParameter> searchParameters = Lists.newArrayList();
        for (Entry<String, List<String>> queryParam : queryParams.entrySet()) {
            try {
                // Ignore special query parameter (q) or empty values
                if (!queryParam.getKey().equals(configuration.getQueryParameterName())
                        && ((queryParam.getValue().size() != 1)
                                || !Strings.isNullOrEmpty(queryParam.getValue().get(0)))) {
                    Pair<AttributeModel, ParameterConfiguration> attributeConf = getParameterAttribute(queryParam
                            .getKey());
                    searchParameters.add(new SearchParameter(queryParam.getKey(), attributeConf.getLeft(),
                            attributeConf.getRight(), queryParam.getValue()));
                }
            } catch (OpenSearchUnknownParameter e) {
                LOGGER.warn("Parameter not found in REGARDS models attributes. Cause : {}", e.getMessage());
                LOGGER.trace(e.getMessage(), e);
                // Adding unknown parameters in search parameters in case an IOpenSearchExtension can handle it.
                searchParameters.add(new SearchParameter(queryParam.getKey(), null, null, queryParam.getValue()));
            }
        }
        return searchParameters;
    }

    /**
     * Retrieve a response builder from existing ones matching the {@link MediaType} from the {@link SearchContext}
     * @param context {@link SearchContext}
     * @return {@link IResponseFormatter}
     * @throws UnsupportedMediaTypesException when asked media type is not handled
     */
    private IResponseFormatter<?> getBuilder(SearchContext context) throws UnsupportedMediaTypesException {
        IResponseFormatter<?> responseBuilder;

        if (context.getHeaders().getAccept().stream().anyMatch(MediaType.APPLICATION_JSON::isCompatibleWith)) {
            responseBuilder = new GeojsonResponseFormatter(authResolver.getToken());
        } else if (context.getHeaders().getAccept().stream()
                .anyMatch(MediaType.APPLICATION_ATOM_XML::isCompatibleWith)) {
            responseBuilder = new AtomResponseFormatter(gson, authResolver.getToken());
        } else {
            throw new UnsupportedMediaTypesException(context.getHeaders().getAccept());
        }
        responseBuilder.addExtension(timeExtension);
        responseBuilder.addExtension(mediaExtension);
        responseBuilder.addExtension(regardsExtension);
        return responseBuilder;
    }

    @Override
    public ResponseEntity<List<String>> getPropertyValues(SearchContext context) throws ModuleException {
        // Convert parameters to business criterion considering dataset
        ICriterion criterion = parse(context);
        // Extract optional request parameters
        String partialText = context.getQueryParams().getFirst(LegacySearchEngine.PARTIAL_TEXT);
        // Do business search
        List<String> values = searchService.retrieveEnumeratedPropertyValues(criterion, context.getSearchType(),
                                                                             context.getPropertyNames().stream()
                                                                                     .findFirst().get(),
                                                                             context.getMaxCount().get(), partialText);
        // Build response
        return ResponseEntity.ok(values);
    }

    @Override
    public ResponseEntity<DocFilesSummary> getSummary(SearchContext context) throws ModuleException {
        // Convert parameters to business criterion considering dataset
        ICriterion criterion = parse(context);
        // Compute summary
        DocFilesSummary summary = searchService.computeDatasetsSummary(criterion, context.getSearchType(),
                                                                       context.getDatasetUrn().orElse(null),
                                                                       context.getDateTypes().get());
        // Build response
        return ResponseEntity.ok(summary);
    }

    /**
     * Try to read pagination parameters from :<ul>
     * <li>1. From opensearch specific parameters 'count' & 'startPage'. Startpage is from 0 to X where X is last page.</li>
     * <li>2. From spring standard parameters 'size' & 'page'
     * </ul>
     */
    private Pageable getPagination(SearchContext context) {
        List<String> count = context.getQueryParams().get(DescriptionBuilder.OPENSEARCH_PAGINATION_COUNT_NAME);
        List<String> startPage = context.getQueryParams().get(DescriptionBuilder.OPENSEARCH_PAGINATION_PAGE_NAME);

        int size = context.getPageable().getPageSize();
        if ((count != null) && (count.size() == 1)) {
            try {
                size = Integer.parseInt(count.get(0));
                if (size > SEARCH_PAGE_SIZE_LIMIT) {
                    size = SEARCH_PAGE_SIZE_LIMIT;
                }
            } catch (NumberFormatException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        int start = context.getPageable().getPageNumber();
        if ((startPage != null) && (startPage.size() == 1)) {
            try {
                start = Integer.parseInt(startPage.get(0));
            } catch (NumberFormatException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        // Build sort parameters from parameter configuration
        List<Order> orders = Lists.newArrayList();
        context.getPageable().getSort().get().forEach(order -> {
            try {
                if (order.isAscending()) {
                    orders.add(Order.asc(getParameterAttribute(order.getProperty()).getLeft().getFullJsonPath()));
                } else {
                    orders.add(Order.desc(getParameterAttribute(order.getProperty()).getLeft().getFullJsonPath()));
                }
            } catch (OpenSearchUnknownParameter e) {
                // Nothing to do
                LOGGER.info("Sort parameter invalid {}", order.getProperty(), e);
            }
        });

        return PageRequest.of(start, size, Sort.by(orders));
    }

    @Override
    public ResponseEntity<List<EntityModel<PropertyBound<?>>>> getPropertiesBounds(SearchContext context)
            throws ModuleException {
        List<PropertyBound<?>> bounds = catalogSearchService
                .retrievePropertiesBounds(context.getPropertyNames(), parse(context), context.getSearchType());
        return ResponseEntity.ok(bounds.stream().map(EntityModel<PropertyBound<?>>::new)
                .collect(Collectors.toList()));
    }

    @Override
    public List<Link> extraLinks(Class<?> searchEngineControllerClass, SearchEngineConfiguration element) {
        List<Link> result = new ArrayList<>();
        String datasetUrn = element.getDatasetUrn();
        if (datasetUrn != null) {
            result.add(resourceService.buildLink(searchEngineControllerClass, "searchSingleDatasetExtra",
                                                 LinkRelation.of(EXTRA_DESCRIPTION),
                                                 MethodParamFactory.build(String.class,
                                                                          element.getConfiguration().getPluginId()),
                                                 MethodParamFactory.build(String.class, datasetUrn),
                                                 MethodParamFactory.build(String.class, EXTRA_DESCRIPTION),
                                                 MethodParamFactory.build(HttpHeaders.class),
                                                 MethodParamFactory.build(MultiValueMap.class),
                                                 MethodParamFactory.build(Pageable.class)));
        } else {
            result.add(resourceService.buildLink(searchEngineControllerClass, "searchAllDataobjectsExtra",
                                                 LinkRelation.of(EXTRA_DESCRIPTION),
                                                 MethodParamFactory.build(String.class,
                                                                          element.getConfiguration().getPluginId()),
                                                 MethodParamFactory.build(String.class, EXTRA_DESCRIPTION),
                                                 MethodParamFactory.build(HttpHeaders.class),
                                                 MethodParamFactory.build(MultiValueMap.class),
                                                 MethodParamFactory.build(Pageable.class)));
        }
        return result;
    }
}
