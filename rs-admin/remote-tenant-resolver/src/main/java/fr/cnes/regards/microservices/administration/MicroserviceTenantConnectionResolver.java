/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;

import com.netflix.hystrix.exception.HystrixRuntimeException;

import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.project.client.rest.IProjectConnectionClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class DefaultMultitenantConnectionsReader
 *
 * Default tenants connections configuration reader. Reads tenants from the microservice "rs-admin". Enabled, only if
 * the microservice is Eureka client.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class MicroserviceTenantConnectionResolver implements ITenantConnectionResolver {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(MicroserviceTenantConnectionResolver.class);

    /**
     * Feign client to request administration service for projects informations
     */
    private final IProjectConnectionClient projectConnectionClient;

    /**
     * Feign client to request administration service for projects informations
     */
    private final IProjectsClient projectsClient;

    /**
     * Current Microservice name
     */
    private final String microserviceName;

    /**
     * Security service
     */
    private final JWTService jwtService;

    /**
     *
     * Constructor
     *
     * @param pProjectsClient
     *            Admin client
     * @param pMicroserviceName
     *            microservice name
     * @since 1.0-SNAPSHOT
     */
    public MicroserviceTenantConnectionResolver(final IProjectConnectionClient pProjectConnectionClient,
            final IProjectsClient pProjectsClient, final String pMicroserviceName, final JWTService pJwtService) {
        super();
        projectConnectionClient = pProjectConnectionClient;
        microserviceName = pMicroserviceName;
        projectsClient = pProjectsClient;
        jwtService = pJwtService;
    }

    @Override
    public List<TenantConnection> getTenantConnections() {

        final List<TenantConnection> tenants = new ArrayList<>();

        try {
            jwtService.injectToken("instance", RoleAuthority.getSysRole(microserviceName));
            final ResponseEntity<List<Resource<Project>>> results = projectsClient.retrieveProjectList();
            if ((results != null) && (results.getBody() != null)) {
                for (final Resource<Project> resource : results.getBody()) {
                    final ProjectConnection projectConnection = getProjectConnection(resource.getContent().getName(),
                                                                                     microserviceName);
                    if (projectConnection != null) {
                        tenants.add(new TenantConnection(resource.getContent().getName(), projectConnection.getUrl(),
                                projectConnection.getUserName(), projectConnection.getPassword(),
                                projectConnection.getDriverClassName()));
                    }
                }
            } else {
                LOG.error("Error during remote request to administration service");
            }
        } catch (final JwtException | HystrixRuntimeException e) {
            LOG.error(e.getMessage(), e);
        }

        return tenants;

    }

    /**
     *
     * Retrieve a tenant connection for a project and a microservice.
     *
     * @param pProjectName
     *            Name of the project
     * @param pMicroserviceName
     *            Name of the microservice
     * @return ProjectConnection
     * @since 1.0-SNAPSHOT
     */
    private ProjectConnection getProjectConnection(final String pProjectName, final String pMicroserviceName) {
        ProjectConnection projectConnection = null;

        final ResponseEntity<Resource<ProjectConnection>> response = projectConnectionClient
                .retrieveProjectConnection(pProjectName, microserviceName);
        switch (response.getStatusCode()) {
            case OK:
                projectConnection = response.getBody().getContent();
                break;
            case NOT_FOUND:
                LOG.error(String.format("No database connection found for project %s", pProjectName));
                break;
            default:
                LOG.error(String.format("Error getting  database connection for project %s", pProjectName));
                break;
        }

        return projectConnection;
    }

}
