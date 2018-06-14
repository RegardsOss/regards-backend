/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnectionState;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class IProjectService
 *
 * Interface for ProjectService. Allow to query projects entities.
 *
 * @author CS
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
public interface IProjectConnectionService {

    /**
     *
     * Retrieve all projects connections.
     *
     * @param pPageable
     *            Pagination information
     * @return List of {@link ProjectConnection}
     * @since 1.0-SNAPSHOT
     */
    Page<ProjectConnection> retrieveProjectsConnections(Pageable pPageable);

    /**
     *
     * Create a new project connection. Requires an existing Project.
     *
     * @param pProjectConnection
     *            ProjectConnection to create
     * @param silent
     *            create a project silently without publishing any event
     * @return Created ProjectConnection
     * @throws ModuleException
     *             <br/>
     *             {@link fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException} Project
     *             connection already exists for couple (project name/
     *             microservice name)<br/>
     *             {@link EntityAlreadyExistsException} ModuleEntityNotFoundException The Project referenced doesn't
     *             exists
     * @since 1.0-SNAPSHOT
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
    ProjectConnection createStaticProjectConnection(ProjectConnection projectConnection)
            throws ModuleException;

    /**
     *
     * Delete a ProjectConnection
     *
     * @param pProjectConnectionId
     *            ProjectConnection Identifier
     * @throws EntityNotFoundException
     *             The ProjectConnection to delete doesn't exists
     * @since 1.0-SNAPSHOT
     */
    void deleteProjectConnection(Long pProjectConnectionId) throws EntityNotFoundException;

    /**
     *
     * Update an existing project connection
     *
     * @param pProjectConnectionId
     *            connection identifier
     * @param pProjectConnection
     *            Project connection to update
     * @return ProjectConnection updated
     * @throws ModuleException
     *             Project connection or referenced project doesn't exists
     * @since 1.0-SNAPSHOT
     */
    ProjectConnection updateProjectConnection(Long pProjectConnectionId, ProjectConnection pProjectConnection)
            throws ModuleException;

    /**
     *
     * Retrieve a ProjectConnection with couple project name/ microservice name
     *
     * @param pProjectName
     *            Project name
     * @param pMicroService
     *            microservice name
     * @return ProjectConnection
     * @throws EntityNotFoundException
     *             ProjectConnection doesn't exists
     * @since 1.0-SNAPSHOT
     */
    ProjectConnection retrieveProjectConnection(final String pProjectName, String pMicroService)
            throws EntityNotFoundException;

    /**
     * Check is a connection already exists
     * @param projectName project name
     * @param microService microservice
     * @return
     */
    boolean existsProjectConnection(final String projectName, String microService);

    /**
     * Retrieve all enabled tenant connections for a specified microservice
     *
     * @param microservice
     *            microservice name
     * @return all tenant connections
     */
    List<ProjectConnection> retrieveProjectConnections(String microService);

    /**
     * Retrieve all project connections from database for a given project/tenant.
     *
     * @param pProjectName
     *            The project name
     * @param pPageable
     *            Spring managed object containing pagination information
     * @return The list of project connections wrapped in a {@link Page}
     * @since 1.0-SNAPSHOT
     */
    Page<ProjectConnection> retrieveProjectsConnectionsByProject(String pProjectName, Pageable pPageable);

    /**
     * Retrieve a project connection by its id
     *
     * @param pId
     *            The project connection id
     * @return The project connection of passed id
     * @throws EntityNotFoundException
     *             Project connection doesn't exists
     * @since 1.0-SNAPSHOT
     */
    ProjectConnection retrieveProjectConnectionById(Long pId) throws EntityNotFoundException;

    /**
     * Update project connection state
     * @param microservice microservice
     * @param projectName project name
     * @param state state
     * @param errorCause error cause if state equals to {@link TenantConnectionState#ERROR}
     * @return {@link ProjectConnection}
     * @throws EntityNotFoundException
     */
    ProjectConnection updateState(String microservice, String projectName, TenantConnectionState state,
            Optional<String> errorCause) throws EntityNotFoundException;
}
