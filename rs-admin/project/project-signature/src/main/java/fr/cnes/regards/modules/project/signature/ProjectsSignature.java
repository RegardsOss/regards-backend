/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.signature;

import java.util.List;

import javax.naming.OperationNotSupportedException;
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
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

public interface ProjectsSignature {

    // Projects Requests
    // -----------------

    @RequestMapping(value = "/projects", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    HttpEntity<List<Resource<Project>>> retrieveProjectList();

    @RequestMapping(value = "/projects", method = RequestMethod.POST, consumes = "application/json",
            produces = "application/json")
    @ResponseBody
    HttpEntity<Resource<Project>> createProject(@Valid @RequestBody Project pNewProject)
            throws AlreadyExistingException;

    @RequestMapping(method = RequestMethod.GET, value = "/projects/{project_id}", produces = "application/json")
    @ResponseBody
    HttpEntity<Resource<Project>> retrieveProject(@PathVariable("project_id") String pProjectId);

    @RequestMapping(method = RequestMethod.PUT, value = "/projects/{project_id}",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> modifyProject(@PathVariable("project_id") String pProjectId, @RequestBody Project pProjectUpdated)
            throws OperationNotSupportedException, EntityNotFoundException;

    @RequestMapping(method = RequestMethod.DELETE, value = "/projects/{project_id}", produces = "application/json")
    @ResponseBody
    HttpEntity<Void> deleteProject(@PathVariable("project_id") String pProjectId);

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
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.GET, value = "/projects/{project_name}/connection/{microservice}",
            produces = "application/json")
    @ResponseBody
    HttpEntity<Resource<ProjectConnection>> retrieveProjectConnection(@PathVariable("project_name") String pProjectName,
            @PathVariable("microservice") String pMicroService);

    /**
     *
     * Create a new project connection in instance database. The associated Project must exists and have a valid
     * identifier.
     *
     * @param pProjectConnection
     *            ProjectConnection to create.
     * @return ProjectConnection created
     * @throws AlreadyExistingException
     *             Thrown in case of the ProjectConnection already exists for the Project and microservice.
     * @throws EntityNotFoundException
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.POST, value = "/projects/connections",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<ProjectConnection>> createProjectConnection(
            @Valid @RequestBody ProjectConnection pProjectConnection)
            throws AlreadyExistingException, EntityNotFoundException;

    /**
     *
     * Update an existing Project connection
     *
     * @param pProjectConnection
     * @return
     * @throws EntityNotFoundException
     * @since TODO
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/projects/connections",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<ProjectConnection>> updateProjectConnection(
            @Valid @RequestBody ProjectConnection pProjectConnection) throws EntityNotFoundException;

    /**
     *
     * Delete an existing Project connection
     *
     * @param pProjectName
     *            project name
     * @param pMicroservice
     *            microservice name
     * @return void
     * @throws EntityNotFoundException
     *             Connection does not exists
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/projects/{project_name}/connection/{microservice}",
            produces = "application/json")
    @ResponseBody
    HttpEntity<Void> deleteProjectConnection(@PathVariable("project_name") String pProjectName,
            @PathVariable("microservice") String pMicroservice) throws EntityNotFoundException;
}
