/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Class RemoteTenantResolver
 *
 * Microservice remote tenant resolver. Retrieve tenants from the administration microservice.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class RemoteTenantResolver implements ITenantResolver {

    /**
     * Administration service client to retrieve projects
     */
    private final IProjectsClient projectsClient;

    public RemoteTenantResolver(final IProjectsClient pProjectsClient) {
        super();
        projectsClient = pProjectsClient;
    }

    @Override
    public Set<String> getAllTenants() {
        final Set<String> tenants = new HashSet<>();
        final ResponseEntity<List<Resource<Project>>> response = projectsClient.retrieveProjectList();
        if (response.getStatusCode().equals(HttpStatus.OK)) {
            final List<Project> projects = HateoasUtils.unwrapList(response.getBody());
            projects.forEach(p -> tenants.add(p.getName()));
        }
        return tenants;
    }

}
