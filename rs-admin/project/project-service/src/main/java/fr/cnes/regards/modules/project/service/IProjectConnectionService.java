/*
 * LICENSE_PLACEHOLDER
 */
/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class IProjectService
 *
 * Interface for ProjectService. Allow to query projects entities.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public interface IProjectConnectionService {

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
     * @throws ModuleException
     *             <br/>
     *             {@link ModuleEntityNotFoundException} The ProjectConnection to delete doesn't exists
     * @since 1.0-SNAPSHOT
     */
    void deleteProjectConnection(Long pProjectConnectionId) throws ModuleEntityNotFoundException;

    /**
     *
     * Update an existing project connection
     *
     * @param pProjectConnection
     *            Project connection to update
     * @return ProjectConnection updated
     * @throws ModuleException
     *             <br/>
     *             {@link ModuleEntityNotFoundException} Project connection or referenced project doesn't exists
     * @since 1.0-SNAPSHOT
     */
    ProjectConnection updateProjectConnection(ProjectConnection pProjectConnection)
            throws ModuleEntityNotFoundException;

    /**
     *
     * Retrieve a ProjectConnection with couple project name/ microservice name
     *
     * @param pProjectName
     *            Project name
     * @param pMicroService
     *            microservice name
     * @return ProjectConnection
     * @throws ModuleEntityNotFoundException
     *             ProjectConnection doesn't exists
     * @since 1.0-SNAPSHOT
     */
    ProjectConnection retrieveProjectConnection(final String pProjectName, String pMicroService)
            throws ModuleEntityNotFoundException;

}
