/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.signature;

import java.util.List;

import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidEntityException;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Class ProjectsSignature
 *
 * Signature interface for Projects module
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RequestMapping("/projects")
public interface IProjectsSignature {

    // Projects Requests
    // -----------------

    /**
     *
     * Retrieve projects list
     *
     * @return List of projects
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    ResponseEntity<List<Resource<Project>>> retrieveProjectList();

    /**
     *
     * Create a new project
     *
     * @param pNewProject
     *            new Project to create
     * @return Created project
     * @throws EntityException
     *             <br/>
     *             {@link AlreadyExistingException} If Project already exists for the given name
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    @ResponseBody
    ResponseEntity<Resource<Project>> createProject(@Valid @RequestBody Project pNewProject) throws EntityException;

    /**
     *
     * Retrieve a project by name
     *
     * @param pProjectName
     *            Project name
     * @return Project
     * @throws EntityException
     *             {@link EntityNotFoundException} project does not exists
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{project_name}", produces = "application/json")
    @ResponseBody
    ResponseEntity<Resource<Project>> retrieveProject(@PathVariable("project_name") String pProjectName)
            throws EntityException;

    /**
     *
     * Update given project.
     *
     * @param pProjectName
     *            project name
     * @param pProjectToUpdate
     *            project to update
     * @return Updated Project
     * @throws EntityException
     *             <br/>
     *             {@link EntityNotFoundException}</b> if the request project does not exists.<br/>
     *             {@link InvalidEntityException} if pProjectName doesn't match the given project
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{project_name}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<Project>> updateProject(@PathVariable("project_name") String pProjectName,
            @RequestBody Project pProjectToUpdate) throws EntityException;

    /**
     *
     * Delete given project
     *
     * @param pProjectName
     *            Project name to delete
     * @return Void
     * @throws EntityException
     *             {@link EntityNotFoundException} project to delete does not exists
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{project_name}", produces = "application/json")
    @ResponseBody
    ResponseEntity<Void> deleteProject(@PathVariable("project_name") String pProjectName) throws EntityException;

}
