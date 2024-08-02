/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.project.client.rest;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.project.domain.Project;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Feign client allowing access to the module with REST requests.
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 */
@RestClient(name = "rs-admin-instance", contextId = "rs-admin-instance.projects-client")
public interface IProjectsClient {

    String ROOT_PATH = "/projects";
    // Projects Requests
    // -----------------

    /**
     * Retrieve projects list
     *
     * @param pPage index of the requested page
     * @param pSize number of elements per page
     * @return List of projects
     */
    @GetMapping(path = ROOT_PATH,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<PagedModel<EntityModel<Project>>> retrieveProjectList(@RequestParam("page") int pPage,
                                                                         @RequestParam("size") int pSize);

    /**
     * Same than {@link IProjectsClient#retrieveProjectList(int, int)} but only for public projects
     */
    @GetMapping(path = ROOT_PATH + "/public",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<PagedModel<EntityModel<Project>>> retrievePublicProjectList(@RequestParam("page") int page,
                                                                               @RequestParam("size") int size);

    /**
     * Create a new project
     *
     * @param pNewProject new Project to create
     * @return Created project
     */
    @PostMapping(path = ROOT_PATH,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<EntityModel<Project>> createProject(@Valid @RequestBody Project pNewProject);

    /**
     * Retrieve a project by name
     *
     * @param pProjectName Project name
     * @return Project
     */
    @GetMapping(path = ROOT_PATH + "/{project_name}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<EntityModel<Project>> retrieveProject(@PathVariable("project_name") String pProjectName);

    /**
     * Update given project.
     *
     * @param pProjectName     project name
     * @param pProjectToUpdate project to update
     * @return Updated Project
     */
    @PutMapping(path = ROOT_PATH + "/{project_name}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<EntityModel<Project>> updateProject(@PathVariable("project_name") String pProjectName,
                                                       @RequestBody Project pProjectToUpdate);

    /**
     * Delete given project
     *
     * @param pProjectName Project name to delete
     * @return Void
     */
    @DeleteMapping(path = ROOT_PATH + "/{project_name}",
                   consumes = MediaType.APPLICATION_JSON_VALUE,
                   produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Void> deleteProject(@PathVariable("project_name") String pProjectName);
}
