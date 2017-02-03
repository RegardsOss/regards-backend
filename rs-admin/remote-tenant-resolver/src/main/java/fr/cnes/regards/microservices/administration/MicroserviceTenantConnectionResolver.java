/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.google.common.base.Throwables;

import feign.FeignException;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(MicroserviceTenantConnectionResolver.class);

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
     * Tenant resolver
     */
    private final ITenantResolver tenantResolver;

    public MicroserviceTenantConnectionResolver(final String pMicroserviceName, final JWTService pJwtService,
            final IProjectsClient pProjectsClient, final IProjectConnectionClient pProjectConnectionsClient,
            final ITenantResolver pTenantResolver) {
        super();
        microserviceName = pMicroserviceName;
        jwtService = pJwtService;
        projectsClient = pProjectsClient;
        projectConnectionsClient = pProjectConnectionsClient;
        tenantResolver = pTenantResolver;
    }

    @Override
    public List<TenantConnection> getTenantConnections() {

        final List<TenantConnection> tenantsConnections = new ArrayList<>();

        final Set<String> tenants = tenantResolver.getAllTenants();

        for (final String tenant : tenants) {
            final ProjectConnection projectConnection = getProjectConnection(tenant);
            if (projectConnection != null) {
                tenantsConnections
                        .add(new TenantConnection(tenant, projectConnection.getUrl(), projectConnection.getUserName(),
                                projectConnection.getPassword(), projectConnection.getDriverClassName()));
            }
        }

        return tenantsConnections;

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
            final ResponseEntity<PagedResources<Resource<ProjectConnection>>> response = projectConnectionsClient
                    .retrieveProjectsConnections(pProjectName, microserviceName);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                projectConnection = response.getBody().iterator().next().getContent();
            }

        } catch (final FeignException e) {
            LOGGER.error(e.getMessage(), e);
            LOGGER.error(String.format("No database connection found for project %s", pProjectName));
        }

        return projectConnection;
    }

    @Override
    public void addTenantConnection(final TenantConnection pTenantConnection) {

        try {
            jwtService.injectToken(pTenantConnection.getName(), RoleAuthority.getSysRole(microserviceName));
        } catch (JwtException e1) {
            Throwables.propagate(e1);
        }

        ResponseEntity<Resource<Project>> response;
        try {
            response = projectsClient.retrieveProject(pTenantConnection.getName());
            if (response.getStatusCode().equals(HttpStatus.OK) && (response.getBody() != null)
                    && (response.getBody().getClass() != null)) {
                final Project project = HateoasUtils.unwrap(response.getBody());
                final ProjectConnection projectConnection = new ProjectConnection(project, microserviceName,
                        pTenantConnection.getUserName(), pTenantConnection.getPassword(),
                        pTenantConnection.getDriverClassName(), pTenantConnection.getUrl());
                projectConnectionsClient.createProjectConnection(projectConnection);
            } else {
                LOGGER.error("Error getting {} project informations from administration microservice",
                             pTenantConnection.getName());
            }
        } catch (final FeignException e) {
            LOGGER.error("Error during initialization of new tenant connection for microservice {} and tenant {}",
                         microserviceName, pTenantConnection.getName());
            LOGGER.error(e.getMessage(), e);
        }
    }

}
