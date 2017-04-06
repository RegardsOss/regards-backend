/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionConfigured;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.utils.DataSourceHelper;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.InvalidConnectionException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Service class to manage REGARDS projects.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Christophe Mertz
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 *
 * @since 1.0-SNAPSHOT
 */
@Service
@InstanceTransactional
public class ProjectConnectionService implements IProjectConnectionService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectConnectionService.class);

    /**
     * JPA Repository to query projects from database
     */
    private final IProjectRepository projectRepository;

    /**
     * JPA Repository to query projectConnection from database
     */
    private final IProjectConnectionRepository projectConnectionRepository;

    /**
     * AMQP message publisher
     */
    private final IInstancePublisher instancePublisher;

    /**
     * The constructor.
     *
     * @param pProjectRepository
     *            The JPA {@link Project} repository.
     * @param pProjectConnectionRepository
     *            The JPA {@link ProjectConnection} repository.
     * @param pDefaultProperties
     *            multitenant DAO properties form config file
     * @param pMicroserviceName
     *            current microservice name
     * @param pInstancePublisher
     *            Amqp publisher
     */
    public ProjectConnectionService(final IProjectRepository pProjectRepository,
            final IProjectConnectionRepository pProjectConnectionRepository,
            final IInstancePublisher pInstancePublisher) {
        super();
        projectRepository = pProjectRepository;
        projectConnectionRepository = pProjectConnectionRepository;
        instancePublisher = pInstancePublisher;
    }

    @Override
    public Page<ProjectConnection> retrieveProjectsConnections(final Pageable pPageable) {
        return projectConnectionRepository.findAll(pPageable);
    }

    @Override
    public ProjectConnection retrieveProjectConnection(final String pProjectName, final String pMicroService)
            throws EntityNotFoundException {
        final ProjectConnection connection = projectConnectionRepository
                .findOneByProjectNameAndMicroservice(pProjectName, pMicroService);
        if (connection == null) {
            throw new EntityNotFoundException(String.format("%s:%s", pProjectName, pMicroService),
                    ProjectConnection.class);
        }
        return connection;
    }

    @Override
    public ProjectConnection createProjectConnection(ProjectConnection pProjectConnection, boolean silent)
            throws ModuleException {
        final ProjectConnection connection;
        final Project project = pProjectConnection.getProject();
        // Check referenced project exists
        if ((project.getId() != null) && projectRepository.exists(project.getId())) {
            // Check project connection to create doesn't already exists
            if (projectConnectionRepository
                    .findOneByProjectNameAndMicroservice(project.getName(),
                                                         pProjectConnection.getMicroservice()) == null) {
                connection = projectConnectionRepository.save(pProjectConnection);
            } else {
                throw new EntityAlreadyExistsException(project.getName());
            }
        } else {
            throw new EntityNotFoundException(pProjectConnection.getId().toString(), ProjectConnection.class);
        }

        // Send event to all microservices that a new connection is available for a new project
        final TenantConnection tenantConnection = new TenantConnection(connection.getProject().getName(),
                connection.getUrl(), connection.getUserName(), connection.getPassword(),
                connection.getDriverClassName());

        if (!silent) {
            instancePublisher.publish(new TenantConnectionConfigured(tenantConnection, connection.getMicroservice()));
        }

        return connection;
    }

    @Override
    public void deleteProjectConnection(final Long pProjectConnectionId) throws EntityNotFoundException {
        if (projectConnectionRepository.exists(pProjectConnectionId)) {
            projectConnectionRepository.delete(pProjectConnectionId);
        } else {
            final String message = "Invalid entity <ProjectConnection> for deletion. Entity (id=%d) does not exists";
            LOGGER.error(String.format(message, pProjectConnectionId));
            throw new EntityNotFoundException(pProjectConnectionId.toString(), ProjectConnection.class);
        }
    }

    @Override
    public ProjectConnection updateProjectConnection(Long pProjectConnectionId, ProjectConnection pProjectConnection)
            throws EntityNotFoundException {
        final ProjectConnection connection;
        // Check that entity to update exists
        if ((pProjectConnection.getId() != null) && projectConnectionRepository.exists(pProjectConnection.getId())) {
            final Project project = pProjectConnection.getProject();
            // Check that the referenced project exists
            if ((project.getId() != null) && projectRepository.exists(project.getId())) {
                // Update entity
                connection = projectConnectionRepository.save(pProjectConnection);
            } else {
                throw new EntityNotFoundException(project.getName(), Project.class);
            }
        } else {
            throw new EntityNotFoundException(pProjectConnection.getId().toString(), ProjectConnection.class);
        }
        return connection;
    }

    @Override
    public Page<ProjectConnection> retrieveProjectsConnectionsByProject(final String pProjectName,
            final Pageable pPageable) {
        return projectConnectionRepository.findByProjectName(pProjectName, pPageable);
    }

    @Override
    public ProjectConnection retrieveProjectConnectionById(final Long pId) throws EntityNotFoundException {
        final Optional<ProjectConnection> result = Optional.ofNullable(projectConnectionRepository.findOne(pId));
        return result.orElseThrow(() -> new EntityNotFoundException(pId, ProjectConnection.class));
    }

    @Override
    public List<ProjectConnection> retrieveProjectConnection(String pMicroService) throws EntityNotFoundException {
        return projectConnectionRepository.findByMicroservice(pMicroService);
    }

    @Override
    public void testProjectConnection(Long pConnectionIdentifier) throws ModuleException {
        ProjectConnection connection = retrieveProjectConnectionById(pConnectionIdentifier);
        DataSource dataSource = DataSourceHelper.createDataSource(connection.getUrl(), connection.getDriverClassName(),
                                                                  connection.getUserName(), connection.getPassword());
        try {
            dataSource.getConnection();
        } catch (SQLException e) {
            LOGGER.error("Connection test fail", e);
            throw new InvalidConnectionException(e);
        }
    }

}
