/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.ITenantConnectionResolver;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import fr.cnes.regards.modules.project.service.IProjectConnectionService;
import fr.cnes.regards.modules.project.service.IProjectService;

/**
 *
 * Class MultitenantConnectionsReader
 *
 * Overides the default method to initiate the liste of connections for the multinants database. The project connections
 * are read from the instance database throught the ProjectService.
 *
 * @author CS
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
     * Administration project service
     */
    private final IProjectService projectService;

    /**
     * Administration project service
     */
    private final IProjectConnectionService projectConnectionService;

    /**
     *
     * Constructor
     *
     * @param pMicroserviceName
     *            name of the current microservice
     * @param pProjectService
     *            project service
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
                        .retreiveProjectConnection(project.getName(), microserviceName);
                if (projectConnection != null) {
                    tenants.add(new TenantConnection(projectConnection.getProject().getName(),
                            projectConnection.getUrl(), projectConnection.getUserName(),
                            projectConnection.getPassword(), projectConnection.getDriverClassName()));
                }
            } catch (final EntityNotFoundException e) {
                LOG.error(e.getMessage(), e);
                LOG.error(String.format("No database connection found for project %s", project.getName()));
            }
        }

        return tenants;

    }

}
