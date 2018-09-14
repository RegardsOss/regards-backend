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
package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.description;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.metrics.stats.ParsedStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import feign.FeignException;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.rest.utils.HttpUtils;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.dam.client.models.IModelAttrAssocClient;
import fr.cnes.regards.modules.dam.domain.entities.StaticProperties;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.dam.domain.models.ModelAttrAssoc;
import fr.cnes.regards.modules.dam.domain.models.attributes.AttributeModel;
import fr.cnes.regards.modules.dam.domain.models.attributes.restriction.PatternRestriction;
import fr.cnes.regards.modules.dam.domain.models.attributes.restriction.RestrictionType;
import fr.cnes.regards.modules.indexer.domain.aggregation.QueryableAttribute;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.rest.SearchEngineController;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.Configuration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.EngineConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.ParameterConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.IOpenSearchExtension;
import fr.cnes.regards.modules.search.schema.ImageType;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.schema.QueryType;
import fr.cnes.regards.modules.search.schema.UrlType;
import fr.cnes.regards.modules.search.schema.parameters.OpenSearchParameter;
import fr.cnes.regards.modules.search.schema.parameters.OpenSearchParameterOption;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import fr.cnes.regards.modules.search.service.SearchException;

/**
 * Opensearch description.xml builder.
 * @author Sébastien Binda
 */
