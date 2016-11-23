/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import feign.FeignException;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
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
     * Current Microservice name
     */
    private final String microserviceName;

    /**
     * Security service
     */
    private final JWTService jwtService;

    /**
     * Administration service projects client
     */
    private final IProjectsClient projectsClient;

    /**
     * Administration service projectConnections client
     */
    private final IProjectConnectionClient projectConnectionsClient;

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
    public MicroserviceTenantConnectionResolver(final String pMicroserviceName, final JWTService pJwtService,
            final IProjectsClient pProjectsClient, final IProjectConnectionClient pProjectConnectionsClient) {
        super();
        microserviceName = pMicroserviceName;
        jwtService = pJwtService;
        projectsClient = pProjectsClient;
        projectConnectionsClient = pProjectConnectionsClient;
    }

    @Override
    public List<TenantConnection> getTenantConnections() {

        final List<TenantConnection> tenants = new ArrayList<>();

        try {
            jwtService.injectToken("instance", RoleAuthority.getSysRole(microserviceName));

            final ResponseEntity<List<Resource<Project>>> results = projectsClient.retrieveProjectList();

            if ((results != null) && (results.getBody() != null)) {
                for (final Resource<Project> resource : results.getBody()) {
                    final ProjectConnection projectConnection = getProjectConnection(resource.getContent().getName());
                    if (projectConnection != null) {
                        tenants.add(new TenantConnection(resource.getContent().getName(), projectConnection.getUrl(),
                                projectConnection.getUserName(), projectConnection.getPassword(),
                                projectConnection.getDriverClassName()));
                    }
                }
            } else {
                LOG.error("Error during remote request to administration service");
            }
        } catch (final JwtException e) {
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
    private ProjectConnection getProjectConnection(final String pProjectName) {
        ProjectConnection projectConnection = null;

        try {
            final ResponseEntity<Resource<ProjectConnection>> response = projectConnectionsClient
                    .retrieveProjectConnection(pProjectName, microserviceName);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                projectConnection = response.getBody().getContent();
            }

        } catch (final FeignException e) {
            LOG.error(e.getMessage(), e);
            LOG.error(String.format("No database connection found for project %s", pProjectName));
        }

        return projectConnection;
    }

    @Override
    public void addTenantConnection(final TenantConnection pTenantConnection) {
        ResponseEntity<Resource<Project>> response;
        try {
            response = projectsClient.retrieveProject(pTenantConnection.getName());
            final Project project = HateoasUtils.unwrap(response.getBody());
            final ProjectConnection projectConnection = new ProjectConnection(project, microserviceName,
                    pTenantConnection.getUserName(), pTenantConnection.getPassword(),
                    pTenantConnection.getDriverClassName(), pTenantConnection.getUrl());
            projectConnectionsClient.createProjectConnection(projectConnection);
        } catch (final FeignException e) {
            LOG.error("Error during initialization of new tenant connection for microservice {} and tenant {}",
                      microserviceName, pTenantConnection.getName());
            LOG.error(e.getMessage(), e);
        }
    }

}
