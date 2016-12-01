/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.client.rest;

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

import fr.cnes.regards.client.core.annotation.RestClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 * Feign client allowing access to the module with REST requests.
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@RestClient(name = "rs-admin")
@RequestMapping(value = "/projects", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IProjectsClient {

    // Projects Requests
    // -----------------

    /**
     *
     * Retrieve projects list
     *
     * @return List of projects
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    ResponseEntity<List<Resource<Project>>> retrieveProjectList();

    /**
     *
     * Create a new project
     *
     * @param pNewProject
     *            new Project to create
     * @return Created project
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    ResponseEntity<Resource<Project>> createProject(@Valid @RequestBody Project pNewProject);

    /**
     *
     * Retrieve a project by name
     *
     * @param pProjectName
     *            Project name
     * @return Project
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{project_name}")
    @ResponseBody
    ResponseEntity<Resource<Project>> retrieveProject(@PathVariable("project_name") String pProjectName);

    /**
     *
     * Update given project.
     *
     * @param pProjectName
     *            project name
     * @param pProjectToUpdate
     *            project to update
     * @return Updated Project
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{project_name}")
    @ResponseBody
    ResponseEntity<Resource<Project>> updateProject(@PathVariable("project_name") String pProjectName,
            @RequestBody Project pProjectToUpdate);

    /**
     *
     * Delete given project
     *
     * @param pProjectName
     *            Project name to delete
     * @return Void
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{project_name}")
    @ResponseBody
    ResponseEntity<Void> deleteProject(@PathVariable("project_name") String pProjectName);
}
