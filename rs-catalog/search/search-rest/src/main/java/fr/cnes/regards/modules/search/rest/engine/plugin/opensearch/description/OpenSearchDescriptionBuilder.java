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

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.StringJoiner;

import org.apache.commons.compress.utils.Lists;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.metrics.stats.ParsedStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import feign.FeignException;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.utils.HttpUtils;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.entities.domain.StaticProperties;
import fr.cnes.regards.modules.indexer.domain.aggregation.QueryableAttribute;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.models.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.models.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.attributes.restriction.PatternRestriction;
import fr.cnes.regards.modules.models.domain.attributes.restriction.RestrictionType;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.rest.SearchEngineController;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchParameterConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.IOpenSearchExtension;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.schema.OpenSearchParameter;
import fr.cnes.regards.modules.search.schema.OpenSearchParameterOption;
import fr.cnes.regards.modules.search.schema.QueryType;
import fr.cnes.regards.modules.search.schema.UrlType;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import fr.cnes.regards.modules.search.service.SearchException;

/**
 * Opensearch description.xml builder.
 * @author Sébastien Binda
 */
@Component
public class OpenSearchDescriptionBuilder {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchDescriptionBuilder.class);

    /**
     * microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * {@link IProjectsClient} instance
     */
    @Autowired
    private IProjectsClient projectClient;

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

    /**
     * Global configuration for description metadatas
     */
    @Autowired
    public OpenSearchConfiguration configuration;

    /**
     * Build an OpenSearch descriptor for the current tenant(i.e. project) on the given path and entity type
     * @param context {@link SearchContext}
     * @param extensions {@link IOpenSearchExtension} extensions to use
     * @param parameterConfs {@link OpenSearchParameterConfiguration}s parameters configuration.
     * @return {@link OpenSearchDescription}
     * @throws UnsupportedEncodingException
     */
    public OpenSearchDescription build(SearchContext context, ICriterion criterion,
            List<IOpenSearchExtension> extensions, List<OpenSearchParameterConfiguration> parameterConfs) {

        // Retrieve informations about current projet
        String currentTenant = tenantResolver.getTenant();
        Project project = getProject(currentTenant);

        // Get all attributes for the given search type.
        Map<AttributeModel, QueryableAttribute> attributes = retreiveSearchContextAttributes(criterion,
                                                                                             context.getSearchType(),
                                                                                             parameterConfs,
                                                                                             currentTenant);

        // Build descriptor generic metadatas
        OpenSearchDescription desc = buildMetadata(project);

        // Build query
        desc.getQuery().add(buildQuery(attributes.keySet()));

        // Build open search parameters to handle parameters extension
        List<OpenSearchParameter> parameters = buildParameters(attributes, extensions);

        // Build urls
        Link searchLink = SearchEngineController.buildPaginationLink(resourceService, context, "search");
        desc.getUrl().add(buildUrl(project, parameters, searchLink.getHref(), MediaType.APPLICATION_ATOM_XML_VALUE));
        desc.getUrl().add(buildUrl(project, parameters, searchLink.getHref(), MediaType.APPLICATION_JSON_VALUE));

        // Apply active extensions to global description
        extensions.stream().filter(IOpenSearchExtension::isActivated)
                .forEach(ext -> ext.applyExtensionToDescription(desc));

        return desc;
    }

    /**
     * Build metadata of the {@link OpenSearchDescription}
     * @param project {@link Project}
     * @param attributes {@link AttributeModel}s attributes
     * @return
     */
    private OpenSearchDescription buildMetadata(Project project) {
        OpenSearchDescription desc = new OpenSearchDescription();
        desc.setDescription(String.format(project.getDescription()));
        desc.setShortName(String.format(project.getName()));
        desc.setLongName(String.format(project.getName()));
        desc.setDeveloper(configuration.getDeveloper());
        desc.setAttribution(configuration.getAttribution());
        desc.setAdultContent(Boolean.toString(configuration.isAdultContent()));
        desc.setLanguage(configuration.getLanguage());
        desc.setContact(configuration.getContactEmail());
        desc.setInputEncoding(StandardCharsets.UTF_8.displayName());
        desc.setOutputEncoding(StandardCharsets.UTF_8.displayName());
        return desc;
    }

    /**
     * Build an {@link UrlType} to add into the {@link OpenSearchDescription}
     * @param project {@link Project}
     * @param parameters {@link OpenSearchParameter}s parameters of the url
     * @param endpoint {@link String} endpoint of the search request
     * @param mediaType {@link String} MediaType of the search request response
     * @return {@link UrlType}
     */
    private UrlType buildUrl(Project project, List<OpenSearchParameter> parameters, String endpoint, String mediaType) {
        UrlType url = new UrlType();
        url.getParameter().addAll(parameters);
        url.setRel(configuration.getUrlsRel());
        url.setType(mediaType);
        StringBuilder urlTemplateBuilder = new StringBuilder(endpoint);
        urlTemplateBuilder.append(String.format("?%s={%s}", configuration.getQueryParameterName(),
                                                configuration.getQueryParameterValue()));
        parameters.stream().forEach(p -> urlTemplateBuilder.append(String.format("&%s=%s", p.getName(), p.getValue())));
        url.setTemplate(urlTemplateBuilder.toString());
        return url;
    }

    /**
     * Build a {@link QueryType} for the given {@link AttributeModel}s.
     * The generate query is an example to shox syntax of the global searchTerms
     * @param attributes {@link AttributeModel}s to handle in the query
     * @return {@link QueryType}
     */
    private QueryType buildQuery(Collection<AttributeModel> attributes) {
        QueryType query = new QueryType();
        query.setRole("example");
        query.setSearchTerms(getSearchTermExample(attributes));
        return query;
    }

    /**
     * Build {@link OpenSearchParameter}s to add for the parameter extension in each {@link UrlType} of the {@link OpenSearchDescription}
     * @param attributes {@link Map} {@link AttributeModel} / {@link QueryableAttribute}
     * @param extensions {@link IOpenSearchExtension}s to apply on parameters
     * @return generated {@link OpenSearchParameter}s
     */
    private List<OpenSearchParameter> buildParameters(Map<AttributeModel, QueryableAttribute> attributes,
            List<IOpenSearchExtension> extensions) {
        List<OpenSearchParameter> parameters = Lists.newArrayList();

        // Add standard q parameter
        OpenSearchParameter qParameter = new OpenSearchParameter();
        qParameter.setTitle(configuration.getQueryParameterTitle());
        qParameter.setName(configuration.getQueryParameterName());
        qParameter.setValue(configuration.getQueryParameterValue());
        parameters.add(qParameter);

        for (Entry<AttributeModel, QueryableAttribute> parameter : attributes.entrySet()) {
            parameters.add(buildParameter(parameter.getKey(), parameter.getValue(), extensions));
        }
        return parameters;
    }

    /**
     * Build a {@link OpenSearchParameter} for a given {@link AttributeModel}
     * @param attribute {@link AttributeModel} to build parameter from.
     * @param queryableAtt {@link QueryableAttribute} attribute query informations.
     * @param extensions {@link IOpenSearchExtension} opensearch extensions to handle.
     * @return
     */
    private OpenSearchParameter buildParameter(AttributeModel attribute, QueryableAttribute queryableAtt,
            List<IOpenSearchExtension> extensions) {
        OpenSearchParameter parameter = new OpenSearchParameter();
        parameter.setName(attribute.getJsonPath());
        if (attribute.isOptional()) {
            parameter.setMinimum("0");
        } else {
            parameter.setMinimum("1");
        }
        parameter.setMaximum("1");
        parameter.setValue(String.format("{%s}", attribute.getName()));
        parameter.setTitle(attribute.getDescription());
        if ((attribute.getRestriction() != null)
                && RestrictionType.PATTERN.equals(attribute.getRestriction().getType())) {
            PatternRestriction restriction = (PatternRestriction) attribute.getRestriction();
            parameter.setPattern(restriction.getPattern());
        }

        if ((queryableAtt.getAggregation() != null)) {
            if (queryableAtt.getAggregation() instanceof ParsedStringTerms) {
                ParsedStringTerms terms = (ParsedStringTerms) queryableAtt.getAggregation();
                terms.getBuckets().forEach(b -> {
                    OpenSearchParameterOption option = new OpenSearchParameterOption();
                    option.setValue(b.getKeyAsString());
                    parameter.getOption().add(option);
                });
            } else if (queryableAtt.getAggregation() instanceof ParsedStats) {
                ParsedStats stats = (ParsedStats) queryableAtt.getAggregation();
                parameter.setMinInclusive(stats.getMinAsString());
                parameter.setMaxInclusive(stats.getMaxAsString());
            }
        }

        for (IOpenSearchExtension ext : extensions) {
            if (ext.isActivated()) {
                ext.applyExtensionToDescriptionParameter(parameter);
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
     * @param searchType {@link SearchType}
     * @param parameterConfs {@link OpenSearchParameterConfiguration} configured parameters.
     * @param pCurrentTenant {@link String} tenant or project.
     * @return {@link Map}<{@link AttributeModel}, {@link QueryableAttribute}>
     */
    private Map<AttributeModel, QueryableAttribute> retreiveSearchContextAttributes(ICriterion criterion,
            SearchType searchType, List<OpenSearchParameterConfiguration> parameterConfs, String pCurrentTenant) {

        Map<AttributeModel, QueryableAttribute> attributes = Maps.newHashMap();

        tenantResolver.forceTenant(pCurrentTenant);
        FeignSecurityManager.asSystem();
        try {
            // Retrieve all AttributeModel fot the given searchType
            ResponseEntity<Collection<ModelAttrAssoc>> assocsResponse = modelAttrAssocClient
                    .getModelAttrAssocsFor(getEntityType(searchType));
            FeignSecurityManager.reset();
            if (!HttpUtils.isSuccess(assocsResponse.getStatusCode())) {
                LOGGER.error("Trying to contact microservice responsible for Model but couldn't contact it");
            }

            // For each attribute retrieve the QueryableAttribute informations
            for (ModelAttrAssoc maa : assocsResponse.getBody()) {
                maa.getAttribute().buildJsonPath(StaticProperties.PROPERTIES);
                Optional<OpenSearchParameterConfiguration> conf = parameterConfs.stream()
                        .filter(pc -> pc.getAttributeModelName().equals(maa.getAttribute().getJsonPath())).findFirst();
                attributes.put(maa.getAttribute(), createEmptyQueryableAttribute(maa.getAttribute(), conf));
            }
            try {
                // Run statistic search on each attributes. Results are set back into the QueryableAttributes parameter.
                searchService.retrievePropertiesStats(criterion, searchType, attributes.values());
            } catch (SearchException e) {
                LOGGER.error("Error retrieving properties for each parameters of the OpenSearchDescription (parameter extension",
                             e);
            }
        } catch (FeignException e) {
            LOGGER.error("Error retrieving attributes from IModelAttrAssocClient", e);
        }

        return attributes;
    }

    /**
     * Create an new empty {@link QueryableAttribute} object for the given {@link AttributeModel}
     * and the associated {@link OpenSearchParameterConfiguration}
     * @param att {@link AttributeModel}
     * @param conf {@link OpenSearchParameterConfiguration}
     * @return {@link QueryableAttribute}
     */
    private QueryableAttribute createEmptyQueryableAttribute(AttributeModel att,
            Optional<OpenSearchParameterConfiguration> conf) {
        if (conf.isPresent()) {
            return new QueryableAttribute(att.getJsonPath(), null, att.isTextAttribute(),
                    conf.get().getOptionsCardinality());
        } else {
            return new QueryableAttribute(att.getJsonPath(), null, att.isTextAttribute(), 0);
        }
    }

    /**
     * Retrieve {@link Project} information
     * @param currentTenant {@link String} project name
     * @return {@link Project}
     */
    private Project getProject(String currentTenant) {
        Project project = new Project("Undefined", null, false, "Undefined");
        try {
            tenantResolver.forceTenant(currentTenant);
            FeignSecurityManager.asSystem();
            ResponseEntity<Resource<Project>> projectResponse = projectClient.retrieveProject(currentTenant);
            FeignSecurityManager.reset();
            // in case of a problem there is already a RuntimeException which is launch by feign
            if (!HttpUtils.isSuccess(projectResponse.getStatusCode())) {
                LOGGER.error("Error retrieve project from IProjectClient. response status : {}",
                             projectResponse.getStatusCode());
            } else {
                project = projectResponse.getBody().getContent();
            }
        } catch (FeignException e) {
            LOGGER.error("Error retrieve project from IProjectClient. response status :", e);
        }
        return project;
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
