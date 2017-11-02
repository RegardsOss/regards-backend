/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.project.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionConfigurationCreated;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionConfigurationDeleted;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionConfigurationUpdated;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
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
    public ProjectConnection createProjectConnection(final ProjectConnection pProjectConnection, final boolean silent)
            throws ModuleException {
        final ProjectConnection connection;
        final Project project = pProjectConnection.getProject();
        // Check referenced project exists
        if ((project.getId() != null) && projectRepository.isActiveProject(project.getId())) {
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
        if (!silent) {
            instancePublisher.publish(new TenantConnectionConfigurationCreated(toTenantConnection(connection),
                    connection.getMicroservice()));
        }

        return connection;
    }

    /**
     * Transform {@link ProjectConnection} to {@link TenantConnection}
     *
     * @param pConnection
     *            related connection
     */
    private TenantConnection toTenantConnection(ProjectConnection pConnection) {
        // Send event to all microservices that a new connection is available for a new project
        return new TenantConnection(pConnection.getProject().getName(), pConnection.getUrl(), pConnection.getUserName(),
                pConnection.getPassword(), pConnection.getDriverClassName());
    }

    @Override
    public void deleteProjectConnection(final Long pProjectConnectionId) throws EntityNotFoundException {
        ProjectConnection connection = projectConnectionRepository.findOne(pProjectConnectionId);
        if (connection != null) {
            projectConnectionRepository.delete(pProjectConnectionId);
        } else {
            final String message = "Invalid entity <ProjectConnection> for deletion. Entity (id=%d) does not exists";
            LOGGER.error(String.format(message, pProjectConnectionId));
            throw new EntityNotFoundException(pProjectConnectionId.toString(), ProjectConnection.class);
        }

        // Publish configuration deletion
        instancePublisher.publish(new TenantConnectionConfigurationDeleted(toTenantConnection(connection),
                connection.getMicroservice()));
    }

    @Override
    public ProjectConnection updateProjectConnection(final Long pProjectConnectionId,
            final ProjectConnection pProjectConnection) throws EntityNotFoundException {
        final ProjectConnection connection;
        // Check that entity to update exists
        if ((pProjectConnection.getId() != null) && projectConnectionRepository.exists(pProjectConnection.getId())) {
            final Project project = pProjectConnection.getProject();
            // Check that the referenced project exists
            if ((project.getId() != null) && projectRepository.isActiveProject(project.getId())) {
                // Disable connection : new configuration may be incorrect
                // Multitenant starter is reponsible for enabling data source
                pProjectConnection.setEnabled(false);
                // Update entity
                connection = projectConnectionRepository.save(pProjectConnection);
            } else {
                throw new EntityNotFoundException(project.getName(), Project.class);
            }
        } else {
            throw new EntityNotFoundException(pProjectConnection.getId().toString(), ProjectConnection.class);
        }

        // Publish configuration update
        instancePublisher.publish(new TenantConnectionConfigurationUpdated(toTenantConnection(connection),
                connection.getMicroservice()));

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
    public List<ProjectConnection> retrieveProjectConnection(final String microservice) throws EntityNotFoundException {
        return projectConnectionRepository.getMicroserviceConnections(microservice);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.project.service.IProjectConnectionService#enableProjectConnection(java.lang.String,
     * java.lang.String)
     */
    @Override
    public ProjectConnection enableProjectConnection(String pMicroService, String pProjectName)
            throws EntityNotFoundException {
        ProjectConnection connection = retrieveProjectConnection(pProjectName, pMicroService);
        connection.setEnabled(true);
        return projectConnectionRepository.save(connection);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.project.service.IProjectConnectionService#disableProjectConnection(java.lang.String,
     * java.lang.String)
     */
    @Override
    public ProjectConnection disableProjectConnection(String pMicroService, String pProjectName)
            throws EntityNotFoundException {
        ProjectConnection connection = retrieveProjectConnection(pProjectName, pMicroService);
        connection.setEnabled(false);
        return projectConnectionRepository.save(connection);
    }
}
