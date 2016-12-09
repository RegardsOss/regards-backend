/*
 * LICENSE_PLACEHOLDER
 */
/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Class IProjectService
 *
 * Interface for ProjectService. Allow to query projects entities.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Christophe Mertz
 * @author SébastienBinda
 *
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
     * @throws ModuleException
     *             Thrown when no {@link Project} with passed <code>name</code> exists.
     * @since 1.0-SNAPSHOT
     */
    Project retrieveProject(String pProjectName) throws ModuleException;

    /**
     *
     * Delete a project
     *
     * @param pProjectName
     *            Project name to delete
     * @throws ModuleException
     *             Thrown when no {@link Project} with passed <code>name</code> exists.
     * @since 1.0-SNAPSHOT
     */
    void deleteProject(String pProjectName) throws ModuleException;

    /**
     *
     * Update a project
     *
     * @param pProjectName
     *            Project name to update
     * @param pProject
     *            Project to update
     * @return Updated Project
     * @throws ModuleException
     *             <br/>
     *             {@link EntityNotFoundException}</b> if the request project does not exists.<br/>
     *             {@link EntityInvalidException} if pProjectName doesn't match the given project
     *
     * @since 1.0-SNAPSHOT
     */
    Project updateProject(String pProjectName, Project pProject) throws ModuleException;

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
     * Retrieve project List.
     *
     * @param pPageable
     *            pagination informations
     *
     * @return List of projects
     * @since 1.0-SNAPSHOT
     */
    Page<Project> retrieveProjectList(Pageable pPageable);

    /**
     *
     * Retrieve all public projects
     *
     * @param pPageable
     *            pagination informations
     *
     * @return List of public projects
     * @since 1.0-SNAPSHOT
     */
    Page<Project> retrievePublicProjectList(Pageable pPageable);

    /**
     *
     * Create a new project
     *
     * @param pNewProject
     *            Project ot create
     * @return Created project
     * @throws ModuleException
     *             <br/>
     *             {@link EntityException} If Project already exists for the given name
     * @since 1.0-SNAPSHOT
     */
    Project createProject(Project pNewProject) throws ModuleException;

}
