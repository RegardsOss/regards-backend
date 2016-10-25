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

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.ITenantConnectionResolver;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.project.client.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class DefaultMultitenantConnectionsReader
 *
 * Default tenants connections configuration reader. Reads tenants from the microservice "rs-admin". Enabled, only if
 * the microservice is Eureka client.
 *
 * @author CS
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
    private final IProjectsClient projectsClient;

    /**
     * Current Microservice name
     */
    private final String microserviceName;

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
    public MicroserviceTenantConnectionResolver(final IProjectsClient pProjectsClient, final String pMicroserviceName) {
        super();
        projectsClient = pProjectsClient;
        microserviceName = pMicroserviceName;
    }

    @Override
    public List<TenantConnection> getTenantConnections() {

        final List<TenantConnection> tenants = new ArrayList<>();

        final List<Project> projects = getProjects();
        for (final Project project : projects) {
            final ProjectConnection projectConnection = getProjectConnection(project.getName(), microserviceName);
            if (projectConnection != null) {
                tenants.add(new TenantConnection(project.getName(), projectConnection.getUrl(),
                        projectConnection.getUserName(), projectConnection.getPassword(),
                        projectConnection.getDriverClassName()));
            }
        }

        return tenants;

    }

    /**
     *
     * Retrieve projects list from rest client
     *
     * @return List<Project>
     * @since 1.0-SNAPSHOT
     */
    private List<Project> getProjects() {

        final List<Project> projects = new ArrayList<>();
        final ResponseEntity<List<Resource<Project>>> response = projectsClient.retrieveProjectList();

        if (response.getStatusCode().equals(HttpStatus.OK)) {
            final List<Resource<Project>> resources = response.getBody();
            for (final Resource<Project> resource : resources) {
                projects.add(resource.getContent());
            }
        } else {
            LOG.error("Error retrieving projects from admin microservice");
        }
        return projects;

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
        try {
            final ResponseEntity<Resource<ProjectConnection>> response = projectsClient
                    .retrieveProjectConnection(pProjectName, microserviceName);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                projectConnection = response.getBody().getContent();
            }
        } catch (final EntityNotFoundException e) {
            LOG.error(e.getMessage(), e);
            LOG.error(String.format("No database connection found for project %s", pProjectName));
        }
        return projectConnection;
    }

}
