/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

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
@Component
@ConditionalOnMissingBean(IMultitenantConnectionsReader.class)
@ConditionalOnProperty(name = "regards.eureka.client.enabled", havingValue = "true", matchIfMissing = false)
public class DefaultMultitenantConnectionsReader implements IMultitenantConnectionsReader {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMultitenantConnectionsReader.class);

    /**
     * Current Microservice name
     */
    @Value("${spring.application.name")
    private String microserviceName;

    /**
     * Feign client to request administration service for projects informations
     */
    @Autowired
    private IProjectsClient projectsClient;

    @Override
    public List<ProjectConnection> getTenantConnections() {

        final List<ProjectConnection> connections = new ArrayList<>();
        final List<Project> projects = getProjects();
        for (final Project project : projects) {
            final ProjectConnection projectConnection = getProjectConnection(project.getName(), microserviceName);
            if (projectConnection != null) {
                connections.add(projectConnection);
            }
        }

        return connections;

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
        final ResponseEntity<List<Resource<Project>>> response = (ResponseEntity<List<Resource<Project>>>) projectsClient
                .retrieveProjectList();

        if (response.getStatusCode().equals(HttpStatus.OK)) {

            final List<Resource<Project>> resources = response.getBody();
            for (final Resource<Project> resource : resources) {
                projects.add(resource.getContent());
            }

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
            final ResponseEntity<Resource<ProjectConnection>> response = (ResponseEntity<Resource<ProjectConnection>>) projectsClient
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
