/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import feign.FeignException;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import fr.cnes.regards.modules.project.service.IProjectConnectionService;
import fr.cnes.regards.modules.project.service.IProjectService;
import fr.cnes.regards.modules.project.service.ProjectConnectionService;
import fr.cnes.regards.modules.project.service.ProjectService;

/**
 *
 * Overrides the default method to initiate the list of connections for the multitenants database. The project
 * connections are read from the instance database through the ProjectService.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class LocalTenantConnectionResolver implements ITenantConnectionResolver {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(LocalTenantConnectionResolver.class);

    /**
     * Current microservice name
     */
    private final String microserviceName;

    /**
     * The {@link ProjectService} management.
     */
    private final IProjectService projectService;

    /**
     * The {@link ProjectConnectionService} management.
     */
    private final IProjectConnectionService projectConnectionService;

    /**
     *
     * Constructor
     *
     * @param pMicroserviceName
     *            name of the current microservice
     * @param pProjectService
     *            the {@link ProjectService}
     * @param pProjectConnectionService
     *            the  {@link ProjectConnectionService}
     * @since 1.0-SNAPSHOT
     */
    public LocalTenantConnectionResolver(final String pMicroserviceName, final IProjectService pProjectService,
            final IProjectConnectionService pProjectConnectionService) {
        super();
        microserviceName = pMicroserviceName;
        projectService = pProjectService;
        projectConnectionService = pProjectConnectionService;
    }

    @Override
    public List<TenantConnection> getTenantConnections() {
        final List<TenantConnection> tenants = new ArrayList<>();
        final Iterable<Project> projects = projectService.retrieveProjectList();

        for (final Project project : projects) {
            try {
                final ProjectConnection projectConnection = projectConnectionService
                        .retrieveProjectConnection(project.getName(), microserviceName);
                if (projectConnection != null) {
                    tenants.add(new TenantConnection(projectConnection.getProject().getName(),
                            projectConnection.getUrl(), projectConnection.getUserName(),
                            projectConnection.getPassword(), projectConnection.getDriverClassName()));
                }
            } catch (final EntityNotFoundException e) {
                LOG.debug(e.getMessage(), e);
                LOG.error(String.format("No database connection found for project %s", project.getName()));
            }
        }

        return tenants;
    }

    @Override
    public void addTenantConnection(final TenantConnection pTenantConnection) {
        try {
            final Project project = projectService.retrieveProject(pTenantConnection.getName());

            final ProjectConnection projectConnection = new ProjectConnection(project, microserviceName,
                    pTenantConnection.getUserName(), pTenantConnection.getPassword(),
                    pTenantConnection.getDriverClassName(), pTenantConnection.getUrl());

            projectConnectionService.createProjectConnection(projectConnection);
        } catch (final FeignException e) {
            LOG.error("Error during initialization of new tenant connection for microservice {} and tenant {}",
                      microserviceName, pTenantConnection.getName());
            LOG.error(e.getMessage(), e);
        } catch (final ModuleException e) {
            LOG.error("Error adding new tenant. Cause : {}", e.getMessage());
            LOG.debug(e.getMessage(), e);
        }

    }

}
