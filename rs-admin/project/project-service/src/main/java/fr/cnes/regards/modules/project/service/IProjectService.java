/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import java.util.List;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

public interface IProjectService {

    Project retrieveProject(String pProjectId);

    List<Project> deleteProject(String pProjectId);

    Project modifyProject(String pProjectId, Project pProject) throws EntityException;

    List<Project> retrieveProjectList();

    Project createProject(Project pNewProject) throws EntityException;

    boolean existProject(String pProjectId);

    boolean notDeletedProject(String pProjectId);

    /**
     *
     * Create a new project connection. Requires an existing Project.
     *
     * @param pProjectConnection
     *            ProjectConnection to create
     * @return Created ProjectConnection
     * @throws AlreadyExistingException
     *             Project connection already exists for couple (project name/ microservice name)
     * @throws EntityNotFoundException
     *             The Project referenced doesn't exists
     * @since 1.0-SNAPSHOT
     */
    ProjectConnection createProjectConnection(ProjectConnection pProjectConnection) throws EntityException;

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
    void deleteProjectConnection(Long pProjectConnectionId) throws EntityException;

    /**
     *
     * Update an existing project connection
     *
     * @param pProjectConnection
     *            Project connection to update
     * @return ProjectConnection updated
     * @throws EntityNotFoundException
     *             Project connecion or referenced project doesn't exists
     * @since 1.0-SNAPSHOT
     */
    ProjectConnection updateProjectConnection(ProjectConnection pProjectConnection) throws EntityException;

    /**
     *
     * Retrieve a ProjectConnection with couple project name/ microservice name
     *
     * @param pProjectName
     *            Project name
     * @param pMicroService
     *            microservice name
     * @return ProjectConnection
     * @since 1.0-SNAPSHOT
     */
    ProjectConnection retreiveProjectConnection(final String pProjectName, String pMicroService);

}
