/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.encryption.IEncryptionService;
import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionConfigurationCreated;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionConfigurationDeleted;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionConfigurationUpdated;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnectionState;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service class to manage REGARDS projects.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Christophe Mertz
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
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

    private final IEncryptionService encryptionService;

    /**
     * The constructor.
     *
     * @param projectRepository           The JPA {@link Project} repository.
     * @param projectConnectionRepository The JPA {@link ProjectConnection} repository.
     * @param instancePublisher           Amqp publisher
     */
    public ProjectConnectionService(IProjectRepository projectRepository,
                                    IProjectConnectionRepository projectConnectionRepository,
                                    IInstancePublisher instancePublisher,
                                    EntityManager em,
                                    IEncryptionService encryptionService) {
        this.projectRepository = projectRepository;
        this.projectConnectionRepository = projectConnectionRepository;
        this.instancePublisher = instancePublisher;
        this.encryptionService = encryptionService;
    }

    @Override
    public Page<ProjectConnection> retrieveProjectsConnections(Pageable pageable) {
        return projectConnectionRepository.findAll(pageable);
    }

    @Override
    public ProjectConnection retrieveProjectConnection(String projectName, String microservice)
        throws EntityNotFoundException {
        ProjectConnection connection = projectConnectionRepository.findOneByProjectNameAndMicroservice(projectName,
                                                                                                       microservice);
        if (connection == null) {
            String message = String.format("No connection found for project %s and microservice %s",
                                           projectName,
                                           microservice);
            LOGGER.error(message);
            throw new EntityNotFoundException(message);
        }
        return connection;
    }

    @Override
    public boolean existsProjectConnection(String projectName, String microservice) {
        return projectConnectionRepository.existsProjectConnection(projectName, microservice);
    }

    @Override
    public ProjectConnection createProjectConnection(ProjectConnection projectConnection, boolean silent)
        throws ModuleException {
        ProjectConnection connection;
        Project project = projectConnection.getProject();
        // Check referenced project exists
        if (project.getId() != null && projectRepository.isActiveProject(project.getId())) {
            // Manage connection conflicts!
            manageProjectConnectionConflicts(projectConnection);
            // Check project connection to create doesn't already exists
            if (projectConnectionRepository.findOneByProjectNameAndMicroservice(project.getName(),
                                                                                projectConnection.getMicroservice())
                == null) {
                // Connection disabled
                // Multitenant starter is responsible for enabling data source
                projectConnection.setState(TenantConnectionState.DISABLED);
                // This service is reponsible for password encryption
                projectConnection.setPassword(encryptionService.encrypt(projectConnection.getPassword()));
                connection = projectConnectionRepository.save(projectConnection);
            } else {
                throw new EntityAlreadyExistsException(project.getName());
            }
        } else {
            throw new EntityNotFoundException(projectConnection.getId().toString(), ProjectConnection.class);
        }

        // Send event to all microservices that a new connection is available for a new project
        if (!silent) {
            instancePublisher.publish(new TenantConnectionConfigurationCreated(toTenantConnection(connection),
                                                                               connection.getMicroservice()));
        }

        return connection;
    }

    /**
     * Test potential conflict with an existing project connection.<br/>
     * <ul>
     * <li>A same connection can be used by a same project on another microservice</li>
     * <li>A same connection cannot be used by 2 different projects</li>
     * </ul>
     *
     * @param projectConnection project connection to control
     * @throws EntityInvalidException if a conflict is detected
     */
    private void manageProjectConnectionConflicts(ProjectConnection projectConnection) throws EntityInvalidException {
        List<ProjectConnection> connections = projectConnectionRepository.findByUrl(projectConnection.getUrl());

        String projectName = projectConnection.getProject().getName();
        for (ProjectConnection connection : connections) {
            if (!projectName.equals(connection.getProject().getName())) {
                String message = String.format(
                    "A same connection can only be used by a same project on another microservice. Conflict detected between project %s and project %s.",
                    projectName,
                    connection.getProject().getName());
                LOGGER.error(message);
                throw new EntityInvalidException(message);
            }
        }

    }

    @Override
    public ProjectConnection createStaticProjectConnection(ProjectConnection projectConnection) throws ModuleException {

        // Only store connection if it really does not exist
        if (existsProjectConnection(projectConnection.getProject().getName(), projectConnection.getMicroservice())) {
            LOGGER.warn("Project connection already exists for tenant {} and microservice {}",
                        projectConnection.getProject().getName(),
                        projectConnection.getMicroservice());
            // Skipping silently
            return projectConnection;
        }

        projectConnection.setState(TenantConnectionState.ENABLED);
        projectConnection.setPassword(encryptionService.encrypt(projectConnection.getPassword()));
        return projectConnectionRepository.save(projectConnection);
    }

    /**
     * Transform {@link ProjectConnection} to {@link TenantConnection}
     *
     * @param connection related connection
     */
    private TenantConnection toTenantConnection(ProjectConnection connection) {
        // Send event to all microservices that a new connection is available for a new project
        return new TenantConnection(connection.getProject().getName(),
                                    connection.getUrl(),
                                    connection.getUserName(),
                                    connection.getPassword(),
                                    connection.getDriverClassName());
    }

    @Override
    public void deleteProjectConnection(Long projectConnectionId) throws EntityNotFoundException {
        Optional<ProjectConnection> ctxOpt = projectConnectionRepository.findById(projectConnectionId);
        if (ctxOpt.isPresent()) {
            projectConnectionRepository.deleteById(projectConnectionId);
        } else {
            LOGGER.error("Invalid entity <ProjectConnection> for deletion. Entity (id={}) does not exists",
                         projectConnectionId);
            throw new EntityNotFoundException(projectConnectionId.toString(), ProjectConnection.class);
        }

        // Publish configuration deletion
        ProjectConnection ctx = ctxOpt.get();
        instancePublisher.publish(new TenantConnectionConfigurationDeleted(toTenantConnection(ctx),
                                                                           ctx.getMicroservice()));
    }

    @Override
    public void deleteProjectConnections(Project project) {
        List<ProjectConnection> connections = projectConnectionRepository.deleteByProjectId(project.getId());
        // Publish configuration deletion
        for (ProjectConnection connection : connections) {
            instancePublisher.publish(new TenantConnectionConfigurationDeleted(toTenantConnection(connection),
                                                                               connection.getMicroservice()));
        }
    }

    @Override
    public ProjectConnection updateProjectConnection(Long projectConnectionId, ProjectConnection projectConnection)
        throws ModuleException {
        ProjectConnection connection;
        // Check that entity to update exists
        if (projectConnection.getId() != null && projectConnectionRepository.existsById(projectConnection.getId())) {
            Project project = projectConnection.getProject();
            // Check that the referenced project exists
            if (project.getId() != null && projectRepository.isActiveProject(project.getId())) {
                // Manage connection conflicts!
                manageProjectConnectionConflicts(projectConnection);
                // Disable connection : new configuration may be incorrect
                // Multitenant starter is reponsible for enabling data source
                projectConnection.setState(TenantConnectionState.DISABLED);
                // lets handle password modifications
                ProjectConnection fromDb = projectConnectionRepository.findById(projectConnectionId).get();
                if (!Objects.equals(fromDb.getPassword(), projectConnection.getPassword())) {
                    projectConnection.setPassword(encryptionService.encrypt(projectConnection.getPassword()));
                }
                // Update entity
                connection = projectConnectionRepository.save(projectConnection);
            } else {
                throw new EntityNotFoundException(project.getName(), Project.class);
            }
        } else {
            throw new EntityNotFoundException(projectConnection.getId().toString(), ProjectConnection.class);
        }

        // Publish configuration update
        instancePublisher.publish(new TenantConnectionConfigurationUpdated(toTenantConnection(connection),
                                                                           connection.getMicroservice()));

        return connection;
    }

    @Override
    public Page<ProjectConnection> retrieveProjectsConnectionsByProject(String projectName, Pageable pageable) {
        return projectConnectionRepository.findByProjectName(projectName, pageable);
    }

    @Override
    public ProjectConnection retrieveProjectConnectionById(Long id) throws EntityNotFoundException {
        Optional<ProjectConnection> result = projectConnectionRepository.findById(id);
        return result.orElseThrow(() -> new EntityNotFoundException(id, ProjectConnection.class));
    }

    @Override
    public List<ProjectConnection> retrieveProjectConnections(String microservice) {
        return projectConnectionRepository.getMicroserviceConnections(microservice);
    }

    @Override
    public ProjectConnection updateState(String microservice,
                                         String projectName,
                                         TenantConnectionState state,
                                         Optional<String> errorCause) throws EntityNotFoundException {
        ProjectConnection connection = retrieveProjectConnection(projectName, microservice);
        connection.setState(state);
        if (errorCause.isPresent()) {
            if (errorCause.get().length() > ProjectConnection.ERROR_MAX_LENGTH) {
                connection.setErrorCause(errorCause.get().substring(0, ProjectConnection.ERROR_MAX_LENGTH));
            } else {
                connection.setErrorCause(errorCause.get());
            }
        } else {
            connection.setErrorCause(null);
        }
        return projectConnectionRepository.save(connection);
    }
}
