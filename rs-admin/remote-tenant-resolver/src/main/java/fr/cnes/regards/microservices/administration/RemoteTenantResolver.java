/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.JwtTokenUtils;
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
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RemoteTenantResolver.class);

    private final String microserviceName;

    private final JWTService jwtService;

    /**
     * Administration service client to retrieve projects
     */
    private final IProjectsClient projectsClient;

    public RemoteTenantResolver(final IProjectsClient pProjectsClient, final JWTService pJwtService,
            final String pMicroserviceName) {
        super();
        projectsClient = pProjectsClient;
        microserviceName = pMicroserviceName;
        jwtService = pJwtService;
    }

    @Override
    public Set<String> getAllTenants() {
        return JwtTokenUtils.asSafeCallableOnRole(this::getAllTenantsSupplier, jwtService, null)
                .apply(RoleAuthority.getSysRole(microserviceName));

    }

    /**
     *
     * Retrieve all tenants from the administration service through the projects client.
     *
     * @return
     * @since 1.0-SNAPSHOT
     */
    public Set<String> getAllTenantsSupplier() {
        final Set<String> tenants = new HashSet<>();
        final List<Resource<Project>> resources = new ArrayList<>();

        boolean nextProjectsPage;
        int projectPage = 0;
        // Get all tenants with projects clients pageable requests
        do {
            final ResponseEntity<PagedResources<Resource<Project>>> response = projectsClient
                    .retrieveProjectList(projectPage, 100);
            if ((response != null) && response.getStatusCode().equals(HttpStatus.OK) && (response.getBody() != null)) {
                nextProjectsPage = projectPage++ == response.getBody().getMetadata().getTotalPages();
                resources.addAll(response.getBody().getContent());
            } else {
                LOG.error("Error during remote request to administration service");
                nextProjectsPage = false;
            }
        } while (!nextProjectsPage);

        final List<Project> projects = HateoasUtils.unwrapList(resources);
        projects.forEach(p -> tenants.add(p.getName()));

        return tenants;
    }

}