@Component
public class DescriptionBuilder {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DescriptionBuilder.class);

    public static final String OPENSEARCH_PAGINATION_COUNT = "count";

    public static final String OPENSEARCH_PAGINATION_PAGE = "startPage";

    /**
     * Workaround for Mizar implementation. As it does not use the value but the parameter name.
     */
    public static final String OPENSEARCH_PAGINATION_COUNT_NAME = "maxRecords";

    /**
     * Workaround for Mizar implementation. As it does not use the value but the parameter name.
     */
    public static final String OPENSEARCH_PAGINATION_PAGE_NAME = "page";

    /**
     * {@link IRuntimeTenantResolver} instance
     */
    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private ICatalogSearchService searchService;

    @Autowired
    private IResourceService resourceService;

    /**
     * {@link IModelAttrAssocClient} instance
     */
    @Autowired
    private IModelAttrAssocClient modelAttrAssocClient;

    @Autowired
    private IAttributeFinder finder;

    /**
     * Global configuration for description metadatas
     */
    @Autowired
    public Configuration configuration;

    /**
     * Build an OpenSearch descriptor for the current tenant(i.e. project) on the given path and entity type
     * @param context {@link SearchContext}
     * @param extensions {@link IOpenSearchExtension} extensions to use
     * @param parameterConfs {@link ParameterConfiguration}s parameters configuration.
     * @return {@link OpenSearchDescription}
     */
    public OpenSearchDescription build(SearchContext context, ICriterion criterion,
            List<IOpenSearchExtension> extensions, List<ParameterConfiguration> parameterConfs,
            EngineConfiguration engineConf, Optional<EntityFeature> dataset) throws ModuleException {

        // Retrieve informations about current projet
        String currentTenant = tenantResolver.getTenant();

        // Get all attributes for the given search type.
        List<DescriptionParameter> descParameters = getDescParameters(criterion, context, parameterConfs,
                                                                      currentTenant);

        // Build descriptor generic metadatas
        OpenSearchDescription desc = buildMetadata(engineConf, dataset);

        // Build query
        desc.getQuery().add(buildQuery(descParameters));

        // Build open search parameters to handle parameters extension
        List<OpenSearchParameter> parameters = buildParameters(descParameters, extensions);

        // Build urls
        Link searchLink = SearchEngineController.buildPaginationLink(resourceService, context, "search");
        desc.getUrl().add(buildUrl(parameters, searchLink.getHref(), MediaType.APPLICATION_ATOM_XML_VALUE,
                                   context.getQueryParams().isEmpty()));
        desc.getUrl().add(buildUrl(parameters, searchLink.getHref(), GeoJsonMediaType.APPLICATION_GEOJSON_VALUE,
                                   context.getQueryParams().isEmpty()));
        desc.getUrl().add(buildUrl(parameters, searchLink.getHref(), MediaType.APPLICATION_JSON_VALUE,
                                   context.getQueryParams().isEmpty()));

        // Apply active extensions to global description
        extensions.stream().filter(IOpenSearchExtension::isActivated).forEach(ext -> ext.applyToDescription(desc));

        return desc;
    }

    /**
     * Build metadata of the {@link OpenSearchDescription}
     * @param project {@link Project}
     * @param dataset
     * @param attributes {@link AttributeModel}s attributes
     * @return
     */
    private OpenSearchDescription buildMetadata(EngineConfiguration engineConf, Optional<EntityFeature> dataset) {
        OpenSearchDescription desc = new OpenSearchDescription();
        if (dataset.isPresent()) {
            desc.setDescription(dataset.get().getLabel());
            desc.setShortName(dataset.get().getLabel());
            desc.setLongName(dataset.get().getLabel());
            desc.setTags(String.join(" ", dataset.get().getTags()));
        } else {
            desc.setDescription(engineConf.getSearchDescription());
            desc.setShortName(engineConf.getShortName());
            desc.setLongName(engineConf.getLongName());
            desc.setTags(engineConf.getTags());
        }
        desc.setDeveloper(configuration.getDeveloper());
        desc.setAttribution(engineConf.getAttribution());
        desc.setAdultContent(Boolean.toString(configuration.isAdultContent()));
        desc.setLanguage(configuration.getLanguage());
        desc.setContact(engineConf.getContact());
        desc.setInputEncoding(StandardCharsets.UTF_8.displayName());
        desc.setOutputEncoding(StandardCharsets.UTF_8.displayName());

        if (engineConf.getImage() != null) {
            ImageType image = new ImageType();
            image.setValue(engineConf.getImage());
            desc.setImage(image);
        }
        return desc;
    }

    /**
     * Build an {@link UrlType} to add into the {@link OpenSearchDescription}
     * @param parameters {@link OpenSearchParameter}s parameters of the url
     * @param endpoint {@link String} endpoint of the search request
     * @param mediaType {@link String} MediaType of the search request response
     * @param queryDelimiter whether to inject "?" into template or not
     * @return {@link UrlType}
     */
    private UrlType buildUrl(List<OpenSearchParameter> parameters, String endpoint, String mediaType,
            boolean queryDelimiter) {
        UrlType url = new UrlType();
        url.getParameter().addAll(parameters);
        url.setRel(configuration.getUrlsRel());
        url.setType(mediaType);
        StringBuilder urlTemplateBuilder = new StringBuilder(endpoint);

        StringJoiner joiner = new StringJoiner("&");
        parameters.forEach(p -> joiner.add(String.format("%s=%s", p.getName(), p.getValue())));
        urlTemplateBuilder.append(queryDelimiter ? "?" : "&");
        urlTemplateBuilder.append(joiner);
        url.setTemplate(urlTemplateBuilder.toString());
        return url;
    }

    /**
     * Build a {@link QueryType} for the given {@link AttributeModel}s.
     * The generate query is an example to shox syntax of the global searchTerms
     * @param attributes {@link AttributeModel}s to handle in the query
     * @return {@link QueryType}
     */
    private QueryType buildQuery(List<DescriptionParameter> descParameters) {
        QueryType query = new QueryType();
        query.setRole("example");
        query.setSearchTerms(getSearchTermExample(descParameters.stream().map(dp -> dp.getAttributeModel())
                .collect(Collectors.toList())));
        return query;
    }

    /**
     * Build {@link OpenSearchParameter}s to add for the parameter extension in each {@link UrlType} of the
     * {@link OpenSearchDescription}
     * @param attributes {@link Map} {@link AttributeModel} / {@link QueryableAttribute}
     * @param extensions {@link IOpenSearchExtension}s to apply on parameters
     * @return generated {@link OpenSearchParameter}s
     */
    private List<OpenSearchParameter> buildParameters(List<DescriptionParameter> descParameters,
            List<IOpenSearchExtension> extensions) {
        List<OpenSearchParameter> parameters = Lists.newArrayList();

        // Add standard q parameter
        OpenSearchParameter qParameter = new OpenSearchParameter();
        qParameter.setTitle(configuration.getQueryParameterTitle());
        qParameter.setName(configuration.getQueryParameterName());
        qParameter.setValue(String.format("{%s}", configuration.getQueryParameterValue()));
        parameters.add(qParameter);

        for (DescriptionParameter descParameter : descParameters) {
            parameters.add(buildParameter(descParameter, extensions));
        }

        for (IOpenSearchExtension ext : extensions) {
            if (ext.isActivated()) {
                parameters.addAll(ext.addParametersToDescription());
            }
        }

        // Add pagination parameters
        OpenSearchParameter countParameter = new OpenSearchParameter();
        countParameter.setTitle("Number of results returned per page (default 20, max 1000)");
        countParameter.setName(OPENSEARCH_PAGINATION_COUNT_NAME);
        countParameter.setValue(String.format("{%s}", OPENSEARCH_PAGINATION_COUNT));
        countParameter.setMaxInclusive("1000");
        countParameter.setMinInclusive("1");
        parameters.add(countParameter);

        OpenSearchParameter startPageParameter = new OpenSearchParameter();
        startPageParameter.setTitle("Results page to return");
        startPageParameter.setName(OPENSEARCH_PAGINATION_PAGE_NAME);
        startPageParameter.setValue(String.format("{%s}", OPENSEARCH_PAGINATION_PAGE));
        startPageParameter.setMinInclusive("1");
        parameters.add(startPageParameter);

        return parameters;
    }

    /**
     * Build a {@link OpenSearchParameter} for a given {@link AttributeModel}
     * @param attribute {@link AttributeModel} to build parameter from.
     * @param queryableAtt {@link QueryableAttribute} attribute query informations.
     * @param extensions {@link IOpenSearchExtension} opensearch extensions to handle.
     * @return
     */
    private OpenSearchParameter buildParameter(DescriptionParameter descParameter,
            List<IOpenSearchExtension> extensions) {
        OpenSearchParameter parameter = new OpenSearchParameter();
        parameter.setName(descParameter.getName());
        parameter.setMinimum("0");
        parameter.setMaximum("1");
        parameter.setValue(String.format("{%s}", descParameter.getAttributeModel().getName()));
        parameter.setTitle(descParameter.getAttributeModel().getDescription());
        if ((descParameter.getAttributeModel().getRestriction() != null)
                && RestrictionType.PATTERN.equals(descParameter.getAttributeModel().getRestriction().getType())) {
            PatternRestriction restriction = (PatternRestriction) descParameter.getAttributeModel().getRestriction();
            parameter.setPattern(restriction.getPattern());
        }

        if ((descParameter.getQueryableAttribute().getAggregation() != null)) {
            if (descParameter.getQueryableAttribute().getAggregation() instanceof ParsedStringTerms) {
                ParsedStringTerms terms = (ParsedStringTerms) descParameter.getQueryableAttribute().getAggregation();
                terms.getBuckets().forEach(b -> {
                    OpenSearchParameterOption option = new OpenSearchParameterOption();
                    option.setValue(b.getKeyAsString());
                    parameter.getOption().add(option);
                });
            } else if (descParameter.getQueryableAttribute().getAggregation() instanceof ParsedStats) {
                ParsedStats stats = (ParsedStats) descParameter.getQueryableAttribute().getAggregation();
                parameter.setMinInclusive(stats.getMinAsString());
                parameter.setMaxInclusive(stats.getMaxAsString());
            }
        }

        for (IOpenSearchExtension ext : extensions) {
            if (ext.isActivated()) {
                ext.applyToDescriptionParameter(parameter, descParameter);
            }
        }
        return parameter;
    }

    /**
     * Retrieve all queryable attributes for the current search context.
     * For each attribute, this method return a couple {@link AttributeModel}/{@link QueryableAttribute}.
     * {@link AttributeModel} is the attribute definition (metadatas)
     * {@link QueryableAttribute} is the attribute available informations for query as boundaries for example.
     * @param criterion {@link ICriterion} search criterion
     * @param context {@link SearchContext}
     * @param parameterConfs {@link ParameterConfiguration} configured parameters.
     * @param pCurrentTenant {@link String} tenant or project.
     * @return {@link Map}<{@link AttributeModel}, {@link QueryableAttribute}>
     */
    private List<DescriptionParameter> getDescParameters(ICriterion criterion, SearchContext context,
            List<ParameterConfiguration> parameterConfs, String pCurrentTenant) throws ModuleException {

        List<DescriptionParameter> parameters = Lists.newArrayList();

        // For each attribute retrieve the QueryableAttribute informations
        List<QueryableAttribute> queryableAttributes = Lists.newArrayList();
        for (ModelAttrAssoc maa : getModelAttributes(context)) {
            maa.getAttribute().buildJsonPath(StaticProperties.FEATURE_PROPERTIES);
            Optional<ParameterConfiguration> conf = parameterConfs.stream()
                    .filter(pc -> pc.getAttributeModelJsonPath().equals(maa.getAttribute().getJsonPath())).findFirst();
            QueryableAttribute queryableAtt = createEmptyQueryableAttribute(maa.getAttribute(), conf);
            queryableAttributes.add(queryableAtt);
            parameters.add(new DescriptionParameter(finder.findName(maa.getAttribute()), maa.getAttribute(),
                    conf.orElse(null), queryableAtt));
        }
        try {
            // Run statistic search on each attributes. Results are set back into the QueryableAttributes parameter.
            searchService.retrievePropertiesStats(criterion, context.getSearchType(), queryableAttributes);
        } catch (SearchException e) {
            LOGGER.error("Error retrieving properties for each parameters of the OpenSearchDescription (parameter extension",
                         e);
        }

        return parameters;
    }

    private Collection<ModelAttrAssoc> getModelAttributes(SearchContext context) throws ModuleException {
        try {
            FeignSecurityManager.asSystem();
            // Retrieve all AttributeModel for the given searchType and dataset if any
            ResponseEntity<Collection<ModelAttrAssoc>> assocsResponse;
            if (context.getDatasetUrn().isPresent()) {
                assocsResponse = modelAttrAssocClient.getModelAttrAssocsForDataInDataset(context.getDatasetUrn().get());
            } else {
                assocsResponse = modelAttrAssocClient.getModelAttrAssocsFor(getEntityType(context.getSearchType()));
            }
            if (!HttpUtils.isSuccess(assocsResponse.getStatusCode())) {
                LOGGER.error("Trying to contact microservice responsible for Model but couldn't contact it");
                throw new ModuleException("Unable to contact model controller");
            } else {
                return assocsResponse.getBody();
            }
        } catch (FeignException e) {
            LOGGER.error("Cannot retrieve model attributes", e);
            throw new ModuleException(e);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    /**
     * Create an new empty {@link QueryableAttribute} object for the given {@link AttributeModel}
     * and the associated {@link ParameterConfiguration}
     * @param att {@link AttributeModel}
     * @param conf {@link ParameterConfiguration}
     * @return {@link QueryableAttribute}
     */
    private QueryableAttribute createEmptyQueryableAttribute(AttributeModel att,
            Optional<ParameterConfiguration> conf) {

        // Build full real path for the attribute in index.
        String name = att.getJsonPath();
        if (att.isDynamic()) {
            name = StaticProperties.FEATURE_NS + name;
        }

        // Set aggregation stats conf if present
        if (conf.isPresent()) {
            return new QueryableAttribute(name, null, att.isTextAttribute(), conf.get().getOptionsCardinality());
        } else {
            return new QueryableAttribute(name, null, att.isTextAttribute(), 0);
        }
    }

    /**
     * Convert {@link SearchType} to {@link EntityType}
     * @param searchType {@link SearchType}
     * @return {@link EntityType}
     */
    private EntityType getEntityType(SearchType searchType) {
        switch (searchType) {
            case COLLECTIONS:
                return EntityType.COLLECTION;
            case DATAOBJECTS:
                return EntityType.DATA;
            case DATASETS:
                return EntityType.DATASET;
            case DOCUMENTS:
                return EntityType.DOCUMENT;
            case ALL:
            case DATAOBJECTS_RETURN_DATASETS:
            default:
                throw new UnsupportedOperationException(
                        String.format("Unsupproted entity type for open search. %s", searchType.toString()));
        }
    }

    /**
     * Generate an example search query for searchTerms standard opensearch parameter.
     * @param pAttrs {@link AttributeModel}s to handle in query
     * @return {@link String} example query
     */
    private String getSearchTermExample(Collection<AttributeModel> pAttrs) {
        StringJoiner sj = new StringJoiner(" AND ");
        for (AttributeModel attr : pAttrs) {
            // result has the following format: "fullAttrName:value", except for arrays. Arrays are represented thanks
            // to multiple values: attr:val1 OR attr:val2 ...
            StringBuilder result = new StringBuilder();
            result.append(attr.getJsonPath()).append(":");
            switch (attr.getType()) {
                case BOOLEAN:
                    result.append("{boolean}");
                    break;
                case DATE_ARRAY:
                    result.append("{ISO-8601 date} OR ").append(attr.getJsonPath()).append(":")
                            .append("{ISO-8601 date}");
                    break;
                case DATE_INTERVAL:
                    result.append("[* TO  {ISO-8601 date} ]");
                    break;
                case DATE_ISO8601:
                    result.append("{ISO-8601 date}");
                    break;
                case DOUBLE:
                    result.append("{double value}");
                    break;
                case DOUBLE_ARRAY:
                    result.append("{double value} OR ").append(attr.getJsonPath()).append(":").append("{double value}");
                    break;
                case DOUBLE_INTERVAL:
                    result.append("[{double value} TO  {double value}]");
                    break;
                case LONG:
                    result.append("{long value}");
                    break;
                case INTEGER:
                    result.append("{integer value}");
                    break;
                case LONG_ARRAY:
                    result.append("{long value} OR ").append(attr.getJsonPath()).append(":").append("{long value}");
                    break;
                case INTEGER_ARRAY:
                    result.append("{integer value} OR ").append(attr.getJsonPath()).append(":")
                            .append("{integer value}");
                    break;
                case LONG_INTERVAL:
                    result.append("[{long value} TO  {long value}]");
                    break;
                case INTEGER_INTERVAL:
                    result.append("[{integer value} TO  {integer value}]");
                    break;
                case STRING:
                    result.append("{string}");
                    break;
                case STRING_ARRAY:
                    result.append("{string} OR ").append(attr.getJsonPath()).append(":").append("{string}");
                    break;
                case URL:
                    result.append("{url}");
                    break;
                default:
                    throw new IllegalArgumentException(
                            attr.getType() + " is not handled for open search descriptor generation");
            }
            sj.add(result.toString());
        }
        return sj.toString();
    }
}
