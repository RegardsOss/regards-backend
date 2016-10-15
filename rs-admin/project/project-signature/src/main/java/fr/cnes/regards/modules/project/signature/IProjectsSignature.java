/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.signature;

import java.util.List;

import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
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
import fr.cnes.regards.modules.project.domain.ProjectConnection;

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
    HttpEntity<List<Resource<Project>>> retrieveProjectList();

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
    HttpEntity<Resource<Project>> createProject(@Valid @RequestBody Project pNewProject) throws EntityException;

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
    HttpEntity<Resource<Project>> retrieveProject(@PathVariable("project_name") String pProjectName)
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
    HttpEntity<Resource<Project>> updateProject(@PathVariable("project_name") String pProjectName,
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
    HttpEntity<Void> deleteProject(@PathVariable("project_name") String pProjectName) throws EntityException;

    // Projects connections requests
    // -----------------------------

    /**
     *
     * Retrieve a project connection from instance database for a given project and a given microservice.
     *
     * @param pProjectName
     *            Project name
     * @param pMicroService
     *            Microservice name
     * @return HttpEntity<Resource<ProjectConnection>>
     * @throws EntityNotFoundException
     *             ProjectConnection doesn't exists
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{project_name}/connection/{microservice}",
            produces = "application/json")
    @ResponseBody
    HttpEntity<Resource<ProjectConnection>> retrieveProjectConnection(@PathVariable("project_name") String pProjectName,
            @PathVariable("microservice") String pMicroService) throws EntityNotFoundException;

    /**
     *
     * Create a new project connection in instance database. The associated Project must exists and have a valid
     * identifier.
     *
     * @param pProjectConnection
     *            ProjectConnection to create.
     * @return ProjectConnection created
     * @throws EntityException
     *             <br/>
     *             {@link AreadyAlreadyExistingException} Thrown in case of the ProjectConnection already exists for the
     *             Project and microservice. <br/>
     *             {@link EntityNotFoundException} Thrown in case of Referenced project does not exists
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.POST, value = "/connections", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<ProjectConnection>> createProjectConnection(
            @Valid @RequestBody ProjectConnection pProjectConnection) throws EntityException;

    /**
     *
     * Update an existing Project connection
     *
     * @param pProjectConnection
     *            ProjectConnection to update
     * @return updated pProjectConnection
     * @throws EntityException
     *             <br/>
     *             {@link EntityNotFoundException} Thrown in case of the ProjectConnection does not exists
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/connections", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<ProjectConnection>> updateProjectConnection(
            @Valid @RequestBody ProjectConnection pProjectConnection) throws EntityException;

    /**
     *
     * Delete an existing Project connection
     *
     * @param pProjectName
     *            project name
     * @param pMicroservice
     *            microservice name
     * @return void
     * @throws EntityException
     *             <br/>
     *             {@link EntityNotFoundException} Thrown in case of the ProjectConnection does not exists
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{project_name}/connection/{microservice}",
            produces = "application/json")
    @ResponseBody
    HttpEntity<Void> deleteProjectConnection(@PathVariable("project_name") String pProjectName,
            @PathVariable("microservice") String pMicroservice) throws EntityException;
}
