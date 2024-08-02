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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.description;

import com.google.common.collect.Sets;
import feign.FeignException;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.utils.ResponseEntityUtils;
import fr.cnes.regards.modules.dam.client.entities.IDatasetClient;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.aggregation.QueryableAttribute;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.model.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.restriction.PatternRestriction;
import fr.cnes.regards.modules.model.domain.attributes.restriction.RestrictionType;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import fr.cnes.regards.modules.model.gson.AbstractAttributeHelper;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.parser.QueryParser;
import fr.cnes.regards.modules.opensearch.service.parser.UpdatedParser;
import fr.cnes.regards.modules.search.domain.plugin.IEntityLinkBuilder;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.schema.ImageType;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.schema.QueryType;
import fr.cnes.regards.modules.search.schema.UrlType;
import fr.cnes.regards.modules.search.schema.parameters.OpenSearchParameter;
import fr.cnes.regards.modules.search.schema.parameters.OpenSearchParameterOption;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.Configuration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.EngineConfiguration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.ParameterConfiguration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.IOpenSearchExtension;
import org.apache.commons.compress.utils.Lists;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.metrics.ParsedStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Opensearch description.xml builder.
 *
 * @author Sébastien Binda
 */
@Component
public class DescriptionBuilder {

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
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DescriptionBuilder.class);

    /**
     * Global configuration for description metadatas
     */
    @Autowired
    public Configuration configuration;

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
    private IDatasetClient datasetClient;

    @Autowired
    private IAttributeFinder finder;

    /**
     * Build an OpenSearch descriptor for the current tenant(i.e. project) on the given path and entity type
     *
     * @param context        {@link SearchContext}
     * @param extensions     {@link IOpenSearchExtension} extensions to use
     * @param parameterConfs {@link ParameterConfiguration}s parameters configuration.
     * @return {@link OpenSearchDescription}
     */
    public OpenSearchDescription build(SearchContext context,
                                       ICriterion criterion,
                                       List<IOpenSearchExtension> extensions,
                                       List<ParameterConfiguration> parameterConfs,
                                       EngineConfiguration engineConf,
                                       Optional<EntityFeature> dataset,
                                       IEntityLinkBuilder linkBuilder) throws ModuleException {

        // Get all attributes for the given search type.
        List<DescriptionParameter> descParameters = getDescParameters(criterion, context, parameterConfs);

        // Build descriptor generic metadatas
        OpenSearchDescription desc = buildMetadata(engineConf, dataset);

        // Build query
        desc.getQuery().add(buildQuery(descParameters));

        // Build open search parameters to handle parameters extension
        List<OpenSearchParameter> parameters = buildParameters(descParameters, extensions);

        // Build urls
        Link searchLink = linkBuilder.buildPaginationLink(resourceService, context, LinkRelation.of("search"));
        desc.getUrl()
            .add(buildUrl(parameters,
                          searchLink.getHref(),
                          MediaType.APPLICATION_ATOM_XML_VALUE,
                          context.getQueryParams().isEmpty()));
        desc.getUrl()
            .add(buildUrl(parameters,
                          searchLink.getHref(),
                          GeoJsonMediaType.APPLICATION_GEOJSON_VALUE,
                          context.getQueryParams().isEmpty()));
        desc.getUrl()
            .add(buildUrl(parameters,
                          searchLink.getHref(),
                          MediaType.APPLICATION_JSON_VALUE,
                          context.getQueryParams().isEmpty()));

        // Apply active extensions to global description
        extensions.stream().filter(IOpenSearchExtension::isActivated).forEach(ext -> ext.applyToDescription(desc));

        return desc;
    }

    /**
     * Build metadata of the {@link OpenSearchDescription}
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
     *
     * @param parameters     {@link OpenSearchParameter}s parameters of the url
     * @param endpoint       {@link String} endpoint of the search request
     * @param mediaType      {@link String} MediaType of the search request response
     * @param queryDelimiter whether to inject "?" into template or not
     * @return {@link UrlType}
     */
    private UrlType buildUrl(List<OpenSearchParameter> parameters,
                             String endpoint,
                             String mediaType,
                             boolean queryDelimiter) {
        UrlType url = new UrlType();
        url.getParameter().addAll(parameters);
        url.setRel(configuration.getUrlsRel());
        url.setType(mediaType);
        StringBuilder urlTemplateBuilder = new StringBuilder(endpoint);

        StringJoiner joiner = new StringJoiner("&");
        parameters.forEach(p -> joiner.add(String.format("%s=%s", p.getName(), p.getValue())));
        joiner.add(String.format("scope=%s", tenantResolver.getTenant()));
        urlTemplateBuilder.append(queryDelimiter ? "?" : "&");
        urlTemplateBuilder.append(joiner);
        url.setTemplate(urlTemplateBuilder.toString());
        return url;
    }

    /**
     * Build a {@link QueryType} for the given {@link AttributeModel}s.
     * The generate query is an example to shox syntax of the global searchTerms
     *
     * @return {@link QueryType}
     */
    private QueryType buildQuery(List<DescriptionParameter> descParameters) {
        QueryType query = new QueryType();
        query.setRole("example");
        query.setSearchTerms(getSearchTermExample(descParameters.stream()
                                                                .map(DescriptionParameter::getAttributeModel)
                                                                .collect(Collectors.toList())));
        return query;
    }

    /**
     * Build {@link OpenSearchParameter}s to add for the parameter extension in each {@link UrlType} of the
     * {@link OpenSearchDescription}
     *
     * @param extensions {@link IOpenSearchExtension}s to apply on parameters
     * @return generated {@link OpenSearchParameter}s
     */
    private List<OpenSearchParameter> buildParameters(List<DescriptionParameter> descParameters,
                                                      List<IOpenSearchExtension> extensions) {
        List<OpenSearchParameter> parameters = Lists.newArrayList();

        // Add standard q parameter
        OpenSearchParameter qParameter = new OpenSearchParameter();
        qParameter.setTitle(configuration.getQueryParameterTitle());
        qParameter.setName(QueryParser.QUERY_PARAMETER);
        qParameter.setValue(String.format("{%s}", configuration.getQueryParameterValue()));
        parameters.add(qParameter);

        for (DescriptionParameter descParameter : descParameters) {
            parameters.add(buildParameter(descParameter, extensions));
        }

        for (IOpenSearchExtension ext : extensions) {
            if (ext.isActivated()) {
                parameters.addAll(ext.getDescriptorBasicExtensionParameters());
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
        startPageParameter.setMinInclusive("0");
        parameters.add(startPageParameter);

        OpenSearchParameter updatedParameter = new OpenSearchParameter();
        updatedParameter.setTitle(
            "Filter features on updated field, return all features having or after the provided date");
        updatedParameter.setName(UpdatedParser.UPDATED_PARAMETER);
        updatedParameter.setValue(String.format("{%s}", UpdatedParser.UPDATED_PARAMETER));
        parameters.add(updatedParameter);

        return parameters;
    }

    /**
     * Build a {@link OpenSearchParameter} for a given {@link AttributeModel}
     */
    private OpenSearchParameter buildParameter(DescriptionParameter descParameter,
                                               List<IOpenSearchExtension> extensions) {
        OpenSearchParameter parameter = new OpenSearchParameter();
        if ((descParameter.getConfiguration() != null) && (descParameter.getConfiguration().getAllias() != null)) {
            parameter.setName(descParameter.getConfiguration().getAllias());
        } else {
            parameter.setName(descParameter.getName());
        }
        parameter.setMinimum("0");
        parameter.setMaximum("1");
        parameter.setValue(String.format("{%s}", descParameter.getAttributeModel().getName()));
        parameter.setTitle(descParameter.getAttributeModel().getDescription());
        if ((descParameter.getAttributeModel().getRestriction() != null)
            && RestrictionType.PATTERN.equals(descParameter.getAttributeModel().getRestriction().getType())) {
            PatternRestriction restriction = (PatternRestriction) descParameter.getAttributeModel().getRestriction();
            parameter.setPattern(restriction.getPattern());
        }

        if (descParameter.getQueryableAttribute().getAggregation() != null) {
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
                Optional<String> value = ext.getDescriptorParameterValue(descParameter);
                value.ifPresent(parameter::setValue);
            }
        }
        return parameter;
    }

    /**
     * Retrieve all queryable attributes for the current search context.
     * For each attribute, this method return a couple {@link AttributeModel}/{@link QueryableAttribute}.
     * {@link AttributeModel} is the attribute definition (metadatas)
     * {@link QueryableAttribute} is the attribute available informations for query as boundaries for example.
     *
     * @param criterion      {@link ICriterion} search criterion
     * @param context        {@link SearchContext}
     * @param parameterConfs {@link ParameterConfiguration} configured parameters.
     * @return {@link Map}<{@link AttributeModel}, {@link QueryableAttribute}>
     */
    private List<DescriptionParameter> getDescParameters(ICriterion criterion,
                                                         SearchContext context,
                                                         List<ParameterConfiguration> parameterConfs)
        throws ModuleException {

        List<DescriptionParameter> parameters = Lists.newArrayList();

        // For each attribute retrieve the QueryableAttribute informations
        Set<QueryableAttribute> queryableAttributes = Sets.newHashSet();
        for (AttributeModel att : getModelAttributes(context)) {
            if (att.isIndexed()) {
                Optional<ParameterConfiguration> conf = parameterConfs.stream()
                                                                      .filter(pc -> pc.getAttributeModelJsonPath()
                                                                                      .equals(att.getJsonPath()))
                                                                      .findFirst();
                QueryableAttribute queryableAtt = createEmptyQueryableAttribute(att, conf);
                if (!queryableAttributes.contains(queryableAtt)) {
                    queryableAttributes.add(queryableAtt);
                    parameters.add(new DescriptionParameter(finder.findName(att),
                                                            att,
                                                            conf.orElse(null),
                                                            queryableAtt));
                }
            } else {
                // The configuration of the attribute tells that it should not be queryable
                LOGGER.warn("The attribute {} is configured as not being indexed, it is ignored by opensearch",
                            att.getLabel());
            }
        }
        // Run statistic search on each attributes. Results are set back into the QueryableAttributes parameter.
        searchService.retrievePropertiesStats(criterion, context.getSearchType(), queryableAttributes);
        return parameters;
    }

    private Collection<AttributeModel> getModelAttributes(SearchContext context) throws ModuleException {
        try {
            FeignSecurityManager.asSystem();
            // Retrieve all AttributeModel for the given searchType and dataset if any
            ResponseEntity<Collection<ModelAttrAssoc>> assocsResponse;
            if (context.getDatasetUrn().isPresent()) {
                if (context.getDatasetUrn().get().isLast()) {
                    throw new ModuleException("You must use the dataset id(URN) and not the virtualId!");
                } else {
                    assocsResponse = datasetClient.getModelAttrAssocsForDataInDataset(context.getDatasetUrn().get());
                }
            } else {
                assocsResponse = modelAttrAssocClient.getModelAttrAssocsFor(getEntityType(context.getSearchType()));
            }

            Collection<ModelAttrAssoc> body = ResponseEntityUtils.extractBodyOrThrow(assocsResponse,
                                                                                     "An error occurred while trying to get model attributes: body is null");
            if (!assocsResponse.getStatusCode().is2xxSuccessful()) {
                LOGGER.error("Trying to contact microservice responsible for Model but couldn't contact it");
                throw new ModuleException("Unable to contact model controller");
            } else {
                List<AttributeModel> attributes = body.stream()
                                                      .map(ModelAttrAssoc::getAttribute)
                                                      .collect(Collectors.toList());
                attributes = AbstractAttributeHelper.computeAttributes(attributes);
                // Return computed attributes without specific JSON ones that are not queriable.
                return attributes.stream().filter(a -> a.getType() != PropertyType.JSON).collect(Collectors.toList());
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
     *
     * @param att  {@link AttributeModel}
     * @param conf {@link ParameterConfiguration}
     * @return {@link QueryableAttribute}
     */
    private QueryableAttribute createEmptyQueryableAttribute(AttributeModel att,
                                                             Optional<ParameterConfiguration> conf) {

        // Build full real path for the attribute in index.
        String name = att.getFullJsonPath();

        // Set aggregation stats conf if present
        if (conf.isPresent()) {
            return new QueryableAttribute(name,
                                          null,
                                          att.isTextAttribute(),
                                          conf.get().getOptionsCardinality(),
                                          att.isBooleanAttribute());
        } else {
            return new QueryableAttribute(name, null, att.isTextAttribute(), 0, att.isBooleanAttribute());
        }
    }

    /**
     * Convert {@link SearchType} to {@link EntityType}
     *
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
            case ALL:
            case DATAOBJECTS_RETURN_DATASETS:
            default:
                throw new UnsupportedOperationException(String.format("Unsupproted entity type for open search. %s",
                                                                      searchType));
        }
    }

    /**
     * Generate an example search query for searchTerms standard opensearch parameter.
     *
     * @param pAttrs {@link AttributeModel}s to handle in query
     * @return {@link String} example query
     */
    @SuppressWarnings("java:S1541") // Cyclomatic complexity too high
    private String getSearchTermExample(Collection<AttributeModel> pAttrs) {
        StringJoiner sj = new StringJoiner(" AND ");
        for (AttributeModel attr : pAttrs) {
            // result has the following format: "fullAttrName:value", except for arrays. Arrays are represented thanks
            // to multiple values: attr:val1 OR attr:val2 ...
            StringBuilder result = new StringBuilder();
            switch (attr.getType()) {
                case BOOLEAN:
                    result.append(attr.getFullJsonPath()).append(":");
                    result.append("{boolean}");
                    break;
                case DATE_ARRAY:
                    result.append(attr.getFullJsonPath()).append(":");
                    result.append("{ISO-8601 date} OR ")
                          .append(attr.getFullJsonPath())
                          .append(":")
                          .append("{ISO-8601 date}");
                    break;
                case DATE_RANGE:
                case DATE_INTERVAL:
                    result.append(attr.getFullJsonPath()).append(":");
                    result.append("[* TO  {ISO-8601 date} ]");
                    break;
                case DATE_ISO8601:
                    result.append(attr.getFullJsonPath()).append(":");
                    result.append("{ISO-8601 date}");
                    break;
                case DOUBLE:
                    result.append(attr.getFullJsonPath()).append(":");
                    result.append("{double value}");
                    break;
                case DOUBLE_ARRAY:
                    result.append(attr.getFullJsonPath()).append(":");
                    result.append("{double value} OR ").append(attr.getJsonPath()).append(":").append("{double value}");
                    break;
                case DOUBLE_RANGE:
                case DOUBLE_INTERVAL:
                    result.append(attr.getFullJsonPath()).append(":");
                    result.append("[{double value} TO  {double value}]");
                    break;
                case LONG:
                    result.append(attr.getFullJsonPath()).append(":");
                    result.append("{long value}");
                    break;
                case INTEGER:
                    result.append(attr.getFullJsonPath()).append(":");
                    result.append("{integer value}");
                    break;
                case LONG_ARRAY:
                    result.append(attr.getFullJsonPath()).append(":");
                    result.append("{long value} OR ").append(attr.getJsonPath()).append(":").append("{long value}");
                    break;
                case INTEGER_ARRAY:
                    result.append(attr.getFullJsonPath()).append(":");
                    result.append("{integer value} OR ")
                          .append(attr.getJsonPath())
                          .append(":")
                          .append("{integer value}");
                    break;
                case LONG_RANGE:
                case LONG_INTERVAL:
                    result.append(attr.getFullJsonPath()).append(":");
                    result.append("[{long value} TO  {long value}]");
                    break;
                case INTEGER_RANGE:
                case INTEGER_INTERVAL:
                    result.append(attr.getFullJsonPath()).append(":");
                    result.append("[{integer value} TO  {integer value}]");
                    break;
                case STRING:
                    result.append(attr.getFullJsonPath()).append(":");
                    result.append("{string}");
                    break;
                case STRING_ARRAY:
                    result.append(attr.getFullJsonPath()).append(":");
                    result.append("{string} OR ").append(attr.getJsonPath()).append(":").append("{string}");
                    break;
                case URL:
                    result.append(attr.getFullJsonPath()).append(":");
                    result.append("{url}");
                    break;
                case JSON:
                    // Do not handle JSON Attributes as simple attributes
                    break;
                default:
                    throw new IllegalArgumentException(attr.getType()
                                                       + " is not handled for open search descriptor generation");
            }
            sj.add(result.toString());
        }
        return sj.toString();
    }
}
