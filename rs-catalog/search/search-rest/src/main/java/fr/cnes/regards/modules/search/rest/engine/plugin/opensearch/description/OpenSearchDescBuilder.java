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
import java.nio.charset.Charset;
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
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import com.google.common.collect.Maps;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
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
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchEngine;
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
 * base class allowing to build a description for our OpenSearch endpoints
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Component
public class OpenSearchDescBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchDescBuilder.class);

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

    /**
     * {@link IModelAttrAssocClient} instance
     */
    @Autowired
    private IModelAttrAssocClient modelAttrAssocClient;

    @Autowired
    public OpenSearchDescriptionConf configuration;

    /**
     * build an OpenSearch descriptor for the current tenant(i.e. project) on the given path and entity type
     * @param context
     * @param extensions
     * @return
     * @throws UnsupportedEncodingException
     */
    public OpenSearchDescription build(SearchContext context, ICriterion criterion,
            List<IOpenSearchExtension> extensions, List<OpenSearchParameterConfiguration> parameterConfs)
            throws UnsupportedEncodingException {
        String currentTenant = tenantResolver.getTenant();
        Project project = getProject(currentTenant);
        Map<AttributeModel, QueryableAttribute> attributes = getAttributesFor(criterion, context.getSearchType(),
                                                                              parameterConfs, currentTenant);

        // Build descriptor generic metadatas
        OpenSearchDescription desc = buildMetadata(project, attributes.keySet());

        // Build open search parameters to handle parameters extension
        List<OpenSearchParameter> parameters = buildParameters(attributes, extensions);

        // Build urls
        String endpointPath = getEndpoint(context.getSearchType());
        desc.getUrl().add(buildUrl(project, parameters, endpointPath, MediaType.APPLICATION_ATOM_XML_VALUE));
        desc.getUrl().add(buildUrl(project, parameters, endpointPath, MediaType.APPLICATION_JSON_VALUE));

        return desc;
    }

    private OpenSearchDescription buildMetadata(Project project, Collection<AttributeModel> attributes) {
        OpenSearchDescription desc = new OpenSearchDescription();
        desc.setDescription(String.format(project.getDescription()));
        desc.setShortName(String.format(project.getName()));
        desc.setLongName(String.format(project.getName()));
        desc.getQuery().add(buildQuery(attributes));
        desc.setDeveloper(configuration.getDeveloper());
        desc.setAttribution(configuration.getAttribution());
        desc.setAdultContent("false");
        desc.setLanguage("en");
        desc.setInputEncoding(StandardCharsets.UTF_8.displayName());
        desc.setOutputEncoding(StandardCharsets.UTF_8.displayName());
        return desc;
    }

    private UrlType buildUrl(Project project, List<OpenSearchParameter> parameters, String endpoint, String mediaType)
            throws UnsupportedEncodingException {
        UrlType url = new UrlType();
        url.getParameter().addAll(parameters);
        url.setRel("results");
        url.setType(mediaType);
        StringJoiner sj = new StringJoiner("/");
        sj.add(project.getHost());
        //FIXME: do not fix the gateway prefix with a constant, we should find a way to retrieve it
        sj.add("api/v1");
        // UriUtils.encode give back the string in US-ASCII encoding, so let create a new String that will then reencode
        // the string however the jvm wants
        sj.add(new String(UriUtils.encode(microserviceName, Charset.defaultCharset().name()).getBytes(),
                StandardCharsets.US_ASCII));
        sj.add(endpoint);
        String urlTemplate = sj.toString();
        // urlTemplate can contain double slashes on the part which is valid but ugly so let make the world a bit
        // prettier. We choose project.getHost().length()-1 because Project.getHost() can finish by a "/" so we would
        // have a double "/" with the joiner and we admit that what the administrator entered as host is valid
        int incorrectDoubleSlashIndex = project.getHost().length() - 1;
        String correctPart = urlTemplate.substring(0, incorrectDoubleSlashIndex);
        String incorrectPart = urlTemplate.substring(incorrectDoubleSlashIndex);
        StringBuilder urlTemplateBuilder = new StringBuilder(correctPart);
        urlTemplateBuilder.append(incorrectPart.replaceAll("//", "/"));
        urlTemplateBuilder.append(String.format("?%s={%s}", configuration.getQueryParameterName(),
                                                configuration.getQueryParameterValue()));
        parameters.stream().forEach(p -> urlTemplateBuilder.append(String.format("&%s=%s", p.getName(), p.getValue())));
        url.setTemplate(urlTemplateBuilder.toString());
        return url;
    }

    private QueryType buildQuery(Collection<AttributeModel> attributes) {
        QueryType query = new QueryType();
        query.setRole("example");
        query.setSearchTerms(getSearchTermExample(attributes));
        return query;
    }

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

    private String getSearchTermExample(Collection<AttributeModel> pAttrs) {
        StringJoiner sj = new StringJoiner(" AND ");
        for (AttributeModel attr : pAttrs) {
            // result has the following format: "fullAttrName:value", except for arrays. Arrays are represented thanks
            // to multiple values: attr:val1 OR attr:val2 ...
            StringBuilder result = new StringBuilder();
            result.append(getAttributeFullName(attr)).append(":");
            switch (attr.getType()) {
                case BOOLEAN:
                    result.append("{boolean}");
                    break;
                case DATE_ARRAY:
                    result.append("{ISO-8601 date} OR ").append(getAttributeFullName(attr)).append(":")
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
                    result.append("{double value} OR ").append(getAttributeFullName(attr)).append(":")
                            .append("{double value}");
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
                    result.append("{long value} OR ").append(getAttributeFullName(attr)).append(":")
                            .append("{long value}");
                    break;
                case INTEGER_ARRAY:
                    result.append("{integer value} OR ").append(getAttributeFullName(attr)).append(":")
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
                    result.append("{string} OR ").append(getAttributeFullName(attr)).append(":").append("{string}");
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

    private String getAttributeFullName(AttributeModel attr) {
        if (attr.getFragment().isDefaultFragment()) {
            return StaticProperties.PROPERTIES + "." + attr.getName();
        } else {
            return StaticProperties.PROPERTIES + "." + attr.getFragment().getName() + "." + attr.getName();
        }
    }

    private Map<AttributeModel, QueryableAttribute> getAttributesFor(ICriterion criterion, SearchType searchType,
            List<OpenSearchParameterConfiguration> parameterConfs, String pCurrentTenant) {

        tenantResolver.forceTenant(pCurrentTenant);
        FeignSecurityManager.asSystem();
        ResponseEntity<Collection<ModelAttrAssoc>> assocsResponse = modelAttrAssocClient
                .getModelAttrAssocsFor(getEntityType(searchType));
        FeignSecurityManager.reset();
        if (!HttpUtils.isSuccess(assocsResponse.getStatusCode())) {
            // it mainly means 404: which would mean route not found as we are retrieving the current project
            throw new IllegalStateException(
                    "Trying to contact microservice responsible for Model but couldn't contact it");
        }

        Map<AttributeModel, QueryableAttribute> attributes = Maps.newHashMap();
        for (ModelAttrAssoc maa : assocsResponse.getBody()) {
            maa.getAttribute().buildJsonPath(StaticProperties.PROPERTIES);
            Optional<OpenSearchParameterConfiguration> conf = parameterConfs.stream()
                    .filter(pc -> pc.getAttributeModelName().equals(maa.getAttribute().getJsonPath())).findFirst();
            attributes.put(maa.getAttribute(), createEmptyQueryableAttribute(maa.getAttribute(), conf));
        }

        // Run search to get statistics on each parameters
        try {
            searchService.retrievePropertiesStats(criterion, searchType, attributes.values());
            LOGGER.info("PLOP");

        } catch (SearchException e) {
            LOGGER.error("Error retrieving properties for each parameters of the OpenSearchDescription (parameter extension",
                         e);
        }

        return attributes;
    }

    private QueryableAttribute createEmptyQueryableAttribute(AttributeModel att,
            Optional<OpenSearchParameterConfiguration> conf) {
        if (conf.isPresent()) {
            return new QueryableAttribute(att.getJsonPath(), null, att.isTextAttribute(),
                    conf.get().getOptionsCardinality());
        } else {
            return new QueryableAttribute(att.getJsonPath(), null, att.isTextAttribute(), 0);
        }
    }

    private Project getProject(String currentTenant) {
        tenantResolver.forceTenant(currentTenant);
        FeignSecurityManager.asSystem();
        ResponseEntity<Resource<Project>> projectResponse = projectClient.retrieveProject(currentTenant);
        FeignSecurityManager.reset();
        // in case of a problem there is already a RuntimeException which is launch by feign
        if (!HttpUtils.isSuccess(projectResponse.getStatusCode())) {
            // it mainly means 404: which would mean route not found as we are retrieving the current project
            throw new IllegalStateException(
                    "Trying to contact microservice responsible for project but couldn't contact it");
        }
        return projectResponse.getBody().getContent();
    }

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

    private String getEndpoint(SearchType searchType) {
        String engineMapping = SearchEngineController.TYPE_MAPPING.replace(SearchEngineController.ENGINE_TYPE_PARAMETER,
                                                                           OpenSearchEngine.ENGINE_ID);
        switch (searchType) {
            case COLLECTIONS:
                return engineMapping + SearchEngineController.SEARCH_COLLECTIONS_MAPPING;
            case DATAOBJECTS:
                return engineMapping + SearchEngineController.SEARCH_DATAOBJECTS_MAPPING;
            case DATASETS:
                return engineMapping + SearchEngineController.SEARCH_DATASETS_MAPPING;
            case DOCUMENTS:
                return engineMapping + SearchEngineController.SEARCH_DOCUMENTS_MAPPING;
            case ALL:
            case DATAOBJECTS_RETURN_DATASETS:
            default:
                throw new UnsupportedOperationException(
                        String.format("Unsupproted entity type for open search. %s", searchType.toString()));
        }
    }

}
