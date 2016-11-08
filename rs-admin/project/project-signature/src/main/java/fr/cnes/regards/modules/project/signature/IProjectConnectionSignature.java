/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.signature;

import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
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
public interface IProjectConnectionSignature {

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
    ResponseEntity<Resource<ProjectConnection>> retrieveProjectConnection(
            @PathVariable("project_name") String pProjectName, @PathVariable("microservice") String pMicroService)
            throws EntityNotFoundException;

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
    ResponseEntity<Resource<ProjectConnection>> createProjectConnection(
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
    ResponseEntity<Resource<ProjectConnection>> updateProjectConnection(
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
    ResponseEntity<Void> deleteProjectConnection(@PathVariable("project_name") String pProjectName,
            @PathVariable("microservice") String pMicroservice) throws EntityException;
}
