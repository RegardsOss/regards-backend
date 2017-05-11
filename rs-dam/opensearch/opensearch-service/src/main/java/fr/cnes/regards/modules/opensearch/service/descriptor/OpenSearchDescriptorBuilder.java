/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.opensearch.service.descriptor;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.utils.HttpUtils;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.models.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.models.client.IModelClient;
import fr.cnes.regards.modules.models.domain.EntityType;
import fr.cnes.regards.modules.models.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.schema.QueryType;
import fr.cnes.regards.modules.search.schema.UrlType;

/**
 * base class allowing to build a description for our OpenSearch endpoints
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Component
public class OpenSearchDescriptorBuilder {

    private static final String DESCRIPTION = "%s REGARDS Search Engine";

    private static final String SHORT_NAME = "%s Search";

    private final String contact;

    private final String developer;

    private final String microserviceName;

    private final IProjectsClient projectClient;

    private final IRuntimeTenantResolver tenantResolver;

    private final IModelClient modelClient;

    private final IModelAttrAssocClient modelAttrAssocClient;

    public OpenSearchDescriptorBuilder(@Value("${regards.opensearch.contact}") String contact,
            @Value("${regards.opensearch.developer}") String developer,
            @Value("${spring.application.name}") String microserviceName, @Autowired IProjectsClient projectClient,
            @Autowired IRuntimeTenantResolver tenantResolver, IModelClient modelClient,
            @Autowired IModelAttrAssocClient modelAttrAssocClient) {
        this.contact = contact;
        this.developer = developer;
        this.microserviceName = microserviceName;
        this.projectClient = projectClient;
        this.tenantResolver = tenantResolver;
        this.modelClient = modelClient;
        this.modelAttrAssocClient = modelAttrAssocClient;
    }

    /**
     * build an OpenSearch descriptor for the current tenant(i.e. project) on the given path and entity type
     *
     * @param searchOn entity type which attributes are relevant for the search. Can be null in which case it means
     * everything
     * @param endpointPath endpoint concerned by the search
     */
    public OpenSearchDescription build(EntityType searchOn, String endpointPath) {
        // lets build the generic part of the descriptor
        String currentTenant = tenantResolver.getTenant();
        Project project = getProject(currentTenant);
        OpenSearchDescription desc = buildGenerics(project);

        UrlType url = new UrlType();
        StringJoiner sj = new StringJoiner("/", project.getHost(), "");
        sj.add(microserviceName);
        sj.add(endpointPath);
        String urlTemplate = sj.toString();
        urlTemplate += "?q={searchTerms}";
        url.setTemplate(urlTemplate);
        desc.getUrl().add(url);

        Set<AttributeModel> attrs = getAttributesFor(searchOn, currentTenant);
        QueryType query = new QueryType();
        query.setRole("example");
        query.setSearchTerms(getSearchTermExample(attrs));
        desc.getQuery().add(query);
        return desc;
    }

    private String getSearchTermExample(Set<AttributeModel> pAttrs) {
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
                    result.append("{ISO-8601 date} OR").append(getAttributeFullName(attr)).append(":")
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
                    result.append("{double value} OR").append(getAttributeFullName(attr)).append(":")
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
                    result.append("{long value} OR").append(getAttributeFullName(attr)).append(":")
                            .append("{long value}");
                    break;
                case INTEGER_ARRAY:
                    result.append("{integer value} OR").append(getAttributeFullName(attr)).append(":")
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
                    result.append("{string} OR").append(getAttributeFullName(attr)).append(":").append("{string}");
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
        return attr.getFragment().isDefaultFragment() ? attr.getName()
                : attr.getFragment().getName() + "." + attr.getName();
    }

    private OpenSearchDescription buildGenerics(Project project) {
        OpenSearchDescription desc = new OpenSearchDescription();
        desc.setInputEncoding(StandardCharsets.UTF_8.displayName());
        desc.setOutputEncoding(StandardCharsets.UTF_8.displayName());
        desc.setContact(contact);
        desc.setDescription(String.format(DESCRIPTION, project.getName()));
        desc.setDeveloper(developer);
        desc.setShortName(String.format(SHORT_NAME, project.getName()));
        return desc;
    }

    private Set<AttributeModel> getAttributesFor(EntityType pType, String pCurrentTenant) {
        tenantResolver.forceTenant(pCurrentTenant);
        FeignSecurityManager.asSystem();
        ResponseEntity<Collection<ModelAttrAssoc>> assocsResponse = modelAttrAssocClient.getModelAttrAssocsFor(pType);
        FeignSecurityManager.reset();
        if (!HttpUtils.isSuccess(assocsResponse.getStatusCode())) {
            // it mainly means 404: which would mean route not found as we are retrieving the current project
            throw new IllegalStateException(
                    "Trying to contact microservice responsible for Model but couldn't contact it");
        }

        return assocsResponse.getBody().stream().map(maa -> maa.getAttribute()).collect(Collectors.toSet());
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

}
