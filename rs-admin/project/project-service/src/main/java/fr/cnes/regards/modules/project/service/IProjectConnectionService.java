/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnectionState;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 * Class IProjectService
 *
 * Interface for ProjectService. Allow to query projects entities.
 * @author CS
 * @author Xavier-Alexandre Brochard
 */
public interface IProjectConnectionService {

    /**
     * Retrieve all projects connections.
     * @param pageable Pagination information
     * @return List of {@link ProjectConnection}
     */
    Page<ProjectConnection> retrieveProjectsConnections(Pageable pageable);

    /**
     * Create a new project connection. Requires an existing Project.
     * @param pProjectConnection ProjectConnection to create
     * @param silent create a project silently without publishing any event
     * @return Created ProjectConnection
     * @throws EntityAlreadyExistsException Project connection already exists for couple (project name/microservice name),
     * @throws EntityNotFoundException      The Project referenced doesn't exist
     */
    ProjectConnection createProjectConnection(ProjectConnection pProjectConnection, boolean silent)
            throws ModuleException;

    /**
     * Create static project connection and activate it if and only if it doesn't exist! Else do nothing! Only useful
     * for statically configured datasources.
     * @param projectConnection the connection to eventually create
     * @return {@link ProjectConnection}
     * @throws ModuleException if error occurs!
     */
    ProjectConnection createStaticProjectConnection(ProjectConnection projectConnection) throws ModuleException;

    /**
     * Delete a ProjectConnection
     * @param pProjectConnectionId ProjectConnection Identifier
     * @throws EntityNotFoundException The ProjectConnection to delete doesn't exists
     */
    void deleteProjectConnection(Long pProjectConnectionId) throws EntityNotFoundException;

    /**
     * Delete all project connections
     */
    void deleteProjectConnections(Project project);

    /**
     * Update an existing project connection
     * @param projectConnectionId connection identifier
     * @param projectConnection Project connection to update
     * @return ProjectConnection updated
     * @throws ModuleException Project connection or referenced project doesn't exists
     */
    ProjectConnection updateProjectConnection(Long projectConnectionId, ProjectConnection projectConnection)
            throws ModuleException;

    /**
     * Retrieve a ProjectConnection with couple project name/ microservice name
     * @param projectName Project name
     * @param microservice microservice name
     * @return ProjectConnection
     * @throws EntityNotFoundException ProjectConnection doesn't exists
     */
    ProjectConnection retrieveProjectConnection(String projectName, String microservice) throws EntityNotFoundException;

    /**
     * Check is a connection already exists
     * @param projectName project name
     * @param microservice microservice
     */
    boolean existsProjectConnection(String projectName, String microservice);

    /**
     * Retrieve all enabled tenant connections for a specified microservice
     * @param microservice microservice name
     * @return all tenant connections
     */
    List<ProjectConnection> retrieveProjectConnections(String microservice);

    /**
     * Retrieve all project connections from database for a given project/tenant.
     * @param projectName The project name
     * @param pageable Spring managed object containing pagination information
     * @return The list of project connections wrapped in a {@link Page}
     */
    Page<ProjectConnection> retrieveProjectsConnectionsByProject(String projectName, Pageable pageable);

    /**
     * Retrieve a project connection by its id
     * @param id The project connection id
     * @return The project connection of passed id
     * @throws EntityNotFoundException Project connection doesn't exists
     */
    ProjectConnection retrieveProjectConnectionById(Long id) throws EntityNotFoundException;

    /**
     * Update project connection state
     * @param microservice microservice
     * @param projectName project name
     * @param state state
     * @param errorCause error cause if state equals to {@link TenantConnectionState#ERROR}
     * @return {@link ProjectConnection}
     */
    ProjectConnection updateState(String microservice, String projectName, TenantConnectionState state,
            Optional<String> errorCause) throws EntityNotFoundException;
}
