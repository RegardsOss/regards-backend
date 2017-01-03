/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.jpa.multitenant.event.NewTenantEvent;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.utils.jwt.JwtTokenUtils;
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
public class ProjectConnectionService implements IProjectConnectionService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectConnectionService.class);

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
    private final IPublisher publisher;

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
     * @param pPublisher
     *            Amqp publisher
     */
    public ProjectConnectionService(final IProjectRepository pProjectRepository,
            final IProjectConnectionRepository pProjectConnectionRepository, final IPublisher pPublisher) {
        super();
        projectRepository = pProjectRepository;
        projectConnectionRepository = pProjectConnectionRepository;
        publisher = pPublisher;
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
    public ProjectConnection createProjectConnection(final ProjectConnection pProjectConnection)
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
        final TenantConnection tenant = new TenantConnection(connection.getProject().getName(), connection.getUrl(),
                connection.getUserName(), connection.getPassword(), connection.getDriverClassName());
        final Supplier<Boolean> publishNewtenantEvent = () -> {
            try {
                publisher.publish(new NewTenantEvent(tenant, connection.getMicroservice()),
                                  AmqpCommunicationMode.ONE_TO_MANY, AmqpCommunicationTarget.EXTERNAL);
                return Boolean.TRUE;
            } catch (final RabbitMQVhostException e) {
                LOG.error(e.getMessage(), e);
                return Boolean.FALSE;
            }
        };
        JwtTokenUtils.asSafeCallableOnTenant(publishNewtenantEvent).apply(tenant.getName());

        return connection;
    }

    @Override
    public void deleteProjectConnection(final Long pProjectConnectionId) throws EntityNotFoundException {
        if (projectConnectionRepository.exists(pProjectConnectionId)) {
            projectConnectionRepository.delete(pProjectConnectionId);
        } else {
            final String message = "Invalid entity <ProjectConnection> for deletion. Entity (id=%d) does not exists";
            LOG.error(String.format(message, pProjectConnectionId));
            throw new EntityNotFoundException(pProjectConnectionId.toString(), ProjectConnection.class);
        }
    }

    @Override
    public ProjectConnection updateProjectConnection(final ProjectConnection pProjectConnection)
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

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.project.service.IProjectConnectionService#retrieveProjectsConnectionsByProject(org.
     * springframework.data.domain.Pageable)
     */
    @Override
    public Page<ProjectConnection> retrieveProjectsConnectionsByProject(final String pProjectName,
            final Pageable pPageable) {
        return projectConnectionRepository.findByProjectName(pProjectName, pPageable);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.project.service.IProjectConnectionService#retrieveProjectConnectionById(java.lang.String)
     */
    @Override
    public ProjectConnection retrieveProjectConnectionById(final Long pId) throws EntityNotFoundException {
        final Optional<ProjectConnection> result = Optional.ofNullable(projectConnectionRepository.findOne(pId));
        return result.orElseThrow(() -> new EntityNotFoundException(pId, ProjectConnection.class));
    }

}
