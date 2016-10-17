package fr.cnes.regards.microservices.administration.configuration;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.IMultitenantConnectionsReader;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import fr.cnes.regards.modules.project.service.ProjectService;

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
@Component
public class MultitenantConnectionsReader implements IMultitenantConnectionsReader {

    private static final Logger LOG = LoggerFactory.getLogger(MultitenantConnectionsReader.class);

    @Value("${spring.application.name")
    private String microserviceName;

    @Autowired
    private ProjectService projectService;

    @Override
    public List<ProjectConnection> getTenantConnections() {
        final List<ProjectConnection> connections = new ArrayList<>();
        final Iterable<Project> projects = projectService.retrieveProjectList();

        for (final Project project : projects) {
            try {
                final ProjectConnection projectConnection = projectService.retreiveProjectConnection(project.getName(),
                                                                                                     microserviceName);
                if (projectConnection != null) {
                    connections.add(projectConnection);
                }
            } catch (final EntityNotFoundException e) {
                LOG.error(e.getMessage(), e);
                LOG.error(String.format("No database connection found for project %s", project.getName()));
            }
        }

        return connections;

    }

}
