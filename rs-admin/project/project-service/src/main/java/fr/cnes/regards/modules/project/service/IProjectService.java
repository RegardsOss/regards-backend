/*
 * LICENSE_PLACEHOLDER
 */
/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import java.util.List;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidEntityException;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Class IProjectService
 *
 * Interface for ProjectService. Allow to query projects entities.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public interface IProjectService {

    /**
     *
     * Retrieve a project with is unique name
     *
     * @param pProjectName
     *            project name to retrieve
     * @return Project
     * @throws EntityNotFoundException
     *             Thrown when no {@link Project} with passed <code>name</code> exists.
     * @since 1.0-SNAPSHOT
     */
    Project retrieveProject(String pProjectName) throws EntityNotFoundException;

    /**
     *
     * Delete a project
     *
     * @param pProjectName
     *            Project name to delete
     * @return Remaining projects
     * @throws EntityNotFoundException
     *             Thrown when no {@link Project} with passed <code>name</code> exists.
     * @since 1.0-SNAPSHOT
     */
    List<Project> deleteProject(String pProjectName) throws EntityNotFoundException;

    /**
     *
     * Update a project
     *
     * @param pProjectName
     *            Project name to update
     * @param pProject
     *            Project to update
     * @return Updated Project
     * @throws EntityException
     *             <br/>
     *             {@link EntityNotFoundException}</b> if the request project does not exists.<br/>
     *             {@link InvalidEntityException} if pProjectName doesn't match the given project
     *
     * @since 1.0-SNAPSHOT
     */
    Project updateProject(String pProjectName, Project pProject) throws EntityException;

    /**
     *
     * Retrieve project List.
     *
     * @return List of projects
     * @since 1.0-SNAPSHOT
     */
    List<Project> retrieveProjectList();

    /**
     *
     * Create a new project
     *
     * @param pNewProject
     *            Project ot create
     * @return Created project
     * @throws EntityException
     *             <br/>
     *             {@link AlreadyExistingException} If Project already exists for the given name
     * @since 1.0-SNAPSHOT
     */
    Project createProject(Project pNewProject) throws EntityException;

    /**
     *
     * Check if given project exists.
     *
     * @param pProjectName
     *            Project to check for existance
     * @return [true|false]
     * @since 1.0-SNAPSHOT
     */
    boolean existProject(String pProjectName);

    /**
     *
     * Check that the given project isn't deleted
     *
     * @param pProjectName
     *            Project to check
     * @return [true|false]
     * @since 1.0-SNAPSHOT
     */
    boolean notDeletedProject(String pProjectName);

}
