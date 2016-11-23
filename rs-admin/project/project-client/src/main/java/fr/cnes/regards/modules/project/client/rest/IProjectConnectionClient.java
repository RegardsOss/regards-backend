/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.client.rest;

import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.client.core.annotation.RestClient;
import fr.cnes.regards.modules.project.client.rest.fallback.ProjectConnectionFallback;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class ProjectsClient
 *
 * Feign client allowing access to the module with REST requests.
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
@RestClient(name = "rs-admin", fallback = ProjectConnectionFallback.class)
@RequestMapping(value = "/projects", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IProjectConnectionClient {

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
    @RequestMapping(method = RequestMethod.GET, value = "/{project_name}/connection/{microservice}")
    @ResponseBody
    ResponseEntity<Resource<ProjectConnection>> retrieveProjectConnection(
            @PathVariable("project_name") String pProjectName, @PathVariable("microservice") String pMicroService);

    /**
     *
     * Create a new project connection in instance database. The associated Project must exists and have a valid
     * identifier.
     *
     * @param pProjectConnection
     *            ProjectConnection to create.
     * @return ProjectConnection created
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.POST, value = "/connections")
    @ResponseBody
    ResponseEntity<Resource<ProjectConnection>> createProjectConnection(
            @Valid @RequestBody ProjectConnection pProjectConnection);

    /**
     *
     * Update an existing Project connection
     *
     * @param pProjectConnection
     *            ProjectConnection to update
     * @return updated pProjectConnection
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/connections")
    @ResponseBody
    ResponseEntity<Resource<ProjectConnection>> updateProjectConnection(
            @Valid @RequestBody ProjectConnection pProjectConnection);

    /**
     *
     * Delete an existing Project connection
     *
     * @param pProjectName
     *            project name
     * @param pMicroservice
     *            microservice name
     * @return void
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{project_name}/connection/{microservice}")
    @ResponseBody
    ResponseEntity<Void> deleteProjectConnection(@PathVariable("project_name") String pProjectName,
            @PathVariable("microservice") String pMicroservice);
}
