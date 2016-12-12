/*
 * LICENSE_PLACEHOLDER
 */
/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
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
     * @return Created ProjectConnection
     * @throws ModuleException
     *             <br/>
     *             {@link AlreadyExistingException} Project connection already exists for couple (project name/
     *             microservice name)<br/>
     *             {@link AlreadyExistingException} ModuleEntityNotFoundException The Project referenced doesn't exists
     * @since 1.0-SNAPSHOT
     */
    ProjectConnection createProjectConnection(ProjectConnection pProjectConnection) throws ModuleException;

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
     * @param pProjectConnection
     *            Project connection to update
     * @return ProjectConnection updated
     * @throws EntityNotFoundException
     *             Project connection or referenced project doesn't exists
     * @since 1.0-SNAPSHOT
     */
    ProjectConnection updateProjectConnection(ProjectConnection pProjectConnection) throws EntityNotFoundException;

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

}
